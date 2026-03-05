import argparse
import base64
import json
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import Any, Callable, Dict, List, Optional, Sequence, Tuple

import requests


ALL_ISSUE_TYPES: Tuple[str, ...] = (
    "REFUND",
    "RESHIP",
    "CLAIM",
    "DELIVERY",
    "ADDRESS",
    "PRODUCT",
    "PAYMENT",
    "OTHER",
)


def _now_ms() -> int:
    return int(time.time() * 1000)


def _pretty(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)


def _require(cond: bool, msg: str) -> None:
    if not cond:
        raise AssertionError(msg)


def _snake_to_camel(key: str) -> str:
    parts = key.split("_")
    if not parts:
        return key
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def _camel_to_snake(key: str) -> str:
    out: List[str] = []
    for ch in key:
        if ch.isupper():
            out.append("_")
            out.append(ch.lower())
        else:
            out.append(ch)
    text = "".join(out)
    return text[1:] if text.startswith("_") else text


def _pick(obj: Dict[str, Any], *keys: str) -> Any:
    for key in keys:
        candidates = [key, _snake_to_camel(key), _camel_to_snake(key)]
        for candidate in candidates:
            if candidate in obj:
                return obj[candidate]
    return None


def _idem_key() -> str:
    return str(uuid.uuid4())


@dataclass(frozen=True)
class ApiResult:
    http_status: int
    body: Dict[str, Any]
    headers: Dict[str, str]
    elapsed_ms: int

    @property
    def success(self) -> bool:
        return bool(self.body.get("success") is True)

    @property
    def code(self) -> str:
        return str(self.body.get("code") or "")

    @property
    def message(self) -> str:
        return str(self.body.get("message") or "")

    @property
    def data(self) -> Any:
        return self.body.get("data")


class ApiClient:
    def __init__(self, base_url: str, timeout_s: int = 30, user_agent: str = "international-shopping-customerservice-idem-test/1.0"):
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s
        self.session = requests.Session()
        self.session.headers.update(
            {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "User-Agent": user_agent,
            }
        )
        self.csrf_token: Optional[str] = None

    def _url(self, path: str) -> str:
        if not path.startswith("/"):
            path = "/" + path
        return self.base_url + path

    def request(
        self,
        method: str,
        path: str,
        *,
        params: Optional[Dict[str, Any]] = None,
        json_body: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        with_csrf: bool = True,
    ) -> ApiResult:
        method_u = method.upper()
        if with_csrf and method_u in ("POST", "PUT", "PATCH", "DELETE") and not path.startswith("/auth/"):
            self.ensure_csrf_for(method_u, path)

        req_headers: Dict[str, str] = {}
        if with_csrf and self.csrf_token:
            req_headers["X-CSRF-Token"] = self.csrf_token
        if headers:
            req_headers.update(headers)

        start = _now_ms()
        resp = self.session.request(
            method=method_u,
            url=self._url(path),
            params=params,
            json=json_body,
            headers=req_headers,
            timeout=self.timeout_s,
        )
        elapsed = _now_ms() - start

        try:
            body = resp.json() if resp.content else {}
        except Exception:
            body = {"raw": resp.text}

        return ApiResult(resp.status_code, body, dict(resp.headers), elapsed)

    def _cookie_values(self, name: str) -> List[str]:
        vals: List[str] = []
        for c in self.session.cookies:
            if c is not None and c.name == name and c.value is not None:
                vals.append(str(c.value))
        return vals

    def _cookie_header_for(self, method: str, path: str) -> str:
        req = requests.Request(method=method, url=self._url(path))
        prep = self.session.prepare_request(req)
        return str(prep.headers.get("Cookie") or "")

    def login(self, account: str, password: str) -> None:
        r = self.request("POST", "/auth/login", json_body={"account": account, "password": password}, with_csrf=False)
        _require(r.http_status == 200 and r.success, f"login failed: {r.http_status}, body={_pretty(r.body)}")
        _require(len(self._cookie_values("access_token")) >= 1, "login succeeded but access_token cookie missing")

    def issue_csrf(self) -> None:
        r = self.request("GET", "/auth/csrf", with_csrf=False)
        _require(r.http_status == 200 and r.success, f"csrf failed: {r.http_status}, body={_pretty(r.body)}")
        token = (r.data or {}).get("csrf_token") if isinstance(r.data, dict) else None
        _require(bool(token), f"csrf token missing, body={_pretty(r.body)}")
        self.csrf_token = str(token)

        vals = self._cookie_values("csrf_token")
        _require(len(vals) >= 1, "csrf_token cookie missing")
        _require(self.csrf_token in vals, f"csrf token mismatch, token={self.csrf_token}, cookie_vals={vals}")

    def ensure_csrf_for(self, method: str, path: str) -> None:
        if not self.csrf_token:
            self.issue_csrf()

        cookie_header = self._cookie_header_for(method, path)
        if "csrf_token=" not in cookie_header:
            self.issue_csrf()
            cookie_header = self._cookie_header_for(method, path)

        _require("csrf_token=" in cookie_header, f"csrf_token cookie not attached: path={path}, cookie_header={cookie_header!r}")

    def access_token(self) -> str:
        vals = self._cookie_values("access_token")
        _require(len(vals) >= 1, "access_token cookie missing")
        return vals[-1]


def _assert_success(r: ApiResult, ok_http: Sequence[int], ctx: str) -> None:
    _require(r.http_status in ok_http, f"{ctx}: http_status={r.http_status}, expect={list(ok_http)}, body={_pretty(r.body)}")
    _require(r.success, f"{ctx}: success=false, body={_pretty(r.body)}")


def _assert_not_error(r: ApiResult, ctx: str) -> None:
    _require(r.http_status < 500, f"{ctx}: unexpected 5xx: {r.http_status}, body={_pretty(r.body)}")


def _run_concurrent(n: int, task: Callable[[], ApiResult]) -> List[ApiResult]:
    workers = max(1, int(n))
    results: List[ApiResult] = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futs = [pool.submit(task) for _ in range(workers)]
        for fut in as_completed(futs):
            results.append(fut.result())
    return results


def _assert_idempotency_concurrency(
    results: List[ApiResult],
    *,
    ok_http: Sequence[int],
    conflict_contains: Sequence[str],
    ctx: str,
    strict_all_success: bool,
) -> List[ApiResult]:
    successes = [r for r in results if r.http_status in ok_http and r.success]
    conflicts = [r for r in results if r.http_status == 409 and (not r.success)]
    others: List[ApiResult] = []
    for r in results:
        if r in successes or r in conflicts:
            continue
        others.append(r)

    if strict_all_success:
        _require(len(successes) == len(results), f"{ctx}: strict mode requires all success, got={[(r.http_status, r.code) for r in results]}")
    else:
        _require(len(successes) >= 1, f"{ctx}: no success response, got={[(r.http_status, r.code, r.message) for r in results]}")
        for c in conflicts:
            _require(
                any(text in c.message for text in conflict_contains),
                f"{ctx}: unexpected conflict message={c.message!r}, allowed={conflict_contains}",
            )
        _require(len(others) == 0, f"{ctx}: unexpected responses={[(r.http_status, r.code, r.message) for r in others]}")
    return successes


def _extract_ticket_identity(data: Any) -> Tuple[int, str]:
    _require(isinstance(data, dict), f"ticket data is not object: {data}")
    ticket_id = _pick(data, "ticket_id", "id")
    ticket_no = _pick(data, "ticket_no")
    _require(isinstance(ticket_id, int), f"ticket_id invalid: {data}")
    _require(bool(ticket_no), f"ticket_no missing: {data}")
    return int(ticket_id), str(ticket_no)


def _extract_message_identity(data: Any) -> Tuple[int, str]:
    _require(isinstance(data, dict), f"message data is not object: {data}")
    message_id = _pick(data, "id", "message_id")
    message_no = _pick(data, "message_no")
    _require(isinstance(message_id, int), f"message_id invalid: {data}")
    _require(bool(message_no), f"message_no missing: {data}")
    return int(message_id), str(message_no)


def _extract_participant_id(data: Any) -> int:
    _require(isinstance(data, dict), f"participant data is not object: {data}")
    participant_id = _pick(data, "id", "participant_id")
    _require(isinstance(participant_id, int), f"participant_id invalid: {data}")
    return int(participant_id)


def _extract_reship_identity(data: Any) -> Tuple[int, str]:
    _require(isinstance(data, dict), f"reship data is not object: {data}")
    rid = _pick(data, "id", "reship_id")
    rno = _pick(data, "reship_no")
    _require(isinstance(rid, int), f"reship_id invalid: {data}")
    _require(bool(rno), f"reship_no missing: {data}")
    return int(rid), str(rno)


def _extract_ws_token(data: Any) -> str:
    _require(isinstance(data, dict), f"ws session data is not object: {data}")
    token = _pick(data, "ws_token")
    _require(bool(token), f"ws_token missing: {data}")
    return str(token)


def _decode_uid_from_access_token(raw_token: str) -> Optional[int]:
    token = raw_token.strip()
    if token.lower().startswith("bearer "):
        token = token[7:].strip()
    parts = token.split(".")
    if len(parts) != 3:
        return None
    payload = parts[1]
    payload += "=" * ((4 - len(payload) % 4) % 4)
    try:
        data = json.loads(base64.urlsafe_b64decode(payload.encode("utf-8")).decode("utf-8"))
    except Exception:
        return None
    uid = data.get("uid")
    if uid is None:
        return None
    try:
        return int(uid)
    except Exception:
        return None


def _resolve_user_id(cli_value: int, client: ApiClient, label: str) -> int:
    if cli_value > 0:
        return cli_value
    uid = _decode_uid_from_access_token(client.access_token())
    _require(uid is not None and uid >= 1, f"无法从 {label} access_token 解析 uid, 请显式传入 --{label}-user-id")
    return int(uid)


def _create_main_ticket(
    user: ApiClient,
    *,
    order_id: int,
    order_item_id: int,
    shipment_id: int,
    preferred_issue_type: str,
) -> Tuple[int, str, str]:
    preferred = preferred_issue_type.strip().upper()
    issue_candidates = [preferred] + [x for x in ALL_ISSUE_TYPES if x != preferred]
    for issue in issue_candidates:
        payload = {
            "order_id": order_id,
            "order_item_id": order_item_id,
            "shipment_id": shipment_id,
            "issue_type": issue,
            "title": f"idem-main-{uuid.uuid4().hex[:12]}",
            "description": "customerservice idempotency script",
            "attachments": [],
            "evidence": [],
            "requested_refund_amount": 1,
            "currency": "USD",
        }
        r = user.request("POST", "/users/me/tickets", json_body=payload, headers={"Idempotency-Key": _idem_key()})
        if r.http_status == 201 and r.success:
            ticket_id, ticket_no = _extract_ticket_identity(r.data)
            return ticket_id, ticket_no, issue
        if r.http_status == 409 and r.code == "CONFLICT" and "进行中的工单" in r.message:
            continue
        raise AssertionError(f"create main ticket unexpected response: {r.http_status}, body={_pretty(r.body)}")
    raise AssertionError("all issue types already have open tickets for order/shipment, cannot bootstrap idempotency test")


def main() -> int:
    parser = argparse.ArgumentParser(description="Idempotency + concurrency tests for customerservice domain APIs.")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1", help="API base URL")
    parser.add_argument("--timeout-s", type=int, default=30)
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--strict-all-success", action="store_true", help="Require no 409 during concurrency tests")

    parser.add_argument("--user-account", required=True)
    parser.add_argument("--user-password", required=True)
    parser.add_argument("--admin-account", required=True)
    parser.add_argument("--admin-password", required=True)

    parser.add_argument("--user-user-id", type=int, default=0, help="optional, auto decode from JWT if <=0")
    parser.add_argument("--admin-user-id", type=int, default=0, help="optional, auto decode from JWT if <=0")

    parser.add_argument("--order-id", type=int, required=True)
    parser.add_argument("--order-item-id", type=int, required=True)
    parser.add_argument("--order-item-sku-id", type=int, required=True)
    parser.add_argument("--shipment-id", type=int, required=True)
    parser.add_argument("--reship-bind-shipment-id", type=int, required=True)
    parser.add_argument("--main-issue-type", default="REFUND", choices=list(ALL_ISSUE_TYPES))
    args = parser.parse_args()

    user = ApiClient(args.base_url, timeout_s=args.timeout_s, user_agent="international-shopping-customerservice-user-idem-test/1.0")
    admin = ApiClient(args.base_url, timeout_s=args.timeout_s, user_agent="international-shopping-customerservice-admin-idem-test/1.0")

    print("[1/9] login + csrf + bootstrap ticket")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    user_user_id = _resolve_user_id(args.user_user_id, user, "user")
    admin_user_id = _resolve_user_id(args.admin_user_id, admin, "admin")
    ticket_id, ticket_no, issue_type = _create_main_ticket(
        user,
        order_id=args.order_id,
        order_item_id=args.order_item_id,
        shipment_id=args.shipment_id,
        preferred_issue_type=args.main_issue_type,
    )
    print(f"  bootstrap: ticket_id={ticket_id}, ticket_no={ticket_no}, issue_type={issue_type}, user_id={user_user_id}, admin_id={admin_user_id}")

    # assign admin as active assignee participant
    assign_key = _idem_key()
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/assign",
        json_body={"action_type": "ASSIGN", "to_assignee_user_id": admin_user_id, "note": "idem bootstrap assign"},
        headers={"Idempotency-Key": assign_key},
    )
    _assert_success(r, (200,), "bootstrap assign admin")

    print("[2/9] user ws-session + create/edit/recall/read idempotency concurrency")
    user_ws_idem = _idem_key()
    user_ws_payload = {"ticket_nos": [ticket_no], "event_types": ["MESSAGE_CREATED"]}

    def _user_ws_once() -> ApiResult:
        return user.request("POST", "/users/me/ws-sessions", json_body=user_ws_payload, headers={"Idempotency-Key": user_ws_idem})

    user_ws_results = _run_concurrent(args.concurrency, _user_ws_once)
    for rr in user_ws_results:
        _assert_not_error(rr, "user ws-session")
    user_ws_success = _assert_idempotency_concurrency(
        user_ws_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="user ws-session concurrency",
        strict_all_success=args.strict_all_success,
    )
    user_ws_tokens = {_extract_ws_token(r.data) for r in user_ws_success}
    _require(len(user_ws_tokens) == 1, f"user ws-session token should be stable, got={user_ws_tokens}")

    user_create_msg_idem = _idem_key()
    user_client_message_id = f"user-idem-{uuid.uuid4().hex[:24]}"
    user_create_msg_payload = {
        "message_type": "TEXT",
        "content": "user message for idempotency",
        "attachments": [],
        "client_message_id": user_client_message_id,
    }

    def _user_create_message_once() -> ApiResult:
        return user.request(
            "POST",
            f"/users/me/tickets/{ticket_no}/messages",
            json_body=user_create_msg_payload,
            headers={"Idempotency-Key": user_create_msg_idem},
        )

    user_create_results = _run_concurrent(args.concurrency, _user_create_message_once)
    user_create_success = _assert_idempotency_concurrency(
        user_create_results,
        ok_http=(201,),
        conflict_contains=("正在处理中",),
        ctx="user create message concurrency",
        strict_all_success=args.strict_all_success,
    )
    user_message_identities = {_extract_message_identity(r.data) for r in user_create_success}
    _require(len(user_message_identities) == 1, f"user create message identity should be stable, got={user_message_identities}")
    user_message_id, user_message_no = list(user_message_identities)[0]
    print(f"  user message: id={user_message_id}, no={user_message_no}")

    user_edit_idem = _idem_key()

    def _user_edit_message_once() -> ApiResult:
        return user.request(
            "PATCH",
            f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
            json_body={"content": "user edited in idem test"},
            headers={"Idempotency-Key": user_edit_idem},
        )

    user_edit_results = _run_concurrent(args.concurrency, _user_edit_message_once)
    _assert_idempotency_concurrency(
        user_edit_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="user edit message concurrency",
        strict_all_success=args.strict_all_success,
    )

    user_recall_idem = _idem_key()

    def _user_recall_message_once() -> ApiResult:
        return user.request(
            "POST",
            f"/users/me/tickets/{ticket_no}/messages/{user_message_no}/recall",
            json_body={"reason": "user idem recall"},
            headers={"Idempotency-Key": user_recall_idem},
        )

    user_recall_results = _run_concurrent(args.concurrency, _user_recall_message_once)
    _assert_idempotency_concurrency(
        user_recall_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="user recall message concurrency",
        strict_all_success=args.strict_all_success,
    )

    user_read_idem = _idem_key()

    def _user_read_once() -> ApiResult:
        return user.request(
            "POST",
            f"/users/me/tickets/{ticket_no}/read",
            json_body={"last_read_message_id": user_message_id},
            headers={"Idempotency-Key": user_read_idem},
        )

    user_read_results = _run_concurrent(args.concurrency, _user_read_once)
    _assert_idempotency_concurrency(
        user_read_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="user read concurrency",
        strict_all_success=args.strict_all_success,
    )

    print("[3/9] admin patch/assign/status idempotency concurrency")
    admin_patch_idem = _idem_key()

    def _admin_patch_once() -> ApiResult:
        return admin.request(
            "PATCH",
            f"/admin/tickets/{ticket_id}",
            json_body={"priority": "HIGH", "tags": ["idem", "concurrency"]},
            headers={"Idempotency-Key": admin_patch_idem},
        )

    admin_patch_results = _run_concurrent(args.concurrency, _admin_patch_once)
    _assert_idempotency_concurrency(
        admin_patch_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin patch ticket concurrency",
        strict_all_success=args.strict_all_success,
    )

    admin_assign_idem = _idem_key()

    def _admin_assign_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/assign",
            json_body={"action_type": "ASSIGN", "to_assignee_user_id": admin_user_id, "note": "idem assign"},
            headers={"Idempotency-Key": admin_assign_idem},
        )

    admin_assign_results = _run_concurrent(args.concurrency, _admin_assign_once)
    _assert_idempotency_concurrency(
        admin_assign_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin assign ticket concurrency",
        strict_all_success=args.strict_all_success,
    )

    admin_status_idem = _idem_key()

    def _admin_status_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/status",
            json_body={"to_status": "IN_PROGRESS", "note": "idem status"},
            headers={"Idempotency-Key": admin_status_idem},
        )

    admin_status_results = _run_concurrent(args.concurrency, _admin_status_once)
    _assert_idempotency_concurrency(
        admin_status_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin ticket status concurrency",
        strict_all_success=args.strict_all_success,
    )

    print("[4/9] admin ws-session + message create/edit/recall/read idempotency concurrency")
    admin_ws_idem = _idem_key()
    admin_ws_payload = {"ticket_ids": [ticket_id], "event_types": ["MESSAGE_CREATED"]}

    def _admin_ws_once() -> ApiResult:
        return admin.request("POST", "/admin/ws-sessions", json_body=admin_ws_payload, headers={"Idempotency-Key": admin_ws_idem})

    admin_ws_results = _run_concurrent(args.concurrency, _admin_ws_once)
    admin_ws_success = _assert_idempotency_concurrency(
        admin_ws_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin ws-session concurrency",
        strict_all_success=args.strict_all_success,
    )
    admin_ws_tokens = {_extract_ws_token(r.data) for r in admin_ws_success}
    _require(len(admin_ws_tokens) == 1, f"admin ws-session token should be stable, got={admin_ws_tokens}")

    admin_create_msg_idem = _idem_key()
    admin_client_message_id = f"admin-idem-{uuid.uuid4().hex[:24]}"
    admin_create_msg_payload = {
        "message_type": "TEXT",
        "content": "admin message for idempotency",
        "attachments": [],
        "client_message_id": admin_client_message_id,
    }

    def _admin_create_message_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/messages",
            json_body=admin_create_msg_payload,
            headers={"Idempotency-Key": admin_create_msg_idem},
        )

    admin_create_results = _run_concurrent(args.concurrency, _admin_create_message_once)
    admin_create_success = _assert_idempotency_concurrency(
        admin_create_results,
        ok_http=(201,),
        conflict_contains=("正在处理中",),
        ctx="admin create message concurrency",
        strict_all_success=args.strict_all_success,
    )
    admin_message_identities = {_extract_message_identity(r.data) for r in admin_create_success}
    _require(len(admin_message_identities) == 1, f"admin create message identity should be stable, got={admin_message_identities}")
    admin_message_id, admin_message_no = list(admin_message_identities)[0]
    print(f"  admin message: id={admin_message_id}, no={admin_message_no}")

    admin_edit_idem = _idem_key()

    def _admin_edit_message_once() -> ApiResult:
        return admin.request(
            "PATCH",
            f"/admin/tickets/{ticket_id}/messages/{admin_message_id}",
            json_body={"content": "admin edited in idem test"},
            headers={"Idempotency-Key": admin_edit_idem},
        )

    admin_edit_results = _run_concurrent(args.concurrency, _admin_edit_message_once)
    _assert_idempotency_concurrency(
        admin_edit_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin edit message concurrency",
        strict_all_success=args.strict_all_success,
    )

    admin_recall_idem = _idem_key()

    def _admin_recall_message_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/messages/{admin_message_id}/recall",
            json_body={"reason": "admin idem recall"},
            headers={"Idempotency-Key": admin_recall_idem},
        )

    admin_recall_results = _run_concurrent(args.concurrency, _admin_recall_message_once)
    _assert_idempotency_concurrency(
        admin_recall_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin recall message concurrency",
        strict_all_success=args.strict_all_success,
    )

    admin_read_idem = _idem_key()

    def _admin_read_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/read",
            json_body={"last_read_message_id": admin_message_id},
            headers={"Idempotency-Key": admin_read_idem},
        )

    admin_read_results = _run_concurrent(args.concurrency, _admin_read_once)
    _assert_idempotency_concurrency(
        admin_read_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="admin read concurrency",
        strict_all_success=args.strict_all_success,
    )

    print("[5/9] admin participant create/patch/leave idempotency concurrency")
    participant_create_idem = _idem_key()
    participant_create_payload = {"participant_type": "SYSTEM", "role": "WATCHER"}

    def _participant_create_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/participants",
            json_body=participant_create_payload,
            headers={"Idempotency-Key": participant_create_idem},
        )

    participant_create_results = _run_concurrent(args.concurrency, _participant_create_once)
    participant_create_success = _assert_idempotency_concurrency(
        participant_create_results,
        ok_http=(201,),
        conflict_contains=("正在处理中",),
        ctx="participant create concurrency",
        strict_all_success=args.strict_all_success,
    )
    participant_ids = {_extract_participant_id(r.data) for r in participant_create_success}
    _require(len(participant_ids) == 1, f"participant create identity should be stable, got={participant_ids}")
    participant_id = list(participant_ids)[0]
    print(f"  participant id={participant_id}")

    participant_patch_idem = _idem_key()

    def _participant_patch_once() -> ApiResult:
        return admin.request(
            "PATCH",
            f"/admin/tickets/{ticket_id}/participants/{participant_id}",
            json_body={"role": "COLLABORATOR"},
            headers={"Idempotency-Key": participant_patch_idem},
        )

    participant_patch_results = _run_concurrent(args.concurrency, _participant_patch_once)
    _assert_idempotency_concurrency(
        participant_patch_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="participant patch concurrency",
        strict_all_success=args.strict_all_success,
    )

    participant_leave_idem = _idem_key()

    def _participant_leave_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/participants/{participant_id}/leave",
            headers={"Idempotency-Key": participant_leave_idem},
        )

    participant_leave_results = _run_concurrent(args.concurrency, _participant_leave_once)
    _assert_idempotency_concurrency(
        participant_leave_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="participant leave concurrency",
        strict_all_success=args.strict_all_success,
    )

    print("[6/9] admin reship create/patch/status/bind idempotency concurrency")
    reship_create_idem = _idem_key()
    reship_create_payload = {
        "order_id": args.order_id,
        "reason_code": "LOST",
        "currency": "USD",
        "note": "reship create idem test",
        "items": [{"order_item_id": args.order_item_id, "sku_id": args.order_item_sku_id, "quantity": 1}],
    }

    def _reship_create_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/tickets/{ticket_id}/reships",
            json_body=reship_create_payload,
            headers={"Idempotency-Key": reship_create_idem},
        )

    reship_create_results = _run_concurrent(args.concurrency, _reship_create_once)
    reship_create_success = _assert_idempotency_concurrency(
        reship_create_results,
        ok_http=(201,),
        conflict_contains=("正在处理中",),
        ctx="reship create concurrency",
        strict_all_success=args.strict_all_success,
    )
    reship_identities = {_extract_reship_identity(r.data) for r in reship_create_success}
    _require(len(reship_identities) == 1, f"reship create identity should be stable, got={reship_identities}")
    reship_id, reship_no = list(reship_identities)[0]
    print(f"  reship: id={reship_id}, no={reship_no}")

    reship_patch_idem = _idem_key()

    def _reship_patch_once() -> ApiResult:
        return admin.request(
            "PATCH",
            f"/admin/reships/{reship_id}",
            json_body={"currency": "USD", "items_cost": "0", "shipping_cost": "0.00", "note": "reship patch idem"},
            headers={"Idempotency-Key": reship_patch_idem},
        )

    reship_patch_results = _run_concurrent(args.concurrency, _reship_patch_once)
    _assert_idempotency_concurrency(
        reship_patch_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="reship patch concurrency",
        strict_all_success=args.strict_all_success,
    )

    reship_status_idem = _idem_key()

    def _reship_status_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/reships/{reship_id}/status",
            json_body={"to_status": "APPROVED", "note": "reship approved idem"},
            headers={"Idempotency-Key": reship_status_idem},
        )

    reship_status_results = _run_concurrent(args.concurrency, _reship_status_once)
    _assert_idempotency_concurrency(
        reship_status_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="reship status concurrency",
        strict_all_success=args.strict_all_success,
    )

    reship_bind_idem = _idem_key()
    reship_bind_payload = {"shipment_ids": [args.reship_bind_shipment_id]}

    def _reship_bind_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/reships/{reship_id}/shipments",
            json_body=reship_bind_payload,
            headers={"Idempotency-Key": reship_bind_idem},
        )

    reship_bind_results = _run_concurrent(args.concurrency, _reship_bind_once)
    bind_success = _assert_idempotency_concurrency(
        reship_bind_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="reship bind shipments concurrency",
        strict_all_success=args.strict_all_success,
    )
    seen_bound = False
    for s in bind_success:
        rows = s.data if isinstance(s.data, list) else []
        for row in rows:
            if isinstance(row, dict) and _pick(row, "shipment_id") == args.reship_bind_shipment_id:
                seen_bound = True
                break
    _require(seen_bound, f"bound shipment_id={args.reship_bind_shipment_id} not found in reship bind success responses")

    print("[7/9] user close idempotency concurrency")
    close_idem = _idem_key()

    def _close_once() -> ApiResult:
        return user.request(
            "POST",
            f"/users/me/tickets/{ticket_no}/close",
            json_body={"note": "close by idem test"},
            headers={"Idempotency-Key": close_idem},
        )

    close_results = _run_concurrent(args.concurrency, _close_once)
    _assert_idempotency_concurrency(
        close_results,
        ok_http=(200,),
        conflict_contains=("正在处理中",),
        ctx="user close ticket concurrency",
        strict_all_success=args.strict_all_success,
    )

    print("[8/9] sequential replay checks with same idempotency keys")
    r = user.request("POST", "/users/me/ws-sessions", json_body=user_ws_payload, headers={"Idempotency-Key": user_ws_idem})
    _assert_success(r, (200,), "user ws-session replay")
    _require(_extract_ws_token(r.data) in user_ws_tokens, "user ws-session replay token mismatch")

    r = admin.request("POST", "/admin/ws-sessions", json_body=admin_ws_payload, headers={"Idempotency-Key": admin_ws_idem})
    _assert_success(r, (200,), "admin ws-session replay")
    _require(_extract_ws_token(r.data) in admin_ws_tokens, "admin ws-session replay token mismatch")

    r = admin.request("POST", f"/admin/tickets/{ticket_id}/reships", json_body=reship_create_payload, headers={"Idempotency-Key": reship_create_idem})
    _assert_success(r, (201,), "reship create replay")
    replay_reship_id, replay_reship_no = _extract_reship_identity(r.data)
    _require(replay_reship_id == reship_id and replay_reship_no == reship_no, "reship replay identity mismatch")

    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body=user_create_msg_payload,
        headers={"Idempotency-Key": user_create_msg_idem},
    )
    _assert_success(r, (201,), "user create message replay")
    replay_user_msg_id, replay_user_msg_no = _extract_message_identity(r.data)
    _require(replay_user_msg_id == user_message_id and replay_user_msg_no == user_message_no, "user message replay mismatch")

    print("[9/9] done")
    print("DONE.")
    print(
        "summary: "
        f"ticket_id={ticket_id}, ticket_no={ticket_no}, user_message_id={user_message_id}, "
        f"admin_message_id={admin_message_id}, participant_id={participant_id}, "
        f"reship_id={reship_id}, reship_no={reship_no}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as err:
        print(f"HTTP error: {err}", file=sys.stderr)
        raise

import argparse
import base64
import json
import sys
import time
import uuid
from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Sequence, Tuple

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


def _str_len(n: int, ch: str = "x") -> str:
    return ch * max(0, int(n))


def _idem_key(n: Optional[int] = None) -> str:
    if n is None:
        return str(uuid.uuid4())
    seed = uuid.uuid4().hex + uuid.uuid4().hex + uuid.uuid4().hex
    if n <= len(seed):
        return seed[:n]
    return (seed + _str_len(n - len(seed), "k"))[:n]


def _rand_ticket_no() -> str:
    return ("TK" + uuid.uuid4().hex[:24]).upper()


def _rand_message_no() -> str:
    return ("MG" + uuid.uuid4().hex[:24]).upper()


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
    def __init__(self, base_url: str, timeout_s: int = 30, user_agent: str = "international-shopping-customerservice-business-test/1.0"):
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
        raw_body: Optional[str] = None,
        headers: Optional[Dict[str, str]] = None,
        with_csrf: bool = True,
    ) -> ApiResult:
        if raw_body is not None and json_body is not None:
            raise ValueError("raw_body and json_body cannot be used together")

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
            data=raw_body,
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


def _assert_error(
    r: ApiResult,
    http_status: int,
    code: str,
    message_contains: Optional[str],
    ctx: str,
) -> None:
    _require(r.http_status == http_status, f"{ctx}: http_status={r.http_status}, expect={http_status}, body={_pretty(r.body)}")
    _require(not r.success, f"{ctx}: expect success=false, body={_pretty(r.body)}")
    _require(r.code == code, f"{ctx}: code={r.code}, expect={code}, body={_pretty(r.body)}")
    if message_contains:
        _require(message_contains in r.message, f"{ctx}: message={r.message!r}, expect contains={message_contains!r}")


def _assert_bad_request(r: ApiResult, contains: str, ctx: str) -> None:
    _assert_error(r, 400, "BAD_REQUEST", contains, ctx)


def _assert_conflict(r: ApiResult, contains: str, ctx: str) -> None:
    _assert_error(r, 409, "CONFLICT", contains, ctx)


def _assert_not_found(r: ApiResult, contains: str, ctx: str) -> None:
    _assert_error(r, 404, "NOT_FOUND", contains, ctx)


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
    reship_id = _pick(data, "id", "reship_id")
    reship_no = _pick(data, "reship_no")
    _require(isinstance(reship_id, int), f"reship_id invalid: {data}")
    _require(bool(reship_no), f"reship_no missing: {data}")
    return int(reship_id), str(reship_no)


def _extract_ws_token(data: Any) -> str:
    _require(isinstance(data, dict), f"ws session data is not object: {data}")
    token = _pick(data, "ws_token")
    _require(bool(token), f"ws_token missing: {data}")
    return str(token)


def _extract_list(data: Any) -> List[Any]:
    _require(isinstance(data, list), f"data should be list, got={data}")
    return data


def _find_participant(
    items: Any,
    *,
    participant_type: Optional[str] = None,
    role: Optional[str] = None,
    participant_user_id: Optional[int] = None,
) -> Optional[Dict[str, Any]]:
    if not isinstance(items, list):
        return None
    for item in items:
        if not isinstance(item, dict):
            continue
        item_type = str(_pick(item, "participant_type") or "")
        item_role = str(_pick(item, "role") or "")
        item_uid = _pick(item, "participant_user_id")
        if participant_type and item_type != participant_type:
            continue
        if role and item_role != role:
            continue
        if participant_user_id is not None and item_uid != participant_user_id:
            continue
        return item
    return None


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
    order_id: int,
    order_item_id: int,
    shipment_id: int,
    preferred_issue_type: str,
) -> Tuple[ApiResult, Dict[str, Any], str]:
    preferred = preferred_issue_type.strip().upper()
    issue_candidates = [preferred] + [x for x in ALL_ISSUE_TYPES if x != preferred]
    for issue in issue_candidates:
        payload = {
            "order_id": order_id,
            "order_item_id": order_item_id,
            "shipment_id": shipment_id,
            "issue_type": issue,
            "title": _str_len(200, "T"),
            "description": "customerservice business flow",
            "attachments": ["https://example.com/ticket/att-1.png"],
            "evidence": ["https://example.com/ticket/evidence-1.png"],
            "requested_refund_amount": 1,
            "currency": "USD",
        }
        r = user.request(
            "POST",
            "/users/me/tickets",
            json_body=payload,
            headers={"Idempotency-Key": _idem_key(64)},
        )
        if r.http_status == 201 and r.success:
            return r, payload, issue
        if r.http_status == 409 and r.code == "CONFLICT" and "进行中的工单" in r.message:
            continue
        raise AssertionError(f"create main ticket unexpected response: {r.http_status}, body={_pretty(r.body)}")
    raise AssertionError("all issue types already have open tickets for given user/order/shipment, cannot create main ticket")


def main() -> int:
    parser = argparse.ArgumentParser(description="Business flow tests for customerservice domain APIs (requests based).")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1", help="API base URL")
    parser.add_argument("--timeout-s", type=int, default=30)

    parser.add_argument("--user-account", required=True)
    parser.add_argument("--user-password", required=True)
    parser.add_argument("--admin-account", required=True)
    parser.add_argument("--admin-password", required=True)

    parser.add_argument("--user-user-id", type=int, default=0, help="optional, auto decode from JWT if <=0")
    parser.add_argument("--admin-user-id", type=int, default=0, help="optional, auto decode from JWT if <=0")

    parser.add_argument("--order-id", type=int, required=True, help="order_id for create ticket and reship")
    parser.add_argument("--order-item-id", type=int, required=True, help="order_item_id for create ticket and reship")
    parser.add_argument("--order-item-sku-id", type=int, required=True, help="sku_id of --order-item-id")
    parser.add_argument("--shipment-id", type=int, required=True, help="shipment_id linked to the ticket")
    parser.add_argument("--reship-bind-shipment-id", type=int, required=True, help="shipment_id to bind into reship, must belong to same order and not bound by another reship")
    parser.add_argument("--main-issue-type", default="REFUND", choices=list(ALL_ISSUE_TYPES))
    args = parser.parse_args()

    user = ApiClient(args.base_url, timeout_s=args.timeout_s, user_agent="international-shopping-customerservice-user-business-test/1.0")
    admin = ApiClient(args.base_url, timeout_s=args.timeout_s, user_agent="international-shopping-customerservice-admin-business-test/1.0")

    print("[1/17] login + csrf + resolve user ids")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()
    user_user_id = _resolve_user_id(args.user_user_id, user, "user")
    admin_user_id = _resolve_user_id(args.admin_user_id, admin, "admin")
    print(f"  user_id={user_user_id}, admin_id={admin_user_id}")

    print("[2/17] /users/me/tickets query validation")
    r = user.request("GET", "/users/me/tickets", params={"page": 1, "size": 20})
    _assert_success(r, (200,), "GET /users/me/tickets positive")
    _extract_list(r.data)

    r = user.request("GET", "/users/me/tickets", params={"page": 1, "size": 200})
    _assert_success(r, (200,), "GET /users/me/tickets size=200")
    r = user.request("GET", "/users/me/tickets", params={"page": 0})
    _assert_bad_request(r, "page", "GET /users/me/tickets page=0")
    r = user.request("GET", "/users/me/tickets", params={"size": 201})
    _assert_bad_request(r, "size", "GET /users/me/tickets size=201")
    r = user.request("GET", "/users/me/tickets", params={"status": "INVALID"})
    _assert_bad_request(r, "status", "GET /users/me/tickets invalid status")
    r = user.request("GET", "/users/me/tickets", params={"order_no": ""})
    _assert_bad_request(r, "order_no", "GET /users/me/tickets order_no blank")
    r = user.request(
        "GET",
        "/users/me/tickets",
        params={"created_from": "2026-03-03T00:00:00Z", "created_to": "2026-03-01T00:00:00Z"},
    )
    _assert_bad_request(r, "created_from", "GET /users/me/tickets invalid date range")

    print("[3/17] /users/me/tickets create validation + positive + duplicate conflict")
    bad_payload_base = {
        "order_id": args.order_id,
        "order_item_id": args.order_item_id,
        "shipment_id": args.shipment_id,
        "issue_type": args.main_issue_type,
        "title": "normal title",
        "description": "normal desc",
        "attachments": [],
        "evidence": [],
        "requested_refund_amount": 1,
        "currency": "USD",
    }
    r = user.request("POST", "/users/me/tickets", json_body=bad_payload_base)
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets missing idem")
    r = user.request(
        "POST",
        "/users/me/tickets",
        json_body=bad_payload_base,
        headers={"Idempotency-Key": _idem_key(65)},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets idem length=65")

    payload_issue_null = dict(bad_payload_base)
    payload_issue_null["issue_type"] = None
    r = user.request(
        "POST",
        "/users/me/tickets",
        json_body=payload_issue_null,
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "issueType", "POST /users/me/tickets issue_type null")

    payload_title_201 = dict(bad_payload_base)
    payload_title_201["title"] = _str_len(201, "T")
    r = user.request(
        "POST",
        "/users/me/tickets",
        json_body=payload_title_201,
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "title", "POST /users/me/tickets title len=201")

    created, main_ticket_payload, selected_issue_type = _create_main_ticket(
        user,
        args.order_id,
        args.order_item_id,
        args.shipment_id,
        args.main_issue_type,
    )
    _assert_success(created, (201,), "POST /users/me/tickets positive main")
    ticket_id, ticket_no = _extract_ticket_identity(created.data)
    print(f"  main ticket created: ticket_id={ticket_id}, ticket_no={ticket_no}, issue_type={selected_issue_type}")

    r = user.request(
        "POST",
        "/users/me/tickets",
        json_body=main_ticket_payload,
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "进行中的工单", "POST /users/me/tickets duplicate open dedupe")

    print("[4/17] user ticket detail + close endpoint validations")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}")
    _assert_success(r, (200,), "GET /users/me/tickets/{ticket_no} positive")
    detail_ticket_id, detail_ticket_no = _extract_ticket_identity(r.data)
    _require(detail_ticket_id == ticket_id and detail_ticket_no == ticket_no, "ticket detail identity mismatch")

    r = user.request("GET", f"/users/me/tickets/{_str_len(9, 'A')}")
    _assert_bad_request(r, "ticket_no", "GET /users/me/tickets/{ticket_no} length<10")
    r = user.request("GET", f"/users/me/tickets/{_rand_ticket_no()}")
    _assert_not_found(r, "工单不存在", "GET /users/me/tickets/{ticket_no} not found")

    note_256 = _str_len(256, "n")
    r = user.request("POST", f"/users/me/tickets/{ticket_no}/close", json_body={"note": note_256})
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets/{ticket_no}/close missing idem")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/close",
        json_body={"note": note_256},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "note", "POST /users/me/tickets/{ticket_no}/close note len=256")
    r = user.request(
        "POST",
        f"/users/me/tickets/{_rand_ticket_no()}/close",
        json_body={"note": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "工单不存在", "POST /users/me/tickets/{ticket_no}/close not found")

    print("[5/17] user messages list/create/edit/recall/read/ws-session")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/messages", params={"size": 100})
    _assert_success(r, (200,), "GET /users/me/tickets/{ticket_no}/messages size=100")
    _extract_list(r.data)
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/messages", params={"size": 101})
    _assert_bad_request(r, "size", "GET /users/me/tickets/{ticket_no}/messages size=101")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/messages", params={"before_id": 1, "after_id": 1})
    _assert_conflict(r, "before_id", "GET /users/me/tickets/{ticket_no}/messages before+after")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/messages", params={"order": "bad"})
    _assert_bad_request(r, "order", "GET /users/me/tickets/{ticket_no}/messages invalid order")
    r = user.request("GET", f"/users/me/tickets/{_rand_ticket_no()}/messages")
    _assert_not_found(r, "工单不存在", "GET /users/me/tickets/{ticket_no}/messages not found")

    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={"content": "hello", "client_message_id": f"cli-{uuid.uuid4().hex[:16]}"},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets/{ticket_no}/messages missing idem")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={"content": None, "attachments": None, "client_message_id": f"cli-{uuid.uuid4().hex[:16]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content 与 attachments", "POST /users/me/tickets/{ticket_no}/messages content+attachments null")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={"content": _str_len(4001, "c"), "client_message_id": f"cli-{uuid.uuid4().hex[:16]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "POST /users/me/tickets/{ticket_no}/messages content len=4001")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={
            "content": "x",
            "attachments": [f"https://example.com/a/{i}.png" for i in range(6)],
            "client_message_id": f"cli-{uuid.uuid4().hex[:16]}",
        },
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "attachments", "POST /users/me/tickets/{ticket_no}/messages attachments >5")

    user_create_msg_key = _idem_key()
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={
            "message_type": "TEXT",
            "content": "user message created by business script",
            "attachments": [],
            "client_message_id": f"cli-{uuid.uuid4().hex[:20]}",
        },
        headers={"Idempotency-Key": user_create_msg_key},
    )
    _assert_success(r, (201,), "POST /users/me/tickets/{ticket_no}/messages positive")
    user_message_id, user_message_no = _extract_message_identity(r.data)
    print(f"  user message: id={user_message_id}, no={user_message_no}")

    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
        json_body={"content": "edited by user"},
    )
    _assert_bad_request(r, "Idempotency-Key", "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} missing idem")
    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
        json_body={"content": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} content null")
    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
        json_body={"content": _str_len(4001, "c")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} content len=4001")
    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{_str_len(9, 'A')}",
        json_body={"content": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "message_no", "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} len<10")
    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{_rand_message_no()}",
        json_body={"content": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "消息不存在", "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} not found")
    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
        json_body={"content": _str_len(4000, "E")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "PATCH /users/me/tickets/{ticket_no}/messages/{message_no} positive boundary len=4000")

    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}/recall",
        json_body={"reason": "test"},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets/{ticket_no}/messages/{message_no}/recall missing idem")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}/recall",
        json_body={"reason": _str_len(256, "r")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "reason", "POST /users/me/tickets/{ticket_no}/messages/{message_no}/recall reason len=256")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages/{_rand_message_no()}/recall",
        json_body={"reason": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "消息不存在", "POST /users/me/tickets/{ticket_no}/messages/{message_no}/recall not found")

    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}/recall",
        json_body={"reason": "recalled by user"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /users/me/tickets/{ticket_no}/messages/{message_no}/recall positive")

    r = user.request(
        "PATCH",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}",
        json_body={"content": "try edit recalled"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "撤回", "PATCH recalled user message should conflict")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages/{user_message_no}/recall",
        json_body={"reason": "again"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "消息已撤回", "POST recall already recalled user message")

    r = user.request("POST", f"/users/me/tickets/{ticket_no}/read", json_body={"last_read_message_id": user_message_id})
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets/{ticket_no}/read missing idem")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/read",
        json_body={"last_read_message_id": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId", "POST /users/me/tickets/{ticket_no}/read null")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/read",
        json_body={"last_read_message_id": 0},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId", "POST /users/me/tickets/{ticket_no}/read 0")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/read",
        json_body={"last_read_message_id": 999999999999},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId 不属于当前工单", "POST /users/me/tickets/{ticket_no}/read unrelated message")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/read",
        json_body={"last_read_message_id": user_message_id},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /users/me/tickets/{ticket_no}/read positive")

    r = user.request("GET", f"/users/me/tickets/{ticket_no}/status-logs", params={"page": 1, "size": 200})
    _assert_success(r, (200,), "GET /users/me/tickets/{ticket_no}/status-logs positive")
    _extract_list(r.data)
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/status-logs", params={"size": 201})
    _assert_bad_request(r, "size", "GET /users/me/tickets/{ticket_no}/status-logs size=201")
    r = user.request("GET", f"/users/me/tickets/{_rand_ticket_no()}/status-logs")
    _assert_not_found(r, "工单不存在", "GET /users/me/tickets/{ticket_no}/status-logs not found")

    r = user.request("GET", f"/users/me/tickets/{ticket_no}/reships")
    _assert_success(r, (200,), "GET /users/me/tickets/{ticket_no}/reships positive(empty allowed)")
    _extract_list(r.data)
    r = user.request("GET", f"/users/me/tickets/{_rand_ticket_no()}/reships")
    _assert_not_found(r, "工单不存在", "GET /users/me/tickets/{ticket_no}/reships not found")

    r = user.request(
        "POST",
        "/users/me/ws-sessions",
        json_body={"ticket_nos": [ticket_no], "event_types": ["MESSAGE_CREATED"]},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/ws-sessions missing idem")
    r = user.request(
        "POST",
        "/users/me/ws-sessions",
        json_body={"last_event_id": _str_len(65, "e")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastEventId", "POST /users/me/ws-sessions last_event_id len=65")
    r = user.request(
        "POST",
        "/users/me/ws-sessions",
        json_body={"ticket_ids": [999999999999]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "无权限订阅", "POST /users/me/ws-sessions unauthorized ticket_id")
    r = user.request(
        "POST",
        "/users/me/ws-sessions",
        json_body={"event_types": ["MESSAGE_CREATED"] * 21},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "eventTypes", "POST /users/me/ws-sessions event_types >20")
    r = user.request(
        "POST",
        "/users/me/ws-sessions",
        json_body={"ticket_nos": [ticket_no], "event_types": ["MESSAGE_CREATED"], "last_event_id": _str_len(64, "e")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /users/me/ws-sessions positive")
    user_ws_token = _extract_ws_token(r.data)
    _require(len(user_ws_token) > 20, "user ws_token seems invalid")

    print("[6/17] admin tickets page/detail/patch/assign")
    r = admin.request("GET", "/admin/tickets", params={"page": 1, "size": 200, "ticket_no": ticket_no})
    _assert_success(r, (200,), "GET /admin/tickets positive")
    _extract_list(r.data)
    r = admin.request("GET", "/admin/tickets", params={"size": 201})
    _assert_bad_request(r, "size", "GET /admin/tickets size=201")
    r = admin.request("GET", "/admin/tickets", params={"status": "INVALID"})
    _assert_bad_request(r, "status", "GET /admin/tickets invalid status")
    r = admin.request("GET", "/admin/tickets", params={"claim_external_id": ""})
    _assert_bad_request(r, "claim_external_id", "GET /admin/tickets blank claim_external_id")
    r = admin.request("GET", "/admin/tickets", params={"created_from": "2026-03-05T00:00:00Z", "created_to": "2026-03-01T00:00:00Z"})
    _assert_bad_request(r, "created_from", "GET /admin/tickets invalid created range")

    r = admin.request("GET", f"/admin/tickets/{ticket_id}")
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id} positive")
    admin_detail_ticket_id, admin_detail_ticket_no = _extract_ticket_identity(r.data)
    _require(admin_detail_ticket_id == ticket_id and admin_detail_ticket_no == ticket_no, "admin ticket detail identity mismatch")
    r = admin.request("GET", "/admin/tickets/0")
    _assert_bad_request(r, "ticket_id", "GET /admin/tickets/{ticket_id} ticket_id=0")
    r = admin.request("GET", "/admin/tickets/999999999999")
    _assert_not_found(r, "工单不存在", "GET /admin/tickets/{ticket_id} not found")

    r = admin.request("PATCH", f"/admin/tickets/{ticket_id}", json_body={"priority": "HIGH"})
    _assert_bad_request(r, "Idempotency-Key", "PATCH /admin/tickets/{ticket_id} missing idem")
    r = admin.request("PATCH", f"/admin/tickets/{ticket_id}", json_body={}, headers={"Idempotency-Key": _idem_key()})
    _assert_bad_request(r, "至少需要提供一个可更新字段", "PATCH /admin/tickets/{ticket_id} empty body")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}",
        json_body={"claim_external_id": _str_len(129, "c")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "claimExternalId", "PATCH /admin/tickets/{ticket_id} claim_external_id len=129")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}",
        json_body={"tags": [f"tag-{i}" for i in range(51)]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "tags", "PATCH /admin/tickets/{ticket_id} tags >50")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}",
        json_body={"priority": "HIGH", "tags": ["vip", "urgent"], "currency": "usd", "requested_refund_amount": 1},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "PATCH /admin/tickets/{ticket_id} positive")

    r = admin.request("POST", f"/admin/tickets/{ticket_id}/assign", json_body={"action_type": "ASSIGN", "to_assignee_user_id": admin_user_id})
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/assign missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/assign",
        json_body={"action_type": "ASSIGN"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "toAssigneeUserId", "POST /admin/tickets/{ticket_id}/assign ASSIGN without assignee")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/assign",
        json_body={"action_type": None, "to_assignee_user_id": admin_user_id},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "actionType", "POST /admin/tickets/{ticket_id}/assign action_type null")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/assign",
        json_body={"action_type": "ASSIGN", "to_assignee_user_id": admin_user_id, "note": _str_len(256, "n")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "note", "POST /admin/tickets/{ticket_id}/assign note len=256")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/assign",
        json_body={"action_type": "ASSIGN", "to_assignee_user_id": admin_user_id, "note": "assign to admin"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/tickets/{ticket_id}/assign positive")

    print("[7/17] admin ticket status/messages/read/ws-session")
    r = admin.request("POST", f"/admin/tickets/{ticket_id}/status", json_body={"to_status": "IN_PROGRESS"})
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/status missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/status",
        json_body={"to_status": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "toStatus", "POST /admin/tickets/{ticket_id}/status to_status null")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/status",
        json_body={"to_status": "IN_PROGRESS", "note": "start processing"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/tickets/{ticket_id}/status positive OPEN->IN_PROGRESS")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/status",
        json_body={"to_status": "OPEN"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "不允许流转", "POST /admin/tickets/{ticket_id}/status invalid transition")

    r = admin.request("GET", f"/admin/tickets/{ticket_id}/messages", params={"size": 100})
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id}/messages positive")
    _extract_list(r.data)
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/messages", params={"size": 101})
    _assert_bad_request(r, "size", "GET /admin/tickets/{ticket_id}/messages size=101")
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/messages", params={"before_id": 1, "after_id": 1})
    _assert_conflict(r, "before_id", "GET /admin/tickets/{ticket_id}/messages before+after")
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/messages", params={"order": "bad"})
    _assert_bad_request(r, "order", "GET /admin/tickets/{ticket_id}/messages invalid order")

    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages",
        json_body={"content": "admin hi", "client_message_id": f"admin-cli-{uuid.uuid4().hex[:16]}"},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/messages missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages",
        json_body={"content": None, "attachments": None, "client_message_id": f"admin-cli-{uuid.uuid4().hex[:16]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content 与 attachments", "POST /admin/tickets/{ticket_id}/messages content+attachments null")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages",
        json_body={"content": _str_len(4001, "c"), "client_message_id": f"admin-cli-{uuid.uuid4().hex[:16]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "POST /admin/tickets/{ticket_id}/messages content len=4001")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages",
        json_body={
            "content": "admin message positive",
            "attachments": [],
            "client_message_id": f"admin-cli-{uuid.uuid4().hex[:20]}",
        },
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (201,), "POST /admin/tickets/{ticket_id}/messages positive")
    admin_message_id, admin_message_no = _extract_message_identity(r.data)
    print(f"  admin message: id={admin_message_id}, no={admin_message_no}")

    r = admin.request("PATCH", f"/admin/tickets/{ticket_id}/messages/{admin_message_id}", json_body={"content": "edit"})
    _assert_bad_request(r, "Idempotency-Key", "PATCH /admin/tickets/{ticket_id}/messages/{message_id} missing idem")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}",
        json_body={"content": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "PATCH /admin/tickets/{ticket_id}/messages/{message_id} content null")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}",
        json_body={"content": _str_len(4001, "c")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "content", "PATCH /admin/tickets/{ticket_id}/messages/{message_id} content len=4001")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/0",
        json_body={"content": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "message_id", "PATCH /admin/tickets/{ticket_id}/messages/{message_id} id=0")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/999999999999",
        json_body={"content": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "消息不存在", "PATCH /admin/tickets/{ticket_id}/messages/{message_id} not found")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}",
        json_body={"content": "edited by admin"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "PATCH /admin/tickets/{ticket_id}/messages/{message_id} positive")

    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}/recall",
        json_body={"reason": "recall"},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/messages/{message_id}/recall missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}/recall",
        json_body={"reason": _str_len(256, "r")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "reason", "POST /admin/tickets/{ticket_id}/messages/{message_id}/recall reason len=256")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages/999999999999/recall",
        json_body={"reason": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "消息不存在", "POST /admin/tickets/{ticket_id}/messages/{message_id}/recall not found")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}/recall",
        json_body={"reason": "admin recalled"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/tickets/{ticket_id}/messages/{message_id}/recall positive")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/messages/{admin_message_id}",
        json_body={"content": "edit recalled by admin"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "撤回", "PATCH recalled admin message should conflict")

    r = admin.request("POST", f"/admin/tickets/{ticket_id}/read", json_body={"last_read_message_id": admin_message_id})
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/read missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/read",
        json_body={"last_read_message_id": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId", "POST /admin/tickets/{ticket_id}/read null")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/read",
        json_body={"last_read_message_id": 0},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId", "POST /admin/tickets/{ticket_id}/read 0")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/read",
        json_body={"last_read_message_id": 999999999999},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastReadMessageId 不属于当前工单", "POST /admin/tickets/{ticket_id}/read unrelated message")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/read",
        json_body={"last_read_message_id": admin_message_id},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/tickets/{ticket_id}/read positive")

    r = admin.request(
        "POST",
        "/admin/ws-sessions",
        json_body={"ticket_ids": [ticket_id], "event_types": ["MESSAGE_CREATED"]},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/ws-sessions missing idem")
    r = admin.request(
        "POST",
        "/admin/ws-sessions",
        json_body={"last_event_id": _str_len(65, "e")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "lastEventId", "POST /admin/ws-sessions last_event_id len=65")
    r = admin.request(
        "POST",
        "/admin/ws-sessions",
        json_body={"event_types": ["MESSAGE_CREATED"] * 21},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "eventTypes", "POST /admin/ws-sessions event_types >20")
    r = admin.request(
        "POST",
        "/admin/ws-sessions",
        json_body={"ticket_ids": [999999999999]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "无权限订阅", "POST /admin/ws-sessions unauthorized ticket_id")
    r = admin.request(
        "POST",
        "/admin/ws-sessions",
        json_body={"ticket_ids": [ticket_id], "event_types": ["MESSAGE_CREATED"], "last_event_id": _str_len(64, "e")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/ws-sessions positive")
    admin_ws_token = _extract_ws_token(r.data)
    _require(len(admin_ws_token) > 20, "admin ws_token seems invalid")

    print("[8/17] admin participants + status/assignment logs")
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/participants")
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id}/participants positive")
    participant_rows = _extract_list(r.data)
    owner_row = _find_participant(participant_rows, participant_type="USER", role="OWNER", participant_user_id=user_user_id)
    assignee_row = _find_participant(participant_rows, participant_type="AGENT", role="ASSIGNEE", participant_user_id=admin_user_id)
    _require(owner_row is not None, f"owner participant not found: {participant_rows}")
    _require(assignee_row is not None, f"assignee participant not found: {participant_rows}")
    owner_participant_id = _extract_participant_id(owner_row)
    assignee_participant_id = _extract_participant_id(assignee_row)

    r = admin.request("GET", f"/admin/tickets/{ticket_id}/participants", params={"x": "y"})
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id}/participants boundary(noise query)")
    r = admin.request("GET", "/admin/tickets/0/participants")
    _assert_bad_request(r, "ticket_id", "GET /admin/tickets/{ticket_id}/participants ticket_id=0")
    r = admin.request("GET", "/admin/tickets/999999999999/participants")
    _assert_not_found(r, "工单不存在", "GET /admin/tickets/{ticket_id}/participants not found")

    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": "SYSTEM", "role": "WATCHER"},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/participants missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": None, "role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "participantType", "POST /admin/tickets/{ticket_id}/participants participant_type null")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": "USER", "participant_user_id": admin_user_id, "role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "USER 类型参与方角色只能为 OWNER", "POST /admin/tickets/{ticket_id}/participants USER role invalid")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": "AGENT", "participant_user_id": admin_user_id, "role": "ASSIGNEE"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "ASSIGNEE", "POST /admin/tickets/{ticket_id}/participants ASSIGNEE not allowed")

    participant_create_key = _idem_key()
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": "SYSTEM", "role": "WATCHER"},
        headers={"Idempotency-Key": participant_create_key},
    )
    _assert_success(r, (201,), "POST /admin/tickets/{ticket_id}/participants positive")
    system_participant_id = _extract_participant_id(r.data)

    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants",
        json_body={"participant_type": "SYSTEM", "role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "活跃参与方已存在", "POST /admin/tickets/{ticket_id}/participants duplicate conflict")

    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/{system_participant_id}",
        json_body={"role": "COLLABORATOR"},
    )
    _assert_bad_request(r, "Idempotency-Key", "PATCH /admin/tickets/{ticket_id}/participants/{participant_id} missing idem")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/{system_participant_id}",
        json_body={"role": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "role", "PATCH /admin/tickets/{ticket_id}/participants/{participant_id} role null")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/0",
        json_body={"role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "participant_id", "PATCH /admin/tickets/{ticket_id}/participants/{participant_id} id=0")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/{owner_participant_id}",
        json_body={"role": "OWNER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "USER 类型参与方角色不可修改", "PATCH user owner participant should conflict")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/{system_participant_id}",
        json_body={"role": "COLLABORATOR"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "PATCH /admin/tickets/{ticket_id}/participants/{participant_id} positive")

    r = admin.request("POST", f"/admin/tickets/{ticket_id}/participants/{system_participant_id}/leave")
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/participants/{participant_id}/leave missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants/0/leave",
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "participant_id", "POST /admin/tickets/{ticket_id}/participants/{participant_id}/leave id=0")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants/{owner_participant_id}/leave",
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "OWNER 参与方不允许离开", "POST leave owner participant conflict")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants/{assignee_participant_id}/leave",
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "ASSIGNEE", "POST leave assignee participant conflict")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/participants/{system_participant_id}/leave",
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/tickets/{ticket_id}/participants/{participant_id}/leave positive")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}/participants/{system_participant_id}",
        json_body={"role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "离开会话", "PATCH left participant should conflict")

    r = admin.request("GET", f"/admin/tickets/{ticket_id}/status-logs", params={"page": 1, "size": 200})
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id}/status-logs positive")
    _extract_list(r.data)
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/status-logs", params={"size": 201})
    _assert_bad_request(r, "size", "GET /admin/tickets/{ticket_id}/status-logs size=201")
    r = admin.request("GET", "/admin/tickets/999999999999/status-logs")
    _assert_not_found(r, "工单不存在", "GET /admin/tickets/{ticket_id}/status-logs not found")

    r = admin.request("GET", f"/admin/tickets/{ticket_id}/assignment-logs", params={"page": 1, "size": 200})
    _assert_success(r, (200,), "GET /admin/tickets/{ticket_id}/assignment-logs positive")
    _extract_list(r.data)
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/assignment-logs", params={"size": 201})
    _assert_bad_request(r, "size", "GET /admin/tickets/{ticket_id}/assignment-logs size=201")
    r = admin.request("GET", "/admin/tickets/999999999999/assignment-logs")
    _assert_not_found(r, "工单不存在", "GET /admin/tickets/{ticket_id}/assignment-logs not found")

    print("[9/17] admin reship create/page/detail/patch/status/shipments")
    create_reship_payload = {
        "order_id": args.order_id,
        "reason_code": "LOST",
        "currency": "USD",
        "note": "create by business script",
        "items": [
            {
                "order_item_id": args.order_item_id,
                "sku_id": args.order_item_sku_id,
                "quantity": 1,
            }
        ],
    }

    r = admin.request("POST", f"/admin/tickets/{ticket_id}/reships", json_body=create_reship_payload)
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/tickets/{ticket_id}/reships missing idem")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/reships",
        json_body={**create_reship_payload, "items": []},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "items", "POST /admin/tickets/{ticket_id}/reships items empty")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/reships",
        json_body={
            **create_reship_payload,
            "items": [
                {"order_item_id": args.order_item_id, "sku_id": args.order_item_sku_id, "quantity": 1},
                {"order_item_id": args.order_item_id, "sku_id": args.order_item_sku_id, "quantity": 1},
            ],
        },
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "orderItemId 不允许重复", "POST /admin/tickets/{ticket_id}/reships duplicate order_item")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/reships",
        json_body={**create_reship_payload, "order_id": args.order_id + 1},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "orderId 与工单关联订单不一致", "POST /admin/tickets/{ticket_id}/reships order mismatch")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/reships",
        json_body={
            **create_reship_payload,
            "items": [{"order_item_id": args.order_item_id, "sku_id": args.order_item_sku_id + 999999, "quantity": 1}],
        },
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "skuId 不匹配", "POST /admin/tickets/{ticket_id}/reships sku mismatch")

    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/reships",
        json_body=create_reship_payload,
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (201,), "POST /admin/tickets/{ticket_id}/reships positive")
    reship_id, reship_no = _extract_reship_identity(r.data)
    print(f"  reship created: id={reship_id}, no={reship_no}")

    r = admin.request("GET", "/admin/reships", params={"page": 1, "size": 200, "ticket_id": ticket_id})
    _assert_success(r, (200,), "GET /admin/reships positive")
    _extract_list(r.data)
    r = admin.request("GET", "/admin/reships", params={"size": 201})
    _assert_bad_request(r, "size", "GET /admin/reships size=201")
    r = admin.request("GET", "/admin/reships", params={"status": "INVALID"})
    _assert_bad_request(r, "status", "GET /admin/reships invalid status")
    r = admin.request("GET", "/admin/reships", params={"created_from": "2026-03-05T00:00:00Z", "created_to": "2026-03-01T00:00:00Z"})
    _assert_bad_request(r, "created_from", "GET /admin/reships invalid created range")

    r = admin.request("GET", f"/admin/reships/{reship_id}")
    _assert_success(r, (200,), "GET /admin/reships/{reship_id} positive")
    detail_reship_id, detail_reship_no = _extract_reship_identity(r.data)
    _require(detail_reship_id == reship_id and detail_reship_no == reship_no, "reship detail identity mismatch")
    r = admin.request("GET", "/admin/reships/0")
    _assert_bad_request(r, "reship_id", "GET /admin/reships/{reship_id} id=0")
    r = admin.request("GET", "/admin/reships/999999999999")
    _assert_not_found(r, "补发单不存在", "GET /admin/reships/{reship_id} not found")

    r = admin.request("PATCH", f"/admin/reships/{reship_id}", json_body={"note": "x"})
    _assert_bad_request(r, "Idempotency-Key", "PATCH /admin/reships/{reship_id} missing idem")
    r = admin.request("PATCH", f"/admin/reships/{reship_id}", json_body={}, headers={"Idempotency-Key": _idem_key()})
    _assert_bad_request(r, "至少需要提供一个可更新字段", "PATCH /admin/reships/{reship_id} empty body")
    r = admin.request(
        "PATCH",
        f"/admin/reships/{reship_id}",
        json_body={"items_cost": "-1"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "itemsCost", "PATCH /admin/reships/{reship_id} items_cost negative")
    r = admin.request(
        "PATCH",
        f"/admin/reships/{reship_id}",
        json_body={"items_cost": "abc"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "itemsCost", "PATCH /admin/reships/{reship_id} items_cost invalid format")
    r = admin.request(
        "PATCH",
        f"/admin/reships/{reship_id}",
        json_body={"note": _str_len(256, "n")},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "note", "PATCH /admin/reships/{reship_id} note len=256")
    r = admin.request(
        "PATCH",
        f"/admin/reships/{reship_id}",
        json_body={"currency": "usd", "items_cost": "0", "shipping_cost": "0.00", "note": "patched"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "PATCH /admin/reships/{reship_id} positive")
    r = admin.request(
        "PATCH",
        "/admin/reships/999999999999",
        json_body={"note": "x"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "补发单不存在", "PATCH /admin/reships/{reship_id} not found")

    r = admin.request("POST", f"/admin/reships/{reship_id}/status", json_body={"to_status": "APPROVED"})
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/reships/{reship_id}/status missing idem")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/status",
        json_body={"to_status": None},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "toStatus", "POST /admin/reships/{reship_id}/status to_status null")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/status",
        json_body={"to_status": "FULFILLED"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "不允许流转", "POST /admin/reships/{reship_id}/status invalid transition")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/status",
        json_body={"to_status": "APPROVED", "note": "approved"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/reships/{reship_id}/status positive INIT->APPROVED")
    r = admin.request(
        "POST",
        "/admin/reships/999999999999/status",
        json_body={"to_status": "APPROVED"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "补发单不存在", "POST /admin/reships/{reship_id}/status not found")

    r = admin.request("GET", f"/admin/reships/{reship_id}/shipments")
    _assert_success(r, (200,), "GET /admin/reships/{reship_id}/shipments positive")
    _extract_list(r.data)
    r = admin.request("GET", "/admin/reships/0/shipments")
    _assert_bad_request(r, "reship_id", "GET /admin/reships/{reship_id}/shipments id=0")
    r = admin.request("GET", "/admin/reships/999999999999/shipments")
    _assert_not_found(r, "补发单不存在", "GET /admin/reships/{reship_id}/shipments not found")

    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/shipments",
        json_body={"shipment_ids": [args.reship_bind_shipment_id]},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/reships/{reship_id}/shipments missing idem")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/shipments",
        json_body={"shipment_ids": []},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "shipmentIds", "POST /admin/reships/{reship_id}/shipments empty")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/shipments",
        json_body={"shipment_ids": [None]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_bad_request(r, "shipmentIds", "POST /admin/reships/{reship_id}/shipments contains null")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/shipments",
        json_body={"shipment_ids": [999999999999]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "物流单", "POST /admin/reships/{reship_id}/shipments not found shipment")

    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/shipments",
        json_body={"shipment_ids": [args.reship_bind_shipment_id]},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /admin/reships/{reship_id}/shipments positive")
    bound_shipments = _extract_list(r.data)
    has_bound = False
    for item in bound_shipments:
        if isinstance(item, dict) and _pick(item, "shipment_id") == args.reship_bind_shipment_id:
            has_bound = True
            break
    _require(has_bound, f"bound shipment not found in response: {bound_shipments}")

    print("[10/17] user reships list should include bound shipment")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/reships")
    _assert_success(r, (200,), "GET /users/me/tickets/{ticket_no}/reships after bind")
    rows = _extract_list(r.data)
    seen_bound = False
    for item in rows:
        if isinstance(item, dict) and _pick(item, "shipment_id") == args.reship_bind_shipment_id:
            seen_bound = True
            break
    _require(seen_bound, f"user reships does not contain bound shipment_id={args.reship_bind_shipment_id}, rows={rows}")

    print("[11/17] close ticket positive + conflict after closed")
    close_note_255 = _str_len(255, "n")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/close",
        json_body={"note": close_note_255},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_success(r, (200,), "POST /users/me/tickets/{ticket_no}/close positive boundary note=255")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/messages",
        json_body={"content": "after closed", "client_message_id": f"after-close-{uuid.uuid4().hex[:12]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "工单已关闭", "POST /users/me/tickets/{ticket_no}/messages after closed")
    r = admin.request(
        "POST",
        f"/admin/tickets/{ticket_id}/messages",
        json_body={"content": "after closed", "client_message_id": f"admin-after-close-{uuid.uuid4().hex[:12]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_conflict(r, "工单已关闭", "POST /admin/tickets/{ticket_id}/messages after closed")

    print("[12/17] smoke checks for not-found on remaining id-based endpoints")
    r = admin.request("POST", "/admin/tickets/999999999999/read", json_body={"last_read_message_id": 1}, headers={"Idempotency-Key": _idem_key()})
    _assert_not_found(r, "工单不存在", "POST /admin/tickets/{ticket_id}/read not found")
    r = admin.request(
        "POST",
        "/admin/tickets/999999999999/messages",
        json_body={"content": "x", "client_message_id": f"nf-{uuid.uuid4().hex[:8]}"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "工单不存在", "POST /admin/tickets/{ticket_id}/messages not found")
    r = admin.request(
        "POST",
        "/admin/tickets/999999999999/participants",
        json_body={"participant_type": "SYSTEM", "role": "WATCHER"},
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "工单不存在", "POST /admin/tickets/{ticket_id}/participants not found")
    r = admin.request(
        "POST",
        "/admin/tickets/999999999999/reships",
        json_body=create_reship_payload,
        headers={"Idempotency-Key": _idem_key()},
    )
    _assert_not_found(r, "工单不存在", "POST /admin/tickets/{ticket_id}/reships not found")

    print("[13/17] idempotency-key boundary on selected write APIs")
    r = user.request(
        "POST",
        f"/users/me/tickets/{ticket_no}/read",
        json_body={"last_read_message_id": user_message_id},
        headers={"Idempotency-Key": _idem_key(65)},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /users/me/tickets/{ticket_no}/read idem len=65")
    r = admin.request(
        "PATCH",
        f"/admin/tickets/{ticket_id}",
        json_body={"priority": "NORMAL"},
        headers={"Idempotency-Key": _idem_key(65)},
    )
    _assert_bad_request(r, "Idempotency-Key", "PATCH /admin/tickets/{ticket_id} idem len=65")
    r = admin.request(
        "POST",
        f"/admin/reships/{reship_id}/status",
        json_body={"to_status": "CANCELLED"},
        headers={"Idempotency-Key": _idem_key(65)},
    )
    _assert_bad_request(r, "Idempotency-Key", "POST /admin/reships/{reship_id}/status idem len=65")

    print("[14/17] list APIs page boundary (page=0)")
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/status-logs", params={"page": 0, "size": 20})
    _assert_bad_request(r, "page", "GET /admin/tickets/{ticket_id}/status-logs page=0")
    r = admin.request("GET", f"/admin/tickets/{ticket_id}/assignment-logs", params={"page": 0, "size": 20})
    _assert_bad_request(r, "page", "GET /admin/tickets/{ticket_id}/assignment-logs page=0")
    r = user.request("GET", f"/users/me/tickets/{ticket_no}/status-logs", params={"page": 0, "size": 20})
    _assert_bad_request(r, "page", "GET /users/me/tickets/{ticket_no}/status-logs page=0")

    print("[15/17] basic security behavior smoke (unauthorized)")
    anon = ApiClient(args.base_url, timeout_s=args.timeout_s, user_agent="international-shopping-customerservice-anon-test/1.0")
    r = anon.request("GET", "/users/me/tickets", with_csrf=False)
    _require(r.http_status in (401, 403), f"anon GET /users/me/tickets expect 401/403, got={r.http_status}, body={_pretty(r.body)}")
    _require(r.code in ("UNAUTHORIZED", "FORBIDDEN"), f"anon GET /users/me/tickets code expect UNAUTHORIZED/FORBIDDEN, got={r.code}")

    print("[16/17] ws tokens + ws handshake smoke checks")
    _require("." in user_ws_token and "." in admin_ws_token, "ws_token should be JWT-like format")
    _require(len(user_ws_token.split(".")) == 3, "user ws_token segments should be 3")
    _require(len(admin_ws_token.split(".")) == 3, "admin ws_token segments should be 3")
    ws_smoke = user.request(
        "GET",
        "/ws/customerservice",
        params={"ws_token": user_ws_token, "ticket_no": ticket_no},
        with_csrf=False,
    )
    _require(
        ws_smoke.http_status < 500,
        f"GET /ws/customerservice handshake smoke should not return 5xx, got={ws_smoke.http_status}, body={_pretty(ws_smoke.body)}",
    )

    print("[17/17] done")
    print("DONE.")
    print(
        "summary: "
        f"ticket_id={ticket_id}, ticket_no={ticket_no}, "
        f"user_message_id={user_message_id}, admin_message_id={admin_message_id}, "
        f"reship_id={reship_id}, reship_no={reship_no}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as err:
        print(f"HTTP error: {err}", file=sys.stderr)
        raise

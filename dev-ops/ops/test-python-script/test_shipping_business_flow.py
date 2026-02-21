import argparse
import hashlib
import json
import sys
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple

import requests


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
    def data(self) -> Any:
        return self.body.get("data")


class ApiClient:
    def __init__(self, base_url: str, timeout_s: int = 30):
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s
        self.session = requests.Session()
        self.session.headers.update(
            {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "User-Agent": "international-shopping-shipping-business-test/1.0",
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

    def admin_manual_create(self, payload: Dict[str, Any], idempotency_key: str) -> ApiResult:
        return self.request(
            "POST",
            "/admin/shipments/manual-create",
            json_body=payload,
            headers={"Idempotency-Key": idempotency_key},
        )

    def admin_fill_label(self, shipment_id: int, payload: Dict[str, Any], idempotency_key: str) -> ApiResult:
        return self.request(
            "POST",
            f"/admin/shipments/{shipment_id}/label",
            json_body=payload,
            headers={"Idempotency-Key": idempotency_key},
        )

    def admin_dispatch(self, shipment_ids: List[int], note: str, idempotency_key: str) -> ApiResult:
        return self.request(
            "POST",
            "/admin/shipments/dispatch",
            json_body={"shipment_ids": shipment_ids, "note": note},
            headers={"Idempotency-Key": idempotency_key},
        )

    def admin_shipment_detail(self, shipment_id: int) -> ApiResult:
        return self.request("GET", f"/admin/shipments/{shipment_id}")

    def admin_shipments_page(self, params: Dict[str, Any]) -> ApiResult:
        return self.request("GET", "/admin/shipments", params=params)

    def admin_status_logs_page(self, params: Dict[str, Any]) -> ApiResult:
        return self.request("GET", "/admin/shipment-status-logs", params=params)

    def user_order_shipments(self, order_no: str, include_logs: bool) -> ApiResult:
        return self.request(
            "GET",
            f"/users/me/orders/{order_no}/shipments",
            params={"include_logs": str(include_logs).lower()},
        )

    def user_shipment_detail(self, shipment_no: str) -> ApiResult:
        return self.request("GET", f"/users/me/shipments/{shipment_no}")

    def webhook_17track(self, sign: str, raw_body: str) -> ApiResult:
        return self.request(
            "POST",
            "/webhooks/17track",
            raw_body=raw_body,
            headers={"sign": sign, "Content-Type": "application/json"},
            with_csrf=False,
        )


def _extract_shipment_identity(data: Any) -> Tuple[int, str]:
    _require(isinstance(data, dict), f"shipment data is not object: {data}")
    shipment_id = _pick(data, "id")
    shipment_no = _pick(data, "shipment_no")
    _require(isinstance(shipment_id, int), f"shipment.id invalid: {data}")
    _require(bool(shipment_no), f"shipment_no missing: {data}")
    return int(shipment_id), str(shipment_no)


def _extract_status(data: Any) -> str:
    _require(isinstance(data, dict), f"shipment data is not object: {data}")
    status = _pick(data, "status")
    _require(bool(status), f"shipment.status missing: {data}")
    return str(status).upper()


def _extract_tracking_no(data: Any) -> str:
    _require(isinstance(data, dict), f"shipment data is not object: {data}")
    tracking_no = _pick(data, "tracking_no")
    _require(bool(tracking_no), f"tracking_no missing: {data}")
    return str(tracking_no)


def _extract_status_logs(item: Dict[str, Any]) -> Optional[List[Any]]:
    value = _pick(item, "status_logs")
    if value is None:
        return None
    _require(isinstance(value, list), f"status_logs should be list or null: {item}")
    return value


def _find_shipment(items: Any, shipment_no: str) -> Optional[Dict[str, Any]]:
    if not isinstance(items, list):
        return None
    target = shipment_no.strip()
    for item in items:
        if not isinstance(item, dict):
            continue
        if str(_pick(item, "shipment_no") or "").strip() == target:
            return item
    return None


def _extract_dispatch_shipment_nos(data: Any) -> List[str]:
    if not isinstance(data, dict):
        return []
    value = _pick(data, "shipment_ids")
    if not isinstance(value, list):
        return []
    out: List[str] = []
    for item in value:
        if item is None:
            continue
        out.append(str(item))
    return out


def _admin_log_count(client: ApiClient, shipment_id: int, source_ref: str) -> int:
    r = client.admin_status_logs_page(
        {
            "shipment_id": shipment_id,
            "source_ref": source_ref,
            "page": 1,
            "size": 200,
            "sort": "created_at,asc",
        }
    )
    _require(r.http_status == 200 and r.success, f"status logs query failed: {r.http_status}, body={_pretty(r.body)}")
    rows = r.data if isinstance(r.data, list) else []
    count = 0
    for row in rows:
        if isinstance(row, dict) and str(_pick(row, "source_ref") or "") == source_ref:
            count += 1
    return count


def _sha256_hex(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _build_webhook_payload(tracking_no: str, carrier_code: str, event: str, sub_status: str) -> Dict[str, Any]:
    iso_time = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    return {
        "event": event,
        "data": {
            "number": tracking_no,
            "carrier": carrier_code,
            "track_info": {
                "latest_status": {"sub_status": sub_status},
                "latest_event": {"time_iso": iso_time},
            },
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Business flow tests for shipping domain APIs.")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1", help="API base URL")

    parser.add_argument("--user-account", required=True)
    parser.add_argument("--user-password", required=True)
    parser.add_argument("--admin-account", required=True)
    parser.add_argument("--admin-password", required=True)

    parser.add_argument("--paid-order-no", required=True, help="A PAID order_no that belongs to --user-account")
    parser.add_argument("--ship-from-address-id", type=int, default=1)

    parser.add_argument("--currency", default="USD")
    parser.add_argument("--declared-value", default="100.00")
    parser.add_argument("--carrier-code", required=True, help="17Track accepted carrier code")
    parser.add_argument("--carrier-name", required=True)
    parser.add_argument("--service-code", default="GROUND")
    parser.add_argument("--label-url", default="https://example.com/labels/shipping-business-test.pdf")
    parser.add_argument("--weight-kg", default="1.234")
    parser.add_argument("--length-cm", default="20.0")
    parser.add_argument("--width-cm", default="10.0")
    parser.add_argument("--height-cm", default="8.0")

    parser.add_argument("--webhook-key", default="", help="17Track webhook key, empty means skip webhook tests")
    parser.add_argument("--webhook-event", default="TRACKING_UPDATED")
    parser.add_argument("--webhook-sub-status", default="InTransit_PickedUp")
    parser.add_argument("--expected-webhook-status", default="PICKED_UP")
    parser.add_argument("--skip-webhook", action="store_true")
    parser.add_argument("--skip-multi-shipment-check", action="store_true")

    parser.add_argument("--timeout-s", type=int, default=30)
    args = parser.parse_args()

    admin = ApiClient(args.base_url, timeout_s=args.timeout_s)
    user = ApiClient(args.base_url, timeout_s=args.timeout_s)

    print("[1/10] login user/admin and issue csrf")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    print("[2/10] manual-create placeholder shipment (no label)")
    create_idem_1 = str(uuid.uuid4())
    create_payload_1 = {
        "ship_from_address_id": args.ship_from_address_id,
        "order_no": args.paid_order_no,
        "declared_value": str(args.declared_value),
        "currency": args.currency,
    }
    created_1 = admin.admin_manual_create(create_payload_1, create_idem_1)
    _require(created_1.http_status in (200, 201), f"manual-create #1 http_status={created_1.http_status}, body={_pretty(created_1.body)}")
    _require(created_1.success, f"manual-create #1 success=false, body={_pretty(created_1.body)}")

    shipment_id_1, shipment_no_1 = _extract_shipment_identity(created_1.data)
    status_1 = _extract_status(created_1.data)
    _require(status_1 == "CREATED", f"manual-create #1 status should be CREATED, got {status_1}")
    print(f"  shipment_1: id={shipment_id_1}, shipment_no={shipment_no_1}, status={status_1}")

    print("[3/10] fill label, verify status keeps CREATED")
    label_idem = str(uuid.uuid4())
    tracking_no = f"TRK{int(time.time() * 1000)}{uuid.uuid4().hex[:10]}"
    ext_external_id = f"ext-{uuid.uuid4().hex[:24]}"
    label_payload = {
        "ship_from_address_id": args.ship_from_address_id,
        "carrier_code": args.carrier_code,
        "carrier_name": args.carrier_name,
        "service_code": args.service_code,
        "tracking_no": tracking_no,
        "ext_external_id": ext_external_id,
        "label_url": args.label_url,
        "weight_kg": args.weight_kg,
        "length_cm": args.length_cm,
        "width_cm": args.width_cm,
        "height_cm": args.height_cm,
        "declared_value": str(args.declared_value),
        "currency": args.currency,
    }
    labeled = admin.admin_fill_label(shipment_id_1, label_payload, label_idem)
    _require(labeled.http_status == 200, f"fill-label http_status={labeled.http_status}, body={_pretty(labeled.body)}")
    _require(labeled.success, f"fill-label success=false, body={_pretty(labeled.body)}")

    status_after_label = _extract_status(labeled.data)
    tracking_after_label = _extract_tracking_no(labeled.data)
    _require(status_after_label == "CREATED", f"fill-label should not advance status, got {status_after_label}")
    _require(tracking_after_label == tracking_no, f"tracking_no mismatch after fill-label, expected={tracking_no}, got={tracking_after_label}")

    print("[4/10] dispatch shipment, verify status=LABEL_CREATED")
    dispatch_idem = str(uuid.uuid4())
    dispatch = admin.admin_dispatch([shipment_id_1], "shipping-business-flow-dispatch", dispatch_idem)
    _require(dispatch.http_status == 200, f"dispatch http_status={dispatch.http_status}, body={_pretty(dispatch.body)}")
    _require(dispatch.success, f"dispatch success=false, body={_pretty(dispatch.body)}")

    dispatch_nos = _extract_dispatch_shipment_nos(dispatch.data)
    _require(shipment_no_1 in dispatch_nos, f"dispatch response should contain shipment_no={shipment_no_1}, got={dispatch_nos}")

    detail_after_dispatch = admin.admin_shipment_detail(shipment_id_1)
    _require(
        detail_after_dispatch.http_status == 200 and detail_after_dispatch.success,
        f"admin shipment detail after dispatch failed: {detail_after_dispatch.http_status}, body={_pretty(detail_after_dispatch.body)}",
    )
    status_after_dispatch = _extract_status(detail_after_dispatch.data)
    _require(status_after_dispatch == "LABEL_CREATED", f"status after dispatch should be LABEL_CREATED, got {status_after_dispatch}")

    print("[5/10] admin shipment page query by order_no")
    page = admin.admin_shipments_page(
        {
            "order_no": args.paid_order_no,
            "status_in": ["CREATED", "LABEL_CREATED", "PICKED_UP", "IN_TRANSIT", "EXCEPTION"],
            "page": 1,
            "size": 100,
            "sort": "updated_at,desc",
        }
    )
    _require(page.http_status == 200 and page.success, f"admin shipments page failed: {page.http_status}, body={_pretty(page.body)}")
    page_items = page.data if isinstance(page.data, list) else []
    _require(_find_shipment(page_items, shipment_no_1) is not None, f"shipment {shipment_no_1} not found in admin shipments page")

    print("[6/10] user-side shipment list/detail checks")
    user_list_with_logs = user.user_order_shipments(args.paid_order_no, include_logs=True)
    _require(
        user_list_with_logs.http_status == 200 and user_list_with_logs.success,
        f"user order shipments(include_logs=true) failed: {user_list_with_logs.http_status}, body={_pretty(user_list_with_logs.body)}",
    )

    with_logs_item = _find_shipment(user_list_with_logs.data, shipment_no_1)
    _require(with_logs_item is not None, f"shipment {shipment_no_1} not found in user list(include_logs=true)")
    with_logs = _extract_status_logs(with_logs_item)
    _require(with_logs is not None and len(with_logs) >= 1, f"status_logs should be present when include_logs=true, item={_pretty(with_logs_item)}")

    user_list_without_logs = user.user_order_shipments(args.paid_order_no, include_logs=False)
    _require(
        user_list_without_logs.http_status == 200 and user_list_without_logs.success,
        f"user order shipments(include_logs=false) failed: {user_list_without_logs.http_status}, body={_pretty(user_list_without_logs.body)}",
    )

    without_logs_item = _find_shipment(user_list_without_logs.data, shipment_no_1)
    _require(without_logs_item is not None, f"shipment {shipment_no_1} not found in user list(include_logs=false)")
    without_logs = _extract_status_logs(without_logs_item)
    _require(without_logs is None or len(without_logs) == 0, f"status_logs should be empty when include_logs=false, item={_pretty(without_logs_item)}")

    user_detail = user.user_shipment_detail(shipment_no_1)
    _require(
        user_detail.http_status == 200 and user_detail.success,
        f"user shipment detail failed: {user_detail.http_status}, body={_pretty(user_detail.body)}",
    )
    detail_id, detail_no = _extract_shipment_identity(user_detail.data)
    _require(detail_id == shipment_id_1 and detail_no == shipment_no_1, f"user detail mismatch: expected id/no={shipment_id_1}/{shipment_no_1}, got={detail_id}/{detail_no}")

    print("[7/10] verify single status-log entry per write source_ref")
    label_source_ref = f"admin:shipment:label:{shipment_id_1}:{label_idem}"
    dispatch_source_ref = f"admin:shipment:dispatch:{dispatch_idem}:{shipment_id_1}"

    label_log_count = _admin_log_count(admin, shipment_id_1, label_source_ref)
    dispatch_log_count = _admin_log_count(admin, shipment_id_1, dispatch_source_ref)
    _require(label_log_count == 1, f"label source_ref should have exactly one log, got {label_log_count}, source_ref={label_source_ref}")
    _require(dispatch_log_count == 1, f"dispatch source_ref should have exactly one log, got {dispatch_log_count}, source_ref={dispatch_source_ref}")

    print("[8/10] verify same order can have multiple shipments (split shipment scenario)")
    shipment_id_2: Optional[int] = None
    shipment_no_2: Optional[str] = None
    if args.skip_multi_shipment_check:
        print("  skipped by --skip-multi-shipment-check")
    else:
        create_idem_2 = str(uuid.uuid4())
        create_payload_2 = {
            "ship_from_address_id": args.ship_from_address_id,
            "order_no": args.paid_order_no,
            "declared_value": str(args.declared_value),
            "currency": args.currency,
        }
        created_2 = admin.admin_manual_create(create_payload_2, create_idem_2)
        _require(created_2.http_status in (200, 201), f"manual-create #2 http_status={created_2.http_status}, body={_pretty(created_2.body)}")
        _require(created_2.success, f"manual-create #2 success=false, body={_pretty(created_2.body)}")

        shipment_id_2, shipment_no_2 = _extract_shipment_identity(created_2.data)
        _require(shipment_id_2 != shipment_id_1, f"multi-shipment check failed, second shipment id duplicated: {shipment_id_2}")
        print(f"  shipment_2: id={shipment_id_2}, shipment_no={shipment_no_2}")

        list_after_second_create = user.user_order_shipments(args.paid_order_no, include_logs=True)
        _require(
            list_after_second_create.http_status == 200 and list_after_second_create.success,
            (
                "user shipment list after second create failed: "
                f"{list_after_second_create.http_status}, body={_pretty(list_after_second_create.body)}"
            ),
        )
        _require(
            _find_shipment(list_after_second_create.data, shipment_no_1) is not None
            and _find_shipment(list_after_second_create.data, shipment_no_2) is not None,
            f"split shipment check failed, expected both shipment_no in user list: {shipment_no_1}, {shipment_no_2}",
        )

    print("[9/10] webhook sequential duplicate idempotency")
    if args.skip_webhook or not args.webhook_key:
        print("  skipped (provide --webhook-key and do not set --skip-webhook to run)")
    else:
        webhook_payload = _build_webhook_payload(
            tracking_no=tracking_no,
            carrier_code=args.carrier_code,
            event=args.webhook_event,
            sub_status=args.webhook_sub_status,
        )
        raw_body = json.dumps(webhook_payload, ensure_ascii=False, separators=(",", ":"))
        sign = _sha256_hex(raw_body + "/" + args.webhook_key)

        webhook_1 = admin.webhook_17track(sign, raw_body)
        _require(webhook_1.http_status == 200 and webhook_1.success, f"webhook first call failed: {webhook_1.http_status}, body={_pretty(webhook_1.body)}")

        webhook_2 = admin.webhook_17track(sign, raw_body)
        _require(webhook_2.http_status == 200 and webhook_2.success, f"webhook duplicate call failed: {webhook_2.http_status}, body={_pretty(webhook_2.body)}")

        detail_after_webhook = admin.admin_shipment_detail(shipment_id_1)
        _require(
            detail_after_webhook.http_status == 200 and detail_after_webhook.success,
            f"admin detail after webhook failed: {detail_after_webhook.http_status}, body={_pretty(detail_after_webhook.body)}",
        )
        status_after_webhook = _extract_status(detail_after_webhook.data)
        if args.expected_webhook_status:
            _require(
                status_after_webhook == str(args.expected_webhook_status).upper(),
                f"webhook status mismatch, expected={args.expected_webhook_status}, got={status_after_webhook}",
            )

        webhook_source_ref = "17track:" + _sha256_hex(raw_body)
        webhook_log_count = _admin_log_count(admin, shipment_id_1, webhook_source_ref)
        _require(webhook_log_count == 1, f"webhook source_ref should have exactly one log, got {webhook_log_count}")

    print("[10/10] done")
    print("DONE.")
    print(
        f"summary: shipment_1={shipment_id_1}/{shipment_no_1}, "
        f"shipment_2={shipment_id_2}/{shipment_no_2}, tracking_no={tracking_no}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as err:
        print(f"HTTP error: {err}", file=sys.stderr)
        raise

import argparse
import hashlib
import json
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Callable, Dict, List, Optional, Tuple

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
                "User-Agent": "international-shopping-shipping-idem-test/1.0",
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

    def admin_status_logs_page(self, params: Dict[str, Any]) -> ApiResult:
        return self.request("GET", "/admin/shipment-status-logs", params=params)

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


def _extract_dispatch_nos(data: Any) -> List[str]:
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


def _run_concurrent(n: int, task: Callable[[], ApiResult]) -> List[ApiResult]:
    workers = max(1, int(n))
    results: List[ApiResult] = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futs = [pool.submit(task) for _ in range(workers)]
        for fut in as_completed(futs):
            results.append(fut.result())
    return results


def _sha256_hex(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _build_webhook_payload(tracking_no: str, carrier_code: str) -> Dict[str, Any]:
    iso_time = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    return {
        "event": "TRACKING_UPDATED",
        "data": {
            "number": tracking_no,
            "carrier": carrier_code,
            "track_info": {
                "latest_status": {"sub_status": "InTransit_PickedUp"},
                "latest_event": {"time_iso": iso_time},
            },
        },
    }


def _success_results(results: List[ApiResult], http_status_set: Tuple[int, ...]) -> List[ApiResult]:
    return [r for r in results if r.http_status in http_status_set and r.success]


def main() -> int:
    parser = argparse.ArgumentParser(description="Idempotency and concurrency tests for shipping domain APIs.")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1", help="API base URL")

    parser.add_argument("--admin-account", required=True)
    parser.add_argument("--admin-password", required=True)

    parser.add_argument("--paid-order-no", required=True, help="A PAID order_no")
    parser.add_argument("--ship-from-address-id", type=int, default=1)

    parser.add_argument("--currency", default="USD")
    parser.add_argument("--declared-value", default="100.00")
    parser.add_argument("--carrier-code", required=True, help="17Track accepted carrier code")
    parser.add_argument("--carrier-name", required=True)
    parser.add_argument("--service-code", default="GROUND")
    parser.add_argument("--label-url", default="https://example.com/labels/shipping-idem-test.pdf")
    parser.add_argument("--weight-kg", default="1.234")
    parser.add_argument("--length-cm", default="20.0")
    parser.add_argument("--width-cm", default="10.0")
    parser.add_argument("--height-cm", default="8.0")

    parser.add_argument("--create-concurrency", type=int, default=10)
    parser.add_argument("--label-concurrency", type=int, default=10)
    parser.add_argument("--dispatch-concurrency", type=int, default=10)
    parser.add_argument("--webhook-concurrency", type=int, default=10)

    parser.add_argument("--webhook-key", default="", help="17Track webhook key, empty means skip webhook concurrency tests")
    parser.add_argument("--strict-webhook-dup-200", action="store_true", help="Require all concurrent duplicate webhooks to return 200 success")
    parser.add_argument("--timeout-s", type=int, default=30)
    args = parser.parse_args()

    admin = ApiClient(args.base_url, timeout_s=args.timeout_s)

    print("[1/7] login admin and issue csrf")
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    print("[2/7] create placeholder shipment for label/dispatch concurrency")
    seed_create_idem = str(uuid.uuid4())
    seed_payload = {
        "ship_from_address_id": args.ship_from_address_id,
        "order_no": args.paid_order_no,
        "declared_value": str(args.declared_value),
        "currency": args.currency,
    }
    seed_created = admin.admin_manual_create(seed_payload, seed_create_idem)
    _require(seed_created.http_status in (200, 201), f"seed manual-create failed: {seed_created.http_status}, body={_pretty(seed_created.body)}")
    _require(seed_created.success, f"seed manual-create success=false: {_pretty(seed_created.body)}")

    seed_shipment_id, seed_shipment_no = _extract_shipment_identity(seed_created.data)
    print(f"  seed_shipment: id={seed_shipment_id}, shipment_no={seed_shipment_no}")

    print("[3/7] concurrent fill-label with same Idempotency-Key")
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

    def _fill_once() -> ApiResult:
        return admin.admin_fill_label(seed_shipment_id, label_payload, label_idem)

    fill_results = _run_concurrent(args.label_concurrency, _fill_once)
    fill_ok = _success_results(fill_results, (200,))
    _require(len(fill_ok) == len(fill_results), f"fill-label has non-success responses: {[ (r.http_status, r.code) for r in fill_results ]}")

    fill_identity = sorted({_extract_shipment_identity(r.data) for r in fill_ok})
    _require(len(fill_identity) == 1, f"fill-label idempotency violated, identities={fill_identity}")

    detail_after_label = admin.admin_shipment_detail(seed_shipment_id)
    _require(detail_after_label.http_status == 200 and detail_after_label.success, f"detail after fill-label failed: {detail_after_label.http_status}, body={_pretty(detail_after_label.body)}")
    status_after_label = _extract_status(detail_after_label.data)
    _require(status_after_label == "CREATED", f"fill-label should not advance status, got {status_after_label}")
    _require(_extract_tracking_no(detail_after_label.data) == tracking_no, "tracking_no mismatch after fill-label")

    label_source_ref = f"admin:shipment:label:{seed_shipment_id}:{label_idem}"
    label_log_count = _admin_log_count(admin, seed_shipment_id, label_source_ref)
    _require(label_log_count == 1, f"fill-label source_ref should have exactly one log, got {label_log_count}")

    print("[4/7] concurrent dispatch with same Idempotency-Key")
    dispatch_idem = str(uuid.uuid4())

    def _dispatch_once() -> ApiResult:
        return admin.admin_dispatch([seed_shipment_id], "shipping-idem-concurrency-dispatch", dispatch_idem)

    dispatch_results = _run_concurrent(args.dispatch_concurrency, _dispatch_once)
    dispatch_ok = _success_results(dispatch_results, (200,))
    _require(len(dispatch_ok) == len(dispatch_results), f"dispatch has non-success responses: {[ (r.http_status, r.code) for r in dispatch_results ]}")

    for r in dispatch_ok:
        shipment_nos = _extract_dispatch_nos(r.data)
        _require(seed_shipment_no in shipment_nos, f"dispatch response missing target shipment_no={seed_shipment_no}, got={shipment_nos}")

    detail_after_dispatch = admin.admin_shipment_detail(seed_shipment_id)
    _require(detail_after_dispatch.http_status == 200 and detail_after_dispatch.success, f"detail after dispatch failed: {detail_after_dispatch.http_status}, body={_pretty(detail_after_dispatch.body)}")
    status_after_dispatch = _extract_status(detail_after_dispatch.data)
    _require(status_after_dispatch == "LABEL_CREATED", f"status after dispatch should be LABEL_CREATED, got {status_after_dispatch}")

    dispatch_source_ref = f"admin:shipment:dispatch:{dispatch_idem}:{seed_shipment_id}"
    dispatch_log_count = _admin_log_count(admin, seed_shipment_id, dispatch_source_ref)
    _require(dispatch_log_count == 1, f"dispatch source_ref should have exactly one log, got {dispatch_log_count}")

    print("[5/7] concurrent manual-create with same Idempotency-Key")
    race_create_idem = str(uuid.uuid4())
    race_payload = {
        "ship_from_address_id": args.ship_from_address_id,
        "order_no": args.paid_order_no,
        "declared_value": str(args.declared_value),
        "currency": args.currency,
    }

    def _manual_create_once() -> ApiResult:
        return admin.admin_manual_create(race_payload, race_create_idem)

    create_results = _run_concurrent(args.create_concurrency, _manual_create_once)
    create_ok = _success_results(create_results, (200, 201))
    _require(len(create_ok) >= 1, f"manual-create no success response: {[ (r.http_status, r.code) for r in create_results ]}")

    create_identity = sorted({_extract_shipment_identity(r.data) for r in create_ok})
    _require(len(create_identity) == 1, f"manual-create idempotency violated, identities={create_identity}")

    race_shipment_id, race_shipment_no = create_identity[0]
    print(f"  concurrent-manual-create shipment: id={race_shipment_id}, shipment_no={race_shipment_no}")

    create_source_ref = f"admin:shipment:manual:create:{race_create_idem}"
    create_log_count = _admin_log_count(admin, race_shipment_id, create_source_ref)
    _require(create_log_count == 1, f"manual-create source_ref should have exactly one log, got {create_log_count}")

    print("[6/7] webhook duplicate concurrency")
    if not args.webhook_key:
        print("  skipped (provide --webhook-key to run)")
    else:
        payload = _build_webhook_payload(tracking_no=tracking_no, carrier_code=args.carrier_code)
        raw_body = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        sign = _sha256_hex(raw_body + "/" + args.webhook_key)

        def _webhook_once() -> ApiResult:
            return admin.webhook_17track(sign, raw_body)

        webhook_results = _run_concurrent(args.webhook_concurrency, _webhook_once)

        success_200 = [r for r in webhook_results if r.http_status == 200 and r.success]
        conflict_409 = [r for r in webhook_results if r.http_status == 409 and not r.success]
        others = [
            r
            for r in webhook_results
            if not ((r.http_status == 200 and r.success) or (r.http_status == 409 and not r.success))
        ]

        if args.strict_webhook_dup_200:
            _require(
                len(success_200) == len(webhook_results),
                f"strict webhook duplicate check failed, statuses={[ (r.http_status, r.code) for r in webhook_results ]}",
            )
        else:
            _require(len(success_200) >= 1, f"webhook concurrent duplicate should have at least one success 200, got={[ (r.http_status, r.code) for r in webhook_results ]}")
            _require(len(others) == 0, f"webhook concurrent duplicate has unexpected responses: {[ (r.http_status, r.code, r.body.get('message')) for r in others ]}")
            print(f"  webhook concurrent results: 200={len(success_200)}, 409={len(conflict_409)}")

        replay = admin.webhook_17track(sign, raw_body)
        _require(replay.http_status == 200 and replay.success, f"webhook sequential duplicate should be 200 success, got {replay.http_status}, body={_pretty(replay.body)}")

        webhook_source_ref = "17track:" + _sha256_hex(raw_body)
        webhook_log_count = _admin_log_count(admin, seed_shipment_id, webhook_source_ref)
        _require(webhook_log_count == 1, f"webhook source_ref should have exactly one log, got {webhook_log_count}")

        detail_after_webhook = admin.admin_shipment_detail(seed_shipment_id)
        _require(detail_after_webhook.http_status == 200 and detail_after_webhook.success, f"detail after webhook failed: {detail_after_webhook.http_status}, body={_pretty(detail_after_webhook.body)}")
        status_after_webhook = _extract_status(detail_after_webhook.data)
        _require(status_after_webhook == "PICKED_UP", f"status after webhook should be PICKED_UP, got {status_after_webhook}")

    print("[7/7] done")
    print("DONE.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as err:
        print(f"HTTP error: {err}", file=sys.stderr)
        raise

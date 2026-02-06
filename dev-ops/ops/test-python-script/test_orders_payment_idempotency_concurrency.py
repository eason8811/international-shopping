import argparse
import json
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Tuple

import requests


def _now_ms() -> int:
    return int(time.time() * 1000)


def _pretty(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)


def _require(cond: bool, msg: str) -> None:
    if not cond:
        raise AssertionError(msg)


def _require_or_warn(cond: bool, msg: str, *, strict: bool) -> None:
    if cond:
        return
    if strict:
        raise AssertionError(msg)
    print(f"[WARN] {msg}")


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
                "User-Agent": "international-shopping-idem-concurrency-test/1.0",
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
    ) -> ApiResult:
        method_u = method.upper()
        if method_u in ("POST", "PUT", "PATCH", "DELETE") and not path.startswith("/auth/"):
            self.ensure_csrf_for(method_u, path)
        h: Dict[str, str] = {}
        if self.csrf_token:
            h["X-CSRF-Token"] = self.csrf_token
        if headers:
            h.update(headers)
        start = _now_ms()
        resp = self.session.request(
            method=method,
            url=self._url(path),
            params=params,
            json=json_body,
            headers=h,
            timeout=self.timeout_s,
        )
        elapsed = _now_ms() - start
        try:
            body = resp.json() if resp.content else {}
        except Exception:
            body = {"raw": resp.text}
        return ApiResult(resp.status_code, body, dict(resp.headers), elapsed)

    def _cookie_values(self, name: str) -> list[str]:
        vals: list[str] = []
        for c in self.session.cookies:
            if c is not None and c.name == name and c.value is not None:
                vals.append(str(c.value))
        return vals

    def _cookie_header_for(self, method: str, path: str) -> str:
        req = requests.Request(method=method, url=self._url(path))
        prep = self.session.prepare_request(req)
        return str(prep.headers.get("Cookie") or "")

    def login(self, account: str, password: str) -> None:
        r = self.request("POST", "/auth/login", json_body={"account": account, "password": password})
        _require(r.http_status == 200 and r.success, f"login failed: {r.http_status}, body={_pretty(r.body)}")
        _require(
            len(self._cookie_values("access_token")) >= 1,
            f"login succeeded but access_token cookie missing, set-cookie={r.headers.get('Set-Cookie')}",
        )

    def issue_csrf(self) -> None:
        r = self.request("GET", "/auth/csrf")
        _require(r.http_status == 200 and r.success, f"csrf failed: {r.http_status}, body={_pretty(r.body)}")
        token = (r.data or {}).get("csrf_token") if isinstance(r.data, dict) else None
        _require(bool(token), f"csrf token missing, body={_pretty(r.body)}")
        self.csrf_token = str(token)
        vals = self._cookie_values("csrf_token")
        _require(len(vals) >= 1, f"csrf_token cookie missing, set-cookie={r.headers.get('Set-Cookie')}")
        _require(
            self.csrf_token in vals,
            f"csrf_token cookie mismatch, cookie_vals={vals}, header_token={self.csrf_token}, set-cookie={r.headers.get('Set-Cookie')}",
        )

    def ensure_csrf_for(self, method: str, path: str) -> None:
        if not self.csrf_token:
            self.issue_csrf()
        cookie_header = self._cookie_header_for(method, path)
        if "csrf_token=" not in cookie_header:
            self.issue_csrf()
            cookie_header = self._cookie_header_for(method, path)
        _require(
            "csrf_token=" in cookie_header,
            (
                "csrf_token cookie not attached to request. "
                f"url={self._url(path)}, cookie_header={cookie_header!r}, "
                f"cookie_domains={sorted({c.domain for c in self.session.cookies})}"
            ),
        )


def _extract_order_no(created: ApiResult) -> str:
    _require(isinstance(created.data, dict), f"create.data not object: {created.data}")
    order_no = str(created.data.get("order_no") or "")
    _require(bool(order_no), f"order_no missing: {created.data}")
    return order_no


def _extract_checkout(checkout: ApiResult) -> int:
    _require(isinstance(checkout.data, dict), f"checkout.data not object: {checkout.data}")
    payment_id = checkout.data.get("payment_id")
    _require(isinstance(payment_id, int), f"payment_id missing: {checkout.data}")
    return payment_id


def _extract_approve_url(checkout: ApiResult) -> str:
    _require(isinstance(checkout.data, dict), f"checkout.data not object: {checkout.data}")
    approve_url = str(checkout.data.get("approve_url") or "")
    _require(bool(approve_url), f"approve_url missing: {checkout.data}")
    return approve_url


def _thread_pool(n: int) -> ThreadPoolExecutor:
    return ThreadPoolExecutor(max_workers=max(1, n))


def main() -> int:
    parser = argparse.ArgumentParser(description="Idempotency + concurrency tests for orders + payment domains.")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1", help="API base URL (default: %(default)s)")

    parser.add_argument("--user-account", required=True)
    parser.add_argument("--user-password", required=True)
    parser.add_argument("--admin-account", required=True)
    parser.add_argument("--admin-password", required=True)

    parser.add_argument("--sku-id", type=int, default=1)
    parser.add_argument("--quantity", type=int, default=1)
    parser.add_argument("--currency", default="USD")
    parser.add_argument("--address-id", type=int, default=1)
    parser.add_argument("--locale", default="en-US")
    parser.add_argument("--source", default="DIRECT")

    parser.add_argument("--return-url", default="https://shopping.example.com/pay/return")
    parser.add_argument("--cancel-url", default="https://shopping.example.com/pay/cancel")

    parser.add_argument("--checkout-concurrency", type=int, default=10, help="Concurrent checkout requests for same order")
    parser.add_argument("--create-concurrency", type=int, default=10, help="Concurrent create-order requests with same Idempotency-Key")
    parser.add_argument("--cancel-capture-race", action="store_true", help="Race cancel vs capture (requires manual approval)")

    parser.add_argument("--refund-concurrency", action="store_true", help="Concurrent refund-request + admin confirm refund (requires manual PayPal approval)")
    parser.add_argument("--refund-request-concurrency", type=int, default=10, help="Concurrent refund-request requests for same order")
    parser.add_argument("--confirm-refund-concurrency", type=int, default=10, help="Concurrent admin confirm-refund requests for same order")
    parser.add_argument("--refund-wait-seconds", type=int, default=120, help="Wait seconds for refund to become SUCCESS/REFUNDED (default: %(default)s)")
    parser.add_argument("--wait-webhook-seconds", type=int, default=0, help="Wait seconds for PayPal webhooks to be recorded (0=skip; best-effort unless --strict-webhook)")
    parser.add_argument("--strict-webhook", action="store_true", help="Fail if webhook evidence not observed within --wait-webhook-seconds window")
    args = parser.parse_args()

    user = ApiClient(args.base_url)
    admin = ApiClient(args.base_url)

    print("[1/6] login user/admin + csrf")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    print("[2/6] create-order idempotency (same Idempotency-Key, concurrent)")
    idem_key = str(uuid.uuid4())

    def _create_once() -> ApiResult:
        payload = {
            "source": args.source,
            "address_id": args.address_id,
            "currency": args.currency,
            "items": [{"sku_id": args.sku_id, "quantity": args.quantity}],
            "discount_code": None,
            "buyer_remark": f"idem-create-{idem_key}",
            "locale": args.locale,
        }
        return user.request("POST", "/users/me/orders", json_body=payload, headers={"Idempotency-Key": idem_key})

    create_results: List[ApiResult] = []
    with _thread_pool(args.create_concurrency) as ex:
        futs = [ex.submit(_create_once) for _ in range(args.create_concurrency)]
        for fut in as_completed(futs):
            create_results.append(fut.result())

    ok_responses = [r for r in create_results if r.http_status in (200, 201) and r.success and isinstance(r.data, dict) and r.data.get("order_no")]
    _require(len(ok_responses) >= 1, f"no successful create responses: {[r.http_status for r in create_results]}")
    order_nos = sorted({str(r.data.get("order_no")) for r in ok_responses})
    _require(len(order_nos) == 1, f"create idempotency violated, got order_nos={order_nos}")
    order_no = order_nos[0]
    print(f"  ok, order_no={order_no}")

    print("[3/6] payment checkout concurrency (same order, different Idempotency-Key)")

    def _checkout_once() -> ApiResult:
        return user.request(
            "POST",
            "/payments/paypal/checkout",
            json_body={"order_no": order_no, "channel": "PAYPAL", "return_url": args.return_url, "cancel_url": args.cancel_url},
            headers={"Idempotency-Key": str(uuid.uuid4())},
        )

    checkout_results: List[ApiResult] = []
    with _thread_pool(args.checkout_concurrency) as ex:
        futs = [ex.submit(_checkout_once) for _ in range(args.checkout_concurrency)]
        for fut in as_completed(futs):
            checkout_results.append(fut.result())

    ok_checkout = [r for r in checkout_results if r.http_status in (200, 201) and r.success]
    _require(len(ok_checkout) >= 1, f"no successful checkout: {[_pretty(r.body) for r in checkout_results[:3]]}")
    payment_ids = sorted({int(r.data.get("payment_id")) for r in ok_checkout if isinstance(r.data, dict) and isinstance(r.data.get("payment_id"), int)})
    _require(len(payment_ids) == 1, f"checkout should reuse single payment_id, got {payment_ids}")
    payment_id = payment_ids[0]
    print(f"  ok, payment_id={payment_id}")

    print("[4/6] payment cancel idempotency (repeat cancel)")
    cancel1 = user.request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})
    _require(cancel1.http_status == 200, f"cancel1 http_status={cancel1.http_status}, body={_pretty(cancel1.body)}")
    _require(cancel1.success, f"cancel1 success=false, body={_pretty(cancel1.body)}")
    cancel2 = user.request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})
    _require(cancel2.http_status == 200, f"cancel2 http_status={cancel2.http_status}, body={_pretty(cancel2.body)}")
    _require(cancel2.success, f"cancel2 success=false, body={_pretty(cancel2.body)}")

    print("[5/6] cancel vs capture race (optional)")
    if args.cancel_capture_race:
        # Re-checkout to get an active attempt to race.
        checkout = _checkout_once()
        _require(checkout.http_status in (200, 201) and checkout.success, f"re-checkout failed: {checkout.http_status}, body={_pretty(checkout.body)}")
        payment_id_race = _extract_checkout(checkout)
        approve_url = _extract_approve_url(checkout)
        print(f"  approve_url={approve_url}")
        print("  Please open approve_url and complete payment in browser, then press ENTER to start race.")
        try:
            input()
        except KeyboardInterrupt:
            return 130

        def _capture_once() -> ApiResult:
            return user.request("POST", f"/payments/paypal/{payment_id_race}/capture", json_body={"payer_id": None, "note": "race capture"}, headers={"Idempotency-Key": str(uuid.uuid4())})

        def _cancel_once() -> ApiResult:
            return user.request("POST", f"/payments/paypal/{payment_id_race}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})

        with _thread_pool(2) as ex:
            fut_capture = ex.submit(_capture_once)
            fut_cancel = ex.submit(_cancel_once)
            r_capture = fut_capture.result()
            r_cancel = fut_cancel.result()

        _require(r_capture.http_status == 200 and r_capture.success, f"race capture failed: {r_capture.http_status}, body={_pretty(r_capture.body)}")
        _require(r_cancel.http_status == 200 and r_cancel.success, f"race cancel failed: {r_cancel.http_status}, body={_pretty(r_cancel.body)}")
        print(f"  race capture.code={r_capture.code}, status={(r_capture.data or {}).get('status') if isinstance(r_capture.data, dict) else None}")
        print(f"  race cancel.code={r_cancel.code}, status={(r_cancel.data or {}).get('status') if isinstance(r_cancel.data, dict) else None}")
    else:
        print("  skipped (enable --cancel-capture-race if you will manually approve PayPal)")

    print("[6/6] refund-request + confirm-refund concurrency (optional)")
    if not args.refund_concurrency:
        print("  skipped (enable --refund-concurrency if you will manually approve PayPal)")
        print("DONE.")
        return 0

    # 1) Create a fresh order to avoid interference with previous cancel tests.
    refund_order_idem_key = str(uuid.uuid4())
    create_refund_order = user.request(
        "POST",
        "/users/me/orders",
        json_body={
            "source": args.source,
            "address_id": args.address_id,
            "currency": args.currency,
            "items": [{"sku_id": args.sku_id, "quantity": args.quantity}],
            "discount_code": None,
            "buyer_remark": f"refund-concurrency-create-{refund_order_idem_key}",
            "locale": args.locale,
        },
        headers={"Idempotency-Key": refund_order_idem_key},
    )
    _require(create_refund_order.http_status in (200, 201) and create_refund_order.success, f"refund test create order failed: {create_refund_order.http_status}, body={_pretty(create_refund_order.body)}")
    refund_order_no = _extract_order_no(create_refund_order)
    print(f"  refund_test.order_no={refund_order_no}")

    checkout_refund = user.request(
        "POST",
        "/payments/paypal/checkout",
        json_body={"order_no": refund_order_no, "channel": "PAYPAL", "return_url": args.return_url, "cancel_url": args.cancel_url},
        headers={"Idempotency-Key": str(uuid.uuid4())},
    )
    _require(checkout_refund.http_status in (200, 201) and checkout_refund.success, f"refund test checkout failed: {checkout_refund.http_status}, body={_pretty(checkout_refund.body)}")
    refund_payment_id = _extract_checkout(checkout_refund)
    approve_url = _extract_approve_url(checkout_refund)
    print(f"  refund_test.payment_id={refund_payment_id}")
    print("\n=== PayPal approval required (refund concurrency tests) ===")
    print("Open approve_url in browser and complete payment, then press ENTER to continue.")
    print(f"approve_url: {approve_url}\n")
    try:
        input()
    except KeyboardInterrupt:
        return 130

    captured = user.request(
        "POST",
        f"/payments/paypal/{refund_payment_id}/capture",
        json_body={"payer_id": None, "note": "refund concurrency capture"},
        headers={"Idempotency-Key": str(uuid.uuid4())},
    )
    _require(captured.http_status == 200 and captured.success, f"refund test capture failed: {captured.http_status}, body={_pretty(captured.body)}")

    # 2) Concurrent refund-request (USER)
    def _refund_request_once() -> ApiResult:
        return user.request(
            "POST",
            f"/users/me/orders/{refund_order_no}/refund-request",
            json_body={"reason_code": "OTHER", "reason_text": "concurrency refund request", "attachments": []},
        )

    refund_req_results: List[ApiResult] = []
    with _thread_pool(args.refund_request_concurrency) as ex:
        futs = [ex.submit(_refund_request_once) for _ in range(args.refund_request_concurrency)]
        for fut in as_completed(futs):
            refund_req_results.append(fut.result())

    ok_refund_req = [r for r in refund_req_results if r.http_status in (200, 202) and r.success]
    _require(len(ok_refund_req) >= 1, f"no successful refund-request responses: {[r.http_status for r in refund_req_results]}")

    # 3) Concurrent admin confirm refund (ADMIN) - should restock only once.
    def _confirm_refund_once() -> ApiResult:
        return admin.request(
            "POST",
            f"/admin/orders/{refund_order_no}/refund/confirm",
            json_body={"note": "concurrency confirm refund"},
        )

    confirm_results: List[ApiResult] = []
    with _thread_pool(args.confirm_refund_concurrency) as ex:
        futs = [ex.submit(_confirm_refund_once) for _ in range(args.confirm_refund_concurrency)]
        for fut in as_completed(futs):
            confirm_results.append(fut.result())

    ok_confirm = [r for r in confirm_results if r.http_status == 200 and r.success]
    _require(len(ok_confirm) >= 1, f"no successful confirm-refund responses: {[r.http_status for r in confirm_results]}")

    # 4) Verify: status logs/inventory logs/refund list should indicate single restock & single success refund.
    def _admin_status_logs() -> ApiResult:
        return admin.request("GET", f"/admin/orders/{refund_order_no}/status-logs")

    def _admin_inventory_logs() -> ApiResult:
        return admin.request("GET", f"/admin/orders/{refund_order_no}/inventory-logs", params={"page": 1, "size": 200})

    def _admin_refunds_list() -> ApiResult:
        return admin.request("GET", "/admin/refunds", params={"order_no": refund_order_no, "page": 1, "size": 200})

    deadline = time.time() + max(1, args.refund_wait_seconds)
    restock_count = 0
    refunded_transition_count = 0
    refunds_len = 0
    while time.time() < deadline:
        inv = _admin_inventory_logs()
        if inv.http_status == 200 and inv.success and isinstance(inv.data, list):
            restock_count = sum(1 for x in inv.data if isinstance(x, dict) and str(x.get("change_type") or "").upper() == "RESTOCK")

        logs = _admin_status_logs()
        if logs.http_status == 200 and logs.success and isinstance(logs.data, list):
            refunded_transition_count = sum(
                1
                for x in logs.data
                if isinstance(x, dict)
                and str(x.get("from_status") or "").upper() == "REFUNDING"
                and str(x.get("to_status") or "").upper() == "REFUNDED"
            )

        refunds = _admin_refunds_list()
        if refunds.http_status == 200 and refunds.success and isinstance(refunds.data, list):
            refunds_len = len(refunds.data)

        if restock_count >= 1 or refunded_transition_count >= 1:
            break
        time.sleep(1.0)

    _require(restock_count == 1, f"RESTOCK should happen once, got restock_count={restock_count}")
    _require(refunded_transition_count == 1, f"REFUNDING->REFUNDED should happen once, got count={refunded_transition_count}")
    _require(refunds_len == 1, f"payment refunds for order should be exactly 1, got refunds_len={refunds_len}")

    # 5) Optional: run ops sync after refunded to simulate late capture events and ensure no status pollution.
    sync = admin.request("POST", f"/admin/payments/{refund_payment_id}/sync", json_body={})
    _require(sync.http_status in (200, 202) and sync.success, f"payment sync after refund failed: {sync.http_status}, body={_pretty(sync.body)}")
    time.sleep(3.0)
    admin_order = admin.request("GET", f"/admin/orders/{refund_order_no}")
    _require(admin_order.http_status == 200 and admin_order.success and isinstance(admin_order.data, dict), f"admin order detail after sync failed: {admin_order.http_status}, body={_pretty(admin_order.body)}")
    _require(str(admin_order.data.get("status") or "").upper() == "REFUNDED", f"order.status should stay REFUNDED after payment sync, got {admin_order.data.get('status')}")
    _require(str(admin_order.data.get("pay_status") or "").upper() == "CLOSED", f"order.pay_status should stay CLOSED after payment sync, got {admin_order.data.get('pay_status')}")

    # Optional webhook evidence checks:
    # - Webhooks are async and can arrive after capture/refund actions.
    # - Best-effort by default; enable --strict-webhook to fail when missing.
    if args.wait_webhook_seconds > 0:
        refunds = _admin_refunds_list()
        _require(refunds.http_status == 200 and refunds.success and isinstance(refunds.data, list) and len(refunds.data) == 1, f"admin refunds list unexpected: {refunds.http_status}, body={_pretty(refunds.body)}")
        refund_id = (refunds.data[0] or {}).get("refund_id") if isinstance(refunds.data[0], dict) else None
        _require(isinstance(refund_id, int), f"refund_id missing in refunds list: {refunds.data[0] if refunds.data else None}")

        def _admin_payment_detail() -> ApiResult:
            return admin.request("GET", f"/admin/payments/{refund_payment_id}")

        def _admin_refund_detail() -> ApiResult:
            return admin.request("GET", f"/admin/refunds/{refund_id}")

        wh_deadline = time.time() + max(1, args.wait_webhook_seconds)
        seen_capture_completed = False
        seen_refunded = False
        while time.time() < wh_deadline and (not seen_capture_completed or not seen_refunded):
            if not seen_capture_completed:
                pd = _admin_payment_detail()
                if pd.http_status == 200 and pd.success and isinstance(pd.data, dict):
                    notify_payload = str(pd.data.get("notify_payload") or "")
                    if "PAYMENT.CAPTURE.COMPLETED" in notify_payload:
                        seen_capture_completed = True

            rd = _admin_refund_detail()
            if rd.http_status == 200 and rd.success and isinstance(rd.data, dict):
                notify_payload = str(rd.data.get("notify_payload") or "")
                if "PAYMENT.CAPTURE.REFUNDED" in notify_payload or "PAYMENT.CAPTURE.REVERSED" in notify_payload:
                    seen_refunded = True
            time.sleep(1.0)

        _require_or_warn(
            seen_capture_completed,
            "did not observe PAYMENT.CAPTURE.COMPLETED webhook recorded in payment.notify_payload within wait window",
            strict=args.strict_webhook,
        )
        _require_or_warn(
            seen_refunded,
            "did not observe PAYMENT.CAPTURE.REFUNDED/REVERSED webhook recorded in refund.notify_payload within wait window",
            strict=args.strict_webhook,
        )

    print("DONE.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as e:
        print(f"HTTP error: {e}", file=sys.stderr)
        raise

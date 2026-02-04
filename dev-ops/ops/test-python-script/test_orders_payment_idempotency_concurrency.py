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

    parser.add_argument("--checkout-concurrency", type=int, default=10, help="Concurrent checkout requests for same order")
    parser.add_argument("--create-concurrency", type=int, default=10, help="Concurrent create-order requests with same Idempotency-Key")
    parser.add_argument("--cancel-capture-race", action="store_true", help="Race cancel vs capture (requires manual approval)")
    args = parser.parse_args()

    user = ApiClient(args.base_url)
    admin = ApiClient(args.base_url)

    print("[1/5] login user/admin + csrf")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()
    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    print("[2/5] create-order idempotency (same Idempotency-Key, concurrent)")
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

    print("[3/5] payment checkout concurrency (same order, different Idempotency-Key)")

    def _checkout_once() -> ApiResult:
        return user.request(
            "POST",
            "/payments/paypal/checkout",
            json_body={"order_no": order_no, "return_url": "https://shopping.example.com/pay/return", "cancel_url": "https://shopping.example.com/pay/cancel"},
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

    print("[4/5] payment cancel idempotency (repeat cancel)")
    cancel1 = user.request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})
    _require(cancel1.http_status == 200, f"cancel1 http_status={cancel1.http_status}, body={_pretty(cancel1.body)}")
    _require(cancel1.success, f"cancel1 success=false, body={_pretty(cancel1.body)}")
    cancel2 = user.request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})
    _require(cancel2.http_status == 200, f"cancel2 http_status={cancel2.http_status}, body={_pretty(cancel2.body)}")
    _require(cancel2.success, f"cancel2 success=false, body={_pretty(cancel2.body)}")

    if not args.cancel_capture_race:
        print("[5/5] skip cancel vs capture race (enable --cancel-capture-race if you will manually approve PayPal)")
        print("DONE.")
        return 0

    print("[5/5] cancel vs capture race (manual approval required)")
    # Re-checkout to get an active attempt to race.
    checkout = _checkout_once()
    _require(checkout.http_status in (200, 201) and checkout.success, f"re-checkout failed: {checkout.http_status}, body={_pretty(checkout.body)}")
    payment_id = _extract_checkout(checkout)
    approve_url = str((checkout.data or {}).get("approve_url") or "")
    print(f"  approve_url={approve_url}")
    print("  Please open approve_url and complete payment in browser, then press ENTER to start race.")
    try:
        input()
    except KeyboardInterrupt:
        return 130

    def _capture_once() -> ApiResult:
        return user.request("POST", f"/payments/paypal/{payment_id}/capture", json_body={"payer_id": None, "note": "race capture"}, headers={"Idempotency-Key": str(uuid.uuid4())})

    def _cancel_once() -> ApiResult:
        return user.request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers={"Idempotency-Key": str(uuid.uuid4())})

    with _thread_pool(2) as ex:
        fut_capture = ex.submit(_capture_once)
        fut_cancel = ex.submit(_cancel_once)
        r_capture = fut_capture.result()
        r_cancel = fut_cancel.result()

    _require(r_capture.http_status == 200 and r_capture.success, f"race capture failed: {r_capture.http_status}, body={_pretty(r_capture.body)}")
    _require(r_cancel.http_status == 200 and r_cancel.success, f"race cancel failed: {r_cancel.http_status}, body={_pretty(r_cancel.body)}")
    print(f"  race capture.code={r_capture.code}, status={(r_capture.data or {}).get('status') if isinstance(r_capture.data, dict) else None}")
    print(f"  race cancel.code={r_cancel.code}, status={(r_cancel.data or {}).get('status') if isinstance(r_cancel.data, dict) else None}")
    print("DONE.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as e:
        print(f"HTTP error: {e}", file=sys.stderr)
        raise

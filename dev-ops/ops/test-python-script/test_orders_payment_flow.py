import argparse
import json
import sys
import time
import uuid
from dataclasses import dataclass
from typing import Any, Dict, Optional, Tuple
import requests


def _now_ms() -> int:
    return int(time.time() * 1000)


def _require(cond: bool, msg: str) -> None:
    if not cond:
        raise AssertionError(msg)


def _pretty(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)


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
                "User-Agent": "international-shopping-e2e-test/1.0",
            }
        )
        self.csrf_token: Optional[str] = None

    def _url(self, path: str) -> str:
        if not path.startswith("/"):
            path = "/" + path
        return self.base_url + path

    def _request(
        self,
        method: str,
        path: str,
        *,
        params: Optional[Dict[str, Any]] = None,
        json_body: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        allow_non_json: bool = False,
    ) -> ApiResult:
        method_u = method.upper()
        if method_u in ("POST", "PUT", "PATCH", "DELETE") and not path.startswith("/auth/"):
            self.ensure_csrf_for(method_u, path)
        h = {}
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

        body: Dict[str, Any]
        if resp.content is None or resp.content == b"":
            body = {}
        else:
            try:
                body = resp.json()
            except Exception:
                if allow_non_json:
                    body = {"raw": resp.text}
                else:
                    raise
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

    def login(self, account: str, password: str) -> ApiResult:
        r = self._request(
            "POST",
            "/auth/login",
            json_body={"account": account, "password": password},
        )
        _require(r.http_status == 200, f"login http_status={r.http_status}, body={_pretty(r.body)}")
        _require(r.success, f"login success=false, body={_pretty(r.body)}")
        _require(
            len(self._cookie_values("access_token")) >= 1,
            f"login succeeded but access_token cookie missing, set-cookie={r.headers.get('Set-Cookie')}",
        )
        return r

    def issue_csrf(self) -> ApiResult:
        r = self._request("GET", "/auth/csrf")
        _require(r.http_status == 200, f"csrf http_status={r.http_status}, body={_pretty(r.body)}")
        _require(r.success, f"csrf success=false, body={_pretty(r.body)}")
        token = (r.data or {}).get("csrf_token") if isinstance(r.data, dict) else None
        _require(bool(token), f"csrf token missing, body={_pretty(r.body)}")
        self.csrf_token = str(token)
        # X-CSRF-Token 使用 body.csrfToken；csrf_token Cookie 交由 requests 自动管理（来自服务端 Set-Cookie）
        vals = self._cookie_values("csrf_token")
        _require(len(vals) >= 1, f"csrf_token cookie missing, set-cookie={r.headers.get('Set-Cookie')}")
        _require(
            self.csrf_token in vals,
            f"csrf_token cookie mismatch, cookie_vals={vals}, header_token={self.csrf_token}, set-cookie={r.headers.get('Set-Cookie')}",
        )
        return r

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

    def cart_upsert(self, sku_id: int, quantity: int, selected: bool = True) -> ApiResult:
        return self._request(
            "POST",
            "/users/me/cart/items",
            json_body={"sku_id": sku_id, "quantity": quantity, "selected": selected},
        )

    def order_preview(
        self,
        *,
        source: str,
        address_id: int,
        currency: str,
        sku_id: int,
        quantity: int,
        locale: str,
        discount_code: Optional[str] = None,
        buyer_remark: Optional[str] = None,
    ) -> ApiResult:
        payload: Dict[str, Any] = {
            "source": source,
            "address_id": address_id,
            "currency": currency,
            "items": [{"sku_id": sku_id, "quantity": quantity}],
            "discount_code": discount_code,
            "buyer_remark": buyer_remark,
            "locale": locale,
        }
        return self._request("POST", "/users/me/orders/preview", json_body=payload)

    def order_create(
        self,
        *,
        source: str,
        address_id: int,
        currency: str,
        sku_id: int,
        quantity: int,
        locale: str,
        discount_code: Optional[str] = None,
        buyer_remark: Optional[str] = None,
        idempotency_key: Optional[str] = None,
    ) -> ApiResult:
        payload: Dict[str, Any] = {
            "source": source,
            "address_id": address_id,
            "currency": currency,
            "items": [{"sku_id": sku_id, "quantity": quantity}],
            "discount_code": discount_code,
            "buyer_remark": buyer_remark,
            "locale": locale,
        }
        headers: Dict[str, str] = {}
        if idempotency_key:
            headers["Idempotency-Key"] = idempotency_key
        return self._request("POST", "/users/me/orders", json_body=payload, headers=headers)

    def order_detail(self, order_no: str) -> ApiResult:
        return self._request("GET", f"/users/me/orders/{order_no}")

    def order_cancel(self, order_no: str, reason: str) -> ApiResult:
        return self._request("POST", f"/users/me/orders/{order_no}/cancel", json_body={"reason": reason})

    def order_refund_request(self, order_no: str, reason_code: str, reason_text: str) -> ApiResult:
        return self._request(
            "POST",
            f"/users/me/orders/{order_no}/refund-request",
            json_body={"reason_code": reason_code, "reason_text": reason_text, "attachments": None},
        )

    def admin_order_detail(self, order_no: str) -> ApiResult:
        return self._request("GET", f"/admin/orders/{order_no}")

    def admin_confirm_refund(self, order_no: str, note: Optional[str] = None) -> ApiResult:
        return self._request("POST", f"/admin/orders/{order_no}/refund/confirm", json_body={"note": note})

    def admin_inventory_logs(self, order_no: str, page: int = 1, size: int = 50) -> ApiResult:
        return self._request("GET", f"/admin/orders/{order_no}/inventory-logs", params={"page": page, "size": size})

    def paypal_checkout(self, order_no: str, return_url: str, cancel_url: str, idempotency_key: str) -> ApiResult:
        return self._request(
            "POST",
            "/payments/paypal/checkout",
            json_body={"order_no": order_no, "channel": "PAYPAL", "return_url": return_url, "cancel_url": cancel_url},
            headers={"Idempotency-Key": idempotency_key},
        )

    def paypal_capture(self, payment_id: int, note: Optional[str] = None, idempotency_key: Optional[str] = None) -> ApiResult:
        headers: Dict[str, str] = {}
        if idempotency_key:
            headers["Idempotency-Key"] = idempotency_key
        payload: Dict[str, Any] = {"payer_id": None, "note": note}
        return self._request("POST", f"/payments/paypal/{payment_id}/capture", json_body=payload, headers=headers)

    def paypal_cancel(self, payment_id: int, idempotency_key: Optional[str] = None) -> ApiResult:
        headers: Dict[str, str] = {}
        if idempotency_key:
            headers["Idempotency-Key"] = idempotency_key
        return self._request("POST", f"/payments/paypal/{payment_id}/cancel", json_body={}, headers=headers)

    def admin_payment_sync(self, payment_id: int) -> ApiResult:
        return self._request("POST", f"/admin/payments/{payment_id}/sync", json_body={})

    def admin_payments_page(self, *, order_no: Optional[str] = None, page: int = 1, size: int = 20) -> ApiResult:
        params: Dict[str, Any] = {"page": page, "size": size}
        if order_no:
            params["order_no"] = order_no
        return self._request("GET", "/admin/payments", params=params)

    def admin_refunds_page(self, *, order_no: Optional[str] = None, page: int = 1, size: int = 20) -> ApiResult:
        params: Dict[str, Any] = {"page": page, "size": size}
        if order_no:
            params["order_no"] = order_no
        return self._request("GET", "/admin/refunds", params=params)


def _extract_order_no(create_result: ApiResult) -> str:
    _require(isinstance(create_result.data, dict), f"create.data not object: {create_result.data}")
    order_no = str(create_result.data.get("order_no") or "")
    _require(bool(order_no), f"order_no missing: {create_result.data}")
    return order_no


def _extract_checkout(checkout_result: ApiResult) -> Tuple[int, str, str]:
    _require(isinstance(checkout_result.data, dict), f"checkout.data not object: {checkout_result.data}")
    payment_id = checkout_result.data.get("payment_id")
    approve_url = checkout_result.data.get("approve_url")
    paypal_order_id = checkout_result.data.get("paypal_order_id")
    _require(isinstance(payment_id, int), f"payment_id missing: {checkout_result.data}")
    _require(bool(approve_url), f"approve_url missing: {checkout_result.data}")
    _require(bool(paypal_order_id), f"paypal_order_id missing: {checkout_result.data}")
    return payment_id, str(paypal_order_id), str(approve_url)


def _wait_for_user_approval(approve_url: str) -> None:
    print("\n=== PayPal approval required ===")
    print("Open approve_url in browser and complete payment, then press ENTER to continue.")
    print(f"approve_url: {approve_url}\n")
    try:
        input()
    except KeyboardInterrupt:
        raise SystemExit(130)


def main() -> int:
    parser = argparse.ArgumentParser(description="E2E business flow test for orders + payment domains.")
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

    parser.add_argument("--manual-approve", action="store_true", help="Wait for manual PayPal approval before capture")
    parser.add_argument("--skip-refund", action="store_true", help="Skip refund part even if payment success")
    args = parser.parse_args()

    user = ApiClient(args.base_url)
    admin = ApiClient(args.base_url)

    print("[1/8] user login")
    user.login(args.user_account, args.user_password)
    user.issue_csrf()

    print("[2/8] cart upsert (optional)")
    cart = user.cart_upsert(args.sku_id, args.quantity, True)
    _require(cart.http_status in (200, 201), f"cart upsert http_status={cart.http_status}, body={_pretty(cart.body)}")
    _require(cart.success, f"cart upsert success=false, body={_pretty(cart.body)}")

    print("[3/8] order preview")
    preview = user.order_preview(
        source=args.source,
        address_id=args.address_id,
        currency=args.currency,
        sku_id=args.sku_id,
        quantity=args.quantity,
        locale=args.locale,
        buyer_remark=f"e2e-preview-{uuid.uuid4()}",
    )
    _require(preview.http_status == 200, f"preview http_status={preview.http_status}, body={_pretty(preview.body)}")
    _require(preview.success, f"preview success=false, body={_pretty(preview.body)}")

    print("[4/8] create order (reserve stock)")
    create_idem = str(uuid.uuid4())
    created = user.order_create(
        source=args.source,
        address_id=args.address_id,
        currency=args.currency,
        sku_id=args.sku_id,
        quantity=args.quantity,
        locale=args.locale,
        buyer_remark=f"e2e-create-{uuid.uuid4()}",
        idempotency_key=create_idem,
    )
    _require(created.http_status in (200, 201), f"create http_status={created.http_status}, body={_pretty(created.body)}")
    _require(created.success, f"create success=false, body={_pretty(created.body)}")
    order_no = _extract_order_no(created)
    print(f"  order_no={order_no}")

    print("[5/8] payment checkout (PayPal)")
    checkout = user.paypal_checkout(order_no, args.return_url, args.cancel_url, idempotency_key=str(uuid.uuid4()))
    _require(checkout.http_status in (200, 201), f"checkout http_status={checkout.http_status}, body={_pretty(checkout.body)}")
    _require(checkout.success, f"checkout success=false, body={_pretty(checkout.body)}")
    payment_id, paypal_order_id, approve_url = _extract_checkout(checkout)
    print(f"  payment_id={payment_id}, paypal_order_id={paypal_order_id}")

    print("[6/8] capture payment")
    if args.manual_approve:
        _wait_for_user_approval(approve_url)
    capture = user.paypal_capture(payment_id, note="e2e capture", idempotency_key=str(uuid.uuid4()))
    _require(capture.http_status == 200, f"capture http_status={capture.http_status}, body={_pretty(capture.body)}")
    _require(capture.success, f"capture success=false, body={_pretty(capture.body)}")
    capture_status = (capture.data or {}).get("status") if isinstance(capture.data, dict) else None
    print(f"  capture.status={capture_status}")

    print("[7/8] verify order & admin views")
    detail = user.order_detail(order_no)
    _require(detail.http_status == 200 and detail.success, f"order detail failed: {detail.http_status}, body={_pretty(detail.body)}")

    admin.login(args.admin_account, args.admin_password)
    admin.issue_csrf()

    admin_detail = admin.admin_order_detail(order_no)
    _require(admin_detail.http_status == 200 and admin_detail.success, f"admin order detail failed: {admin_detail.http_status}, body={_pretty(admin_detail.body)}")

    payments = admin.admin_payments_page(order_no=order_no, page=1, size=20)
    _require(payments.http_status == 200 and payments.success, f"admin payments page failed: {payments.http_status}, body={_pretty(payments.body)}")

    # Ops sync should be Accepted/OK depending on impl
    sync = admin.admin_payment_sync(payment_id)
    _require(sync.http_status in (200, 202), f"admin payment sync failed: {sync.http_status}, body={_pretty(sync.body)}")

    if str(capture_status) != "SUCCESS":
        print("[8/8] payment not SUCCESS -> run cancel attempt and finish")
        cancel = user.paypal_cancel(payment_id, idempotency_key=str(uuid.uuid4()))
        _require(cancel.http_status == 200, f"cancel http_status={cancel.http_status}, body={_pretty(cancel.body)}")
        _require(cancel.success, f"cancel success=false, body={_pretty(cancel.body)}")
        print("DONE (capture not SUCCESS).")
        return 0

    if args.skip_refund:
        print("[8/8] skip refund")
        print("DONE.")
        return 0

    print("[8/8] refund flow: request refund -> admin confirm refund -> verify restock")
    refund_req = user.order_refund_request(order_no, reason_code="OTHER", reason_text="e2e refund request")
    _require(refund_req.http_status == 202 and refund_req.success, f"refund-request failed: {refund_req.http_status}, body={_pretty(refund_req.body)}")

    confirm = admin.admin_confirm_refund(order_no, note="e2e confirm refund")
    _require(confirm.http_status == 200 and confirm.success, f"admin confirm refund failed: {confirm.http_status}, body={_pretty(confirm.body)}")

    inv = admin.admin_inventory_logs(order_no, page=1, size=50)
    _require(inv.http_status == 200 and inv.success, f"inventory logs failed: {inv.http_status}, body={_pretty(inv.body)}")

    refunds = admin.admin_refunds_page(order_no=order_no, page=1, size=20)
    _require(refunds.http_status == 200 and refunds.success, f"admin refunds page failed: {refunds.http_status}, body={_pretty(refunds.body)}")

    print("DONE (refund confirmed).")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as e:
        print(f"HTTP error: {e}", file=sys.stderr)
        raise

import json
import os
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080/api/v1")
TIMEOUT = 10

# Auth headers/cookies as requested
CSRF_TOKEN = "ec562f27-410f-4c67-9272-7905c5607172"
COOKIE = (
    "csrf_token=ec562f27-410f-4c67-9272-7905c5607172; "
    "access_token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidWlkIjoxLCJhdWQiOiJ3ZWIiLCJuYmYiOjE3NjY4OTg3MTYsInJvbGVzIjoiVVNFUiIsImlzcyI6InNob3BwaW5nLmludGVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3ODI0NTA3MTYsImlhdCI6MTc2Njg5ODcxNiwiZW1haWwiOiJsZWlsYTk0Njg1NTVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.saKZnZfE9pPS7r3BCzwspIBUXk00-p-CebwCDtrDvHs"
)

DEFAULT_HEADERS = {
    "Content-Type": "application/json",
    "X-CSRF-Token": CSRF_TOKEN,
    "Cookie": COOKIE,
}

ADDRESS_ID = int(os.getenv("ADDRESS_ID", "1"))
SKU_ID = int(os.getenv("SKU_ID", "3"))
QUANTITY = int(os.getenv("QUANTITY", "1"))
CURRENCY = os.getenv("CURRENCY", "USD")
SOURCE = os.getenv("SOURCE", "DIRECT")
LOCALE = os.getenv("LOCALE", "zh-CN")

IDEMPOTENCY_CONCURRENCY = int(os.getenv("IDEMPOTENCY_CONCURRENCY", "8"))
CONCURRENCY_TOTAL = int(os.getenv("CONCURRENCY_TOTAL", "10"))
CONCURRENCY_WORKERS = int(os.getenv("CONCURRENCY_WORKERS", "10"))

EXPECTED = {
    "preview": {"http": {200}, "success": True, "code": {"OK"}},
    "create": {"http": {201}, "success": True, "code": {"CREATED"}},
    "list": {"http": {200}, "success": True, "code": {"OK"}},
    "detail": {"http": {200}, "success": True, "code": {"OK"}},
    "cancel": {"http": {200}, "success": True, "code": {"OK"}},
    "change_address": {"http": {200}, "success": True, "code": {"OK"}},
    "refund": {"http": {202}, "success": True, "code": {"ACCEPTED"}},
    "idempotency": {"unique_order_nos": 1},
    "concurrency": {"duplicates_allowed": 0},
    "race": {"max_success_ops": 1},
}

ALLOWED_FAILURE_CODES = {
    "OUT_OF_STOCK",
    "INSUFFICIENT_STOCK",
    "INVENTORY_SHORTAGE",
}

session = requests.Session()
session.headers.update(DEFAULT_HEADERS)


def request_json(method, path, headers=None, payload=None, params=None):
    url = BASE_URL.rstrip("/") + path
    hdrs = DEFAULT_HEADERS.copy()
    if headers:
        hdrs.update(headers)
    start = time.time()
    resp = session.request(
        method,
        url,
        headers=hdrs,
        json=payload,
        params=params,
        timeout=TIMEOUT,
    )
    cost_ms = round((time.time() - start) * 1000, 2)
    try:
        body = resp.json()
    except Exception:
        body = {"raw": resp.text}
    return {"status": resp.status_code, "cost_ms": cost_ms, "body": body}


def get_data(resp):
    body = resp.get("body", {}) if isinstance(resp, dict) else {}
    return body.get("data")


def get_meta_total(resp):
    body = resp.get("body", {}) if isinstance(resp, dict) else {}
    meta = body.get("meta") or {}
    return meta.get("total")


def summarize(resp):
    body = resp.get("body", {}) if isinstance(resp, dict) else {}
    return {
        "http": resp.get("status"),
        "success": body.get("success"),
        "code": body.get("code"),
        "message": body.get("message"),
    }


def is_success(resp):
    body = resp.get("body", {}) if isinstance(resp, dict) else {}
    return body.get("success") is True


def code_of(resp):
    body = resp.get("body", {}) if isinstance(resp, dict) else {}
    code = body.get("code")
    return code if code is None else str(code)


def check_api(name, resp):
    exp = EXPECTED.get(name, {})
    summ = summarize(resp)
    ok = True
    if "http" in exp and summ["http"] not in exp["http"]:
        ok = False
    if "success" in exp and summ["success"] is not exp["success"]:
        ok = False
    if "code" in exp and summ["code"] not in exp["code"]:
        ok = False
    return ok, summ, exp


def record_check(name, expected, actual, passed, checks):
    checks.append({"name": name, "expected": expected, "actual": actual, "passed": passed})
    status = "PASS" if passed else "FAIL"
    print(f"[{status}] {name} | expected={expected} actual={actual}")


def preview_order():
    payload = {
        "source": SOURCE,
        "items": [{"sku_id": SKU_ID, "quantity": QUANTITY}],
        "address_id": ADDRESS_ID,
        "currency": CURRENCY,
        "discount_code": None,
        "buyer_remark": "preview-test",
        "locale": LOCALE,
    }
    return request_json("POST", "/users/me/orders/preview", payload=payload)


def create_order(idempotency_key=None):
    payload = {
        "source": SOURCE,
        "items": [{"sku_id": SKU_ID, "quantity": QUANTITY}],
        "address_id": ADDRESS_ID,
        "currency": CURRENCY,
        "discount_code": None,
        "buyer_remark": "create-test",
        "locale": LOCALE,
    }
    headers = {}
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key
    return request_json("POST", "/users/me/orders", headers=headers, payload=payload)


def list_orders():
    params = {"page": 1, "size": 40}
    return request_json("GET", "/users/me/orders", params=params)


def order_detail(order_no):
    return request_json("GET", f"/users/me/orders/{order_no}")


def cancel_order(order_no):
    payload = {"reason": "test-cancel"}
    return request_json("POST", f"/users/me/orders/{order_no}/cancel", payload=payload)


def change_address(order_no, address_id):
    payload = {"address_id": address_id, "note": "race-test"}
    return request_json("POST", f"/users/me/orders/{order_no}/change-address", payload=payload)


def refund_request(order_no):
    payload = {
        "reason_code": "OTHER",
        "reason_text": "race-test",
        "attachments": [],
    }
    return request_json("POST", f"/users/me/orders/{order_no}/refund-request", payload=payload)


def idempotency_test(concurrency):
    idem_key = str(uuid.uuid4())
    results = []
    with ThreadPoolExecutor(max_workers=concurrency) as ex:
        futs = [ex.submit(create_order, idem_key) for _ in range(concurrency)]
        for fut in as_completed(futs):
            results.append(fut.result())
    order_nos = []
    for r in results:
        data = get_data(r)
        if isinstance(data, dict) and data.get("orderNo"):
            order_nos.append(data["orderNo"])
    return idem_key, results, order_nos


def concurrency_create_test(total, workers):
    results = []
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futs = [ex.submit(create_order, str(uuid.uuid4())) for _ in range(total)]
        for fut in as_completed(futs):
            results.append(fut.result())
    order_nos = []
    for r in results:
        data = get_data(r)
        if isinstance(data, dict) and data.get("orderNo"):
            order_nos.append(data["orderNo"])
    return results, order_nos


def race_test(order_no, new_address_id):
    results = {}
    with ThreadPoolExecutor(max_workers=3) as ex:
        fut_cancel = ex.submit(cancel_order, order_no)
        fut_refund = ex.submit(refund_request, order_no)
        fut_change = ex.submit(change_address, order_no, new_address_id)
        results["cancel"] = fut_cancel.result()
        results["refund"] = fut_refund.result()
        results["change_address"] = fut_change.result()
    results["detail"] = order_detail(order_no)
    return results


def main():
    checks = []

    before_list = list_orders()
    before_total = get_meta_total(before_list)

    # Preview
    preview = preview_order()
    ok, actual, exp = check_api("preview", preview)
    record_check("preview", exp, actual, ok, checks)

    # Idempotency test
    idem_key, idem_results, idem_order_nos = idempotency_test(IDEMPOTENCY_CONCURRENCY)
    idem_unique = sorted(set(idem_order_nos))
    idem_expected_unique = EXPECTED["idempotency"]["unique_order_nos"]
    idem_ok = len(idem_unique) == idem_expected_unique

    # Check each idempotent response for success unless allowed failure code
    idem_resp_ok = True
    for r in idem_results:
        if is_success(r):
            continue
        if code_of(r) in ALLOWED_FAILURE_CODES:
            continue
        idem_resp_ok = False

    record_check(
        "idempotency.unique_order_nos",
        idem_expected_unique,
        len(idem_unique),
        idem_ok,
        checks,
    )
    record_check(
        "idempotency.all_success_or_allowed_failures",
        True,
        idem_resp_ok,
        idem_resp_ok,
        checks,
    )
    print(f"idempotency_key={idem_key} order_nos={idem_order_nos}")

    # Concurrency create test
    conc_results, conc_order_nos = concurrency_create_test(CONCURRENCY_TOTAL, CONCURRENCY_WORKERS)
    conc_unique = sorted(set(conc_order_nos))
    dup_count = len(conc_order_nos) - len(conc_unique)
    conc_ok = dup_count <= EXPECTED["concurrency"]["duplicates_allowed"]
    record_check(
        "concurrency.duplicate_order_nos",
        EXPECTED["concurrency"]["duplicates_allowed"],
        dup_count,
        conc_ok,
        checks,
    )

    # Check each create response for expected status/code
    conc_resp_ok = True
    for r in conc_results:
        if is_success(r):
            # allow 201 only for create
            if r["status"] != 201:
                conc_resp_ok = False
        else:
            if code_of(r) not in ALLOWED_FAILURE_CODES:
                conc_resp_ok = False
    record_check(
        "concurrency.create_success_or_allowed_failures",
        True,
        conc_resp_ok,
        conc_resp_ok,
        checks,
    )

    # Race test: use a created order
    race_order_no = None
    if conc_unique:
        race_order_no = conc_unique[0]
    elif idem_unique:
        race_order_no = idem_unique[0]

    if race_order_no:
        race = race_test(race_order_no, ADDRESS_ID)
        cancel_ok, cancel_actual, cancel_exp = check_api("cancel", race["cancel"])
        refund_ok, refund_actual, refund_exp = check_api("refund", race["refund"])
        change_ok, change_actual, change_exp = check_api("change_address", race["change_address"])

        record_check("race.cancel", cancel_exp, cancel_actual, cancel_ok, checks)
        record_check("race.refund", refund_exp, refund_actual, refund_ok, checks)
        record_check("race.change_address", change_exp, change_actual, change_ok, checks)

        success_ops = []
        if is_success(race["cancel"]):
            success_ops.append("cancel")
        if is_success(race["refund"]):
            success_ops.append("refund")
        if is_success(race["change_address"]):
            success_ops.append("change_address")

        race_max_ok = len(success_ops) <= EXPECTED["race"]["max_success_ops"]
        record_check(
            "race.max_success_ops",
            EXPECTED["race"]["max_success_ops"],
            len(success_ops),
            race_max_ok,
            checks,
        )

        # Side effect checks against final detail
        detail = race["detail"]
        detail_ok, detail_actual, detail_exp = check_api("detail", detail)
        record_check("race.detail", detail_exp, detail_actual, detail_ok, checks)

        detail_data = get_data(detail) or {}
        final_status = detail_data.get("status")
        address_changed = detail_data.get("addressChanged")

        if "cancel" in success_ops:
            expect_status = {"CANCELLED"}
            record_check("race.status_after_cancel", expect_status, final_status, final_status in expect_status, checks)
        if "refund" in success_ops:
            expect_status = {"REFUNDING", "REFUNDED"}
            record_check("race.status_after_refund", expect_status, final_status, final_status in expect_status, checks)
        if "change_address" in success_ops:
            record_check("race.address_changed", True, address_changed, address_changed is True, checks)
    else:
        print("[WARN] No orderNo available for race test; create responses may have failed.")

    after_list = list_orders()
    after_total = get_meta_total(after_list)

    # Side effect: total order count should be >= created unique orders
    created_unique = len(set(idem_order_nos + conc_order_nos))
    if before_total is not None and after_total is not None:
        delta = after_total - before_total
        record_check(
            "side_effect.order_total_increase",
            f">= {created_unique}",
            delta,
            delta >= created_unique,
            checks,
        )
    else:
        print("[WARN] list meta total missing; skip total side-effect check.")

    # Summary
    passed = sum(1 for c in checks if c["passed"])
    print("\n=== SUMMARY ===")
    print(f"checks_passed={passed}/{len(checks)}")
    print(json.dumps(checks, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()

import json
from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Dict, List, Tuple

import requests

BASE_URL = "http://127.0.0.1:8080"
ORDER_URL = f"{BASE_URL}/api/v1/users/me/orders"

# ====== 你需要填完整值（你消息里用 *** 打码了）======
CSRF_TOKEN = "ec562f27-410f-***-9272-****"
ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzd****GVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3ODI0NTA3MTYsImlhdCI6MT****TVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.saKZnZfE9p****rDvHs"

# 货币小数位（本次用到的）
MINOR_UNIT = {
    "USD": 2,
    "CNY": 2,
    "SGD": 2,
}

def q(amount: str, currency: str) -> str:
    """按币种小数位规范化为字符串，便于对比"""
    if amount is None:
        return None
    d = Decimal(str(amount))
    mu = MINOR_UNIT.get(currency, 2)
    quant = Decimal("1") if mu == 0 else Decimal("1").scaleb(-mu)  # 10^-mu
    return str(d.quantize(quant, rounding=ROUND_HALF_UP))

def diff_kv(path: str, expected: Any, actual: Any, diffs: List[str]):
    if expected != actual:
        diffs.append(f"{path}: expected={expected} actual={actual}")

def index_items_by_sku(items: List[Dict[str, Any]]) -> Dict[int, Dict[str, Any]]:
    m = {}
    for it in items or []:
        sku = it.get("sku_id")
        if sku is not None:
            m[int(sku)] = it
    return m

def compare_response(name: str, expected: Dict[str, Any], resp_json: Dict[str, Any]) -> List[str]:
    diffs: List[str] = []

    # 顶层断言（宽松：CREATED 或 OK）
    if resp_json.get("success") is not True:
        diffs.append(f"$.success: expected=True actual={resp_json.get('success')}")
    if resp_json.get("code") not in ("CREATED", "OK"):
        diffs.append(f"$.code: expected in (CREATED,OK) actual={resp_json.get('code')}")

    data = resp_json.get("data") or {}

    # 订单号/时间存在性
    if not data.get("order_no"):
        diffs.append("$.data.order_no: expected non-empty string")
    if not data.get("created_at"):
        diffs.append("$.data.created_at: expected present")

    # status 合法性（允许 CREATED / PENDING_PAYMENT）
    if data.get("status") not in ("CREATED", "PENDING_PAYMENT"):
        diffs.append(f"$.data.status: expected CREATED or PENDING_PAYMENT actual={data.get('status')}")

    # 关键金额断言
    currency = expected["currency"]
    diff_kv("$.data.currency", currency, data.get("currency"), diffs)

    # items_count
    diff_kv("$.data.items_count", expected["items_count"], data.get("items_count"), diffs)

    # total/discount/pay（有些实现可能把 total/discount/shipping 置 null，这里也会提示差异）
    for k in ("total_amount", "discount_amount", "pay_amount"):
        exp_v = q(expected.get(k), currency)
        act_v = q(data.get(k), currency) if data.get(k) is not None else None
        diff_kv(f"$.data.{k}", exp_v, act_v, diffs)

    # items 对比：按 sku_id 对齐
    exp_items = expected.get("items", [])
    act_items = data.get("items", [])
    exp_map = index_items_by_sku(exp_items)
    act_map = index_items_by_sku(act_items)

    for sku_id, exp_it in exp_map.items():
        act_it = act_map.get(sku_id)
        if not act_it:
            diffs.append(f"$.data.items[sku_id={sku_id}]: missing in actual response")
            continue

        # 必要字段
        for f in ("product_id", "sku_id", "quantity"):
            diff_kv(f"$.data.items[sku_id={sku_id}].{f}", exp_it.get(f), act_it.get(f), diffs)

        # 金额字段（字符串）
        for f in ("unit_price", "subtotal_amount"):
            exp_v = q(exp_it.get(f), currency)
            act_v = q(act_it.get(f), currency) if act_it.get(f) is not None else None
            diff_kv(f"$.data.items[sku_id={sku_id}].{f}", exp_v, act_v, diffs)

        # discount_code_id（只有当 expected 明确给了才强断言）
        if "discount_code_id" in exp_it:
            diff_kv(
                f"$.data.items[sku_id={sku_id}].discount_code_id",
                exp_it.get("discount_code_id"),
                act_it.get("discount_code_id"),
                diffs
            )

    return diffs

def make_session() -> requests.Session:
    s = requests.Session()
    s.headers.update({
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-CSRF-Token": CSRF_TOKEN,
    })
    # 用 cookies 字典比自己拼 Cookie header 更稳
    s.cookies.set("csrf_token", CSRF_TOKEN)
    s.cookies.set("access_token", ACCESS_TOKEN)
    return s

TEST_CASES: List[Dict[str, Any]] = [
    {
        "name": "TC-DIRECT-USD-BASE-001",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 1, "quantity": 1}],
            "discount_code": None,
            "buyer_remark": "TC-DIRECT-USD-BASE-001",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 1,
            "total_amount": "100.00",
            "discount_amount": "0.00",
            "pay_amount": "100.00",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 1, "unit_price": "100.00", "subtotal_amount": "100.00"}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-BJFDDQ-HITCAP-002",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 1, "quantity": 3}],
            "discount_code": "BJFDDQ",
            "buyer_remark": "TC-DIRECT-USD-BJFDDQ-HITCAP-002",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "300.00",
            "discount_amount": "20.00",
            "pay_amount": "280.00",
            "items": [
                # ITEM 折扣：一般会给行挂上 discount_code_id=1（如果你实现不挂，也可以把这行字段删掉）
                {"product_id": 1, "sku_id": 1, "quantity": 3, "unit_price": "100.00", "subtotal_amount": "300.00", "discount_code_id": 1}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-BJFDDQ-MIX-NOMIN-003",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 1, "quantity": 1}, {"sku_id": 4, "quantity": 2}],
            "discount_code": "BJFDDQ",
            "buyer_remark": "TC-DIRECT-USD-BJFDDQ-MIX-NOMIN-003",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "300.00",
            "discount_amount": "0.00",
            "pay_amount": "300.00",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 1, "unit_price": "100.00", "subtotal_amount": "100.00"},
                {"product_id": 2, "sku_id": 4, "quantity": 2, "unit_price": "100.00", "subtotal_amount": "200.00"}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-CPCYWX-ITEMPCT-NOCAP-004",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 1, "quantity": 3}],
            "discount_code": "CPCYWX",
            "buyer_remark": "TC-DIRECT-USD-CPCYWX-ITEMPCT-NOCAP-004",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "300.00",
            "discount_amount": "150.00",
            "pay_amount": "150.00",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 3, "unit_price": "100.00", "subtotal_amount": "300.00", "discount_code_id": 4}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-LBCY6Q-ITEMPCT-CAP-005",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 3, "quantity": 3}],
            "discount_code": "LBCY6Q",
            "buyer_remark": "TC-DIRECT-USD-LBCY6Q-ITEMPCT-CAP-005",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "390.00",
            "discount_amount": "20.00",
            "pay_amount": "370.00",
            "items": [
                {"product_id": 1, "sku_id": 3, "quantity": 3, "unit_price": "130.00", "subtotal_amount": "390.00", "discount_code_id": 3}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-ULN9AZ-ORDERPCT-CAP-006",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 4, "quantity": 3}],
            "discount_code": "ULN9AZ",
            "buyer_remark": "TC-DIRECT-USD-ULN9AZ-ORDERPCT-CAP-006",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "300.00",
            "discount_amount": "100.00",
            "pay_amount": "200.00",
            "items": [
                {"product_id": 2, "sku_id": 4, "quantity": 3, "unit_price": "100.00", "subtotal_amount": "300.00"}
            ]
        }
    },
    {
        "name": "TC-DIRECT-USD-ULN9AZ-MIX-NOMIN-007",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "USD",
            "items": [{"sku_id": 1, "quantity": 1}, {"sku_id": 4, "quantity": 2}],
            "discount_code": "ULN9AZ",
            "buyer_remark": "TC-DIRECT-USD-ULN9AZ-MIX-NOMIN-007",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 3,
            "total_amount": "300.00",
            "discount_amount": "0.00",
            "pay_amount": "300.00",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 1, "unit_price": "100.00", "subtotal_amount": "100.00"},
                {"product_id": 2, "sku_id": 4, "quantity": 2, "unit_price": "100.00", "subtotal_amount": "200.00"}
            ]
        }
    },
    {
        "name": "TC-DIRECT-CNY-SALEPRICE-BASE-008",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "CNY",
            "items": [{"sku_id": 2, "quantity": 1}],
            "discount_code": None,
            "buyer_remark": "TC-DIRECT-CNY-SALEPRICE-BASE-008",
            "locale": "zh-CN"
        },
        "expected": {
            "currency": "CNY",
            "items_count": 1,
            "total_amount": "1000.00",
            "discount_amount": "0.00",
            "pay_amount": "1000.00",
            "items": [
                {"product_id": 1, "sku_id": 2, "quantity": 1, "unit_price": "1000.00", "subtotal_amount": "1000.00"}
            ]
        }
    },
    {
        "name": "TC-DIRECT-CNY-CPCYWX-MINFX-009",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "CNY",
            "items": [{"sku_id": 1, "quantity": 4}],
            "discount_code": "CPCYWX",
            "buyer_remark": "TC-DIRECT-CNY-CPCYWX-MINFX-009",
            "locale": "zh-CN"
        },
        "expected": {
            "currency": "CNY",
            "items_count": 4,
            "total_amount": "2794.24",
            "discount_amount": "1397.12",
            "pay_amount": "1397.12",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 4, "unit_price": "698.56", "subtotal_amount": "2794.24", "discount_code_id": 4}
            ]
        }
    },
    {
        "name": "TC-DIRECT-SGD-LBCY6Q-MINFX-CAP-010",
        "body": {
            "source": "DIRECT",
            "address_id": 1,
            "currency": "SGD",
            "items": [{"sku_id": 4, "quantity": 3}],
            "discount_code": "LBCY6Q",
            "buyer_remark": "TC-DIRECT-SGD-LBCY6Q-MINFX-CAP-010",
            "locale": "en-US"
        },
        "expected": {
            "currency": "SGD",
            "items_count": 3,
            "total_amount": "385.65",
            "discount_amount": "25.71",
            "pay_amount": "359.94",
            "items": [
                {"product_id": 2, "sku_id": 4, "quantity": 3, "unit_price": "128.55", "subtotal_amount": "385.65", "discount_code_id": 3}
            ]
        }
    },
    {
        "name": "TC-CART-USD-BJFDDQ-011",
        "body": {
            "source": "CART",
            "address_id": 1,
            "currency": "USD",
            "discount_code": "BJFDDQ",
            "buyer_remark": "TC-CART-USD-BJFDDQ-011",
            "locale": "en-US"
        },
        "expected": {
            "currency": "USD",
            "items_count": 5,  # sku1×3 + sku4×2
            "total_amount": "500.00",
            "discount_amount": "20.00",
            "pay_amount": "480.00",
            "items": [
                {"product_id": 1, "sku_id": 1, "quantity": 3, "unit_price": "100.00", "subtotal_amount": "300.00", "discount_code_id": 1},
                {"product_id": 2, "sku_id": 4, "quantity": 2, "unit_price": "100.00", "subtotal_amount": "200.00"}
            ]
        }
    },
]

def main():
    s = make_session()

    for tc in TEST_CASES:
        name = tc["name"]
        body = tc["body"]
        expected = tc["expected"]

        print("=" * 90)
        print(f"[RUN] {name}")
        print("[REQUEST]", json.dumps(body, ensure_ascii=False))

        try:
            r = s.post(ORDER_URL, json=body, timeout=20)
        except Exception as e:
            print(f"[ERROR] request failed: {e}")
            continue

        print(f"[HTTP] {r.status_code}")
        try:
            resp_json = r.json()
        except Exception:
            print("[ERROR] response is not JSON:", r.text[:500])
            continue

        diffs = compare_response(name, expected, resp_json)

        # 简洁输出：只展示关键金额与差异
        data = resp_json.get("data") or {}
        print("[ACTUAL] currency=", data.get("currency"),
              " total=", data.get("total_amount"),
              " discount=", data.get("discount_amount"),
              " pay=", data.get("pay_amount"),
              " status=", data.get("status"))

        if not diffs:
            print("[PASS] no diffs")
        else:
            print(f"[FAIL] diffs={len(diffs)}")
            for d in diffs:
                print("  -", d)

    print("=" * 90)
    print("Done.")

if __name__ == "__main__":
    main()

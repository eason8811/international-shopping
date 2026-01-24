package shopping.international.trigger.controller.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.payment.PayPalCaptureRequest;
import shopping.international.api.req.payment.PayPalCheckoutCreateRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.payment.PayPalCheckoutRespond;
import shopping.international.api.resp.payment.PaymentResultRespond;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧支付接口 (PayPal)
 *
 * <p>路径前缀：{@code /payments}</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/payments")
public class PaymentController {

    /**
     * 支付领域服务
     */
    private final IPaymentService paymentService;

    /**
     * 货币配置服务 (用于金额展示换算)
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 创建 PayPal Checkout (返回收银台跳转链接)
     *
     * @param req            请求体
     * @param idempotencyKey 幂等键 (必填)
     * @return Created
     */
    @PostMapping("/paypal/checkout")
    public ResponseEntity<Result<PayPalCheckoutRespond>> checkout(@RequestBody PayPalCheckoutCreateRequest req,
                                                                  @RequestHeader("Idempotency-Key") String idempotencyKey) {
        req.validate();
        Long userId = requireCurrentUserId();
        String normalizedIdempotencyKey = normalizeNotNullField(idempotencyKey, "Idempotency-Key 不能为空",
                s -> s.length() <= 64, "Idempotency-Key 长度不能超过 64 个字符");

        IPaymentService.PayPalCheckoutView view = paymentService.createPayPalCheckout(
                userId,
                req.getOrderNo(),
                req.getReturnUrl(),
                req.getCancelUrl(),
                normalizedIdempotencyKey
        );
        CurrencyConfig currencyConfig = currencyConfigService.get(view.currency());

        PayPalCheckoutRespond resp = PayPalCheckoutRespond.builder()
                .paymentId(view.paymentId())
                .orderNo(view.orderNo())
                .channel(view.channel())
                .amount(currencyConfig.toMajor(view.amountMinor()).toPlainString())
                .currency(view.currency())
                .status(view.status())
                .paypalOrderId(view.paypalOrderId())
                .approveUrl(view.approveUrl())
                .build();

        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus()).body(Result.created(resp));
    }

    /**
     * Capture PayPal 支付 (回跳后确认扣款)
     *
     * @param paymentId      支付单 ID
     * @param req            请求体 (可为空对象)
     * @param idempotencyKey 幂等键 (可选)
     * @return OK
     */
    @PostMapping("/paypal/{payment_id}/capture")
    public ResponseEntity<Result<PaymentResultRespond>> capture(@PathVariable("payment_id") Long paymentId,
                                                                @RequestBody(required = false) PayPalCaptureRequest req,
                                                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long userId = requireCurrentUserId();
        if (req != null)
            req.validate();

        String normalizedIdempotencyKey = normalizeNullableField(idempotencyKey, "Idempotency-Key 不能为空",
                s -> s.length() <= 64, "Idempotency-Key 长度不能超过 64 个字符");

        IPaymentService.PaymentResultView view = paymentService.capturePayPalPayment(
                userId,
                paymentId,
                req == null ? null : req.getPayerId(),
                req == null ? null : req.getNote(),
                normalizedIdempotencyKey
        );

        PaymentResultRespond resp = PaymentResultRespond.builder()
                .paymentId(view.paymentId())
                .status(view.status())
                .externalId(view.externalId())
                .orderNo(view.orderNo())
                .message(view.message())
                .build();
        return ResponseEntity.ok(Result.ok(resp));
    }

    /**
     * 取消本次支付尝试 (关闭支付单，不等于关闭订单)
     *
     * @param paymentId 支付单 ID
     * @return OK
     */
    @PostMapping("/paypal/{payment_id}/cancel")
    public ResponseEntity<Result<PaymentResultRespond>> cancel(@PathVariable("payment_id") Long paymentId) {
        Long userId = requireCurrentUserId();
        IPaymentService.PaymentResultView view = paymentService.cancelPayPalPayment(userId, paymentId);
        PaymentResultRespond resp = PaymentResultRespond.builder()
                .paymentId(view.paymentId())
                .status(view.status())
                .externalId(view.externalId())
                .orderNo(view.orderNo())
                .message(view.message())
                .build();
        return ResponseEntity.ok(Result.ok(resp));
    }

    /**
     * 从安全上下文中解析当前用户 ID
     *
     * @return 当前用户 ID
     * @throws AccountException 未登录或无法解析
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}


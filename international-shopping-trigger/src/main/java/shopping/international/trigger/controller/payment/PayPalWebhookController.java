package shopping.international.trigger.controller.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.utils.FieldValidateUtils;

import java.util.Map;

/**
 * PayPal Webhook 回调入口 (匿名)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/webhooks/paypal")
public class PayPalWebhookController {

    /**
     * 支付领域服务
     */
    private final IPaymentService paymentService;

    /**
     * Webhook 回调入口
     *
     * <p>注意：Webhook 验签依赖 webhook_event 的原始 JSON 内容。</p>
     * <p>因此这里使用 Map 接收请求体，避免将 create_time 等字段反序列化为时间对象后再序列化导致验签失败。</p>
     *
     * @param event   回调事件 (原始结构)
     * @param headers 请求头 (用于验签)
     * @return OK
     */
    @PostMapping
    public ResponseEntity<Result<Void>> webhook(@RequestBody Map<String, Object> event,
                                                @RequestHeader Map<String, String> headers) {
        String eventId = String.valueOf(event.getOrDefault("id", ""));
        FieldValidateUtils.requireNotBlank(eventId, "id 不能为空");
        paymentService.handlePayPalWebhook(event, headers);
        return ResponseEntity.ok(Result.ok(null));
    }
}

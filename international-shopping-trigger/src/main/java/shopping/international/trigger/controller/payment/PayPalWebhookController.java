package shopping.international.trigger.controller.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.payment.PayPalWebhookEventRequest;
import shopping.international.api.resp.Result;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.constant.SecurityConstants;

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
     * JSON 工具 (用于将请求体转为 Map 参与验签与业务处理)
     */
    private final ObjectMapper objectMapper;

    /**
     * Webhook 回调入口
     *
     * @param req     回调事件
     * @param headers 请求头 (用于验签)
     * @return OK
     */
    @PostMapping
    public ResponseEntity<Result<Void>> webhook(@RequestBody PayPalWebhookEventRequest req,
                                                @RequestHeader Map<String, String> headers) {
        req.validate();
        Map<String, Object> event = objectMapper.convertValue(req, new TypeReference<>() {
        });
        paymentService.handlePayPalWebhook(event, headers);
        return ResponseEntity.ok(Result.ok(null));
    }
}


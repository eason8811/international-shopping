package shopping.international.trigger.controller.shipping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.shipping.SeventeenTrackWebhookRequest;
import shopping.international.api.resp.Result;
import shopping.international.domain.service.shipping.IShipmentWebhookService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.LinkedHashMap;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 物流 WebHook 控制器, 提供 17Track 回调入口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/webhooks")
public class ShipmentWebhookController {

    /**
     * 物流 WebHook 领域服务
     */
    private final IShipmentWebhookService shipmentWebhookService;
    /**
     * JSON 工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 接收 17Track WebHook 回调
     *
     * @param sign    签名头
     * @param rawBody 原始请求体
     * @return 处理结果
     */
    @PostMapping("/17track")
    public ResponseEntity<Result<Void>> seventeenTrackWebhook(@RequestHeader("sign") String sign,
                                                              @RequestBody String rawBody) {
        requireNotBlank(sign, "sign 不能为空");
        requireNotBlank(rawBody, "请求体不能为空");

        SeventeenTrackWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, SeventeenTrackWebhookRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalParamException("WebHook 请求体不是合法 JSON", exception);
        }
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", request.getEvent());
        payload.put("data", request.getData());

        shipmentWebhookService.handleSeventeenTrackWebhook(sign, rawBody, payload);
        return ResponseEntity.ok(Result.ok(null));
    }
}

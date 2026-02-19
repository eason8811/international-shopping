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

import java.util.LinkedHashMap;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

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
     * @param request 请求对象
     * @return 处理结果
     */
    @PostMapping("/17track")
    public ResponseEntity<Result<Void>> seventeenTrackWebhook(@RequestHeader("sign") String sign,
                                                              @RequestBody SeventeenTrackWebhookRequest request) {
        requireNotBlank(sign, "sign 不能为空");
        requireNotNull(request, "请求体不能为空");
        request.validate();

        String rawBody;
        try {
            rawBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 WebHook 请求体失败", e);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", request.getEvent());
        payload.put("data", request.getData());

        shipmentWebhookService.handleSeventeenTrackWebhook(sign, rawBody, payload);
        return ResponseEntity.ok(Result.ok(null));
    }
}

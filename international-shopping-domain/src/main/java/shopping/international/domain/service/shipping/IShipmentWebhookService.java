package shopping.international.domain.service.shipping;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 物流 WebHook 领域服务接口
 */
public interface IShipmentWebhookService {

    /**
     * 处理 17Track WebHook 回调
     *
     * @param signHeader 签名头
     * @param rawBody 原始请求体
     * @param payload 解析后的请求体
     */
    void handleSeventeenTrackWebhook(@NotNull String signHeader,
                                     @NotNull String rawBody,
                                     @NotNull Map<String, Object> payload);
}

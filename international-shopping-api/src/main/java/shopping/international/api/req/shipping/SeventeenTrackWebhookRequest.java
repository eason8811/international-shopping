package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 17Track 回调请求对象 (SeventeenTrackWebhookRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeventeenTrackWebhookRequest implements Verifiable {
    /**
     * 回调事件类型
     */
    @Nullable
    private String event;

    /**
     * 回调数据载荷
     */
    @Nullable
    private Map<String, Object> data;

    /**
     * 对 17Track 回调请求参数进行校验与规范化
     */
    @Override
    public void validate() {
        event = normalizeNotNullField(event, "event 不能为空",
                SeventeenTrackWebhookRequest::isSupportedEvent,
                "event 仅支持 TRACKING_UPDATED 或 TRACKING_STOPPED")
                .toUpperCase(Locale.ROOT);

        requireNotNull(data, "data 不能为空");
    }

    /**
     * 判断回调事件是否受支持
     *
     * @param raw 原始事件值
     * @return 若事件受支持则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isSupportedEvent(String raw) {
        String value = raw.strip().toUpperCase(Locale.ROOT);
        return "TRACKING_UPDATED".equals(value) || "TRACKING_STOPPED".equals(value);
    }
}

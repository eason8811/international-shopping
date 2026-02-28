package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * WebSocket 会话创建请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsWsSessionCreateRequest implements Verifiable {
    /**
     * 订阅的工单号列表
     */
    @Nullable
    private List<String> ticketNos;
    /**
     * 订阅的工单 ID 列表
     */
    @Nullable
    private List<Long> ticketIds;
    /**
     * 订阅的事件类型列表
     */
    @Nullable
    private List<String> eventTypes;
    /**
     * 续传锚点事件 ID
     */
    @Nullable
    private String lastEventId;

    /**
     * 对 WebSocket 会话创建参数进行校验与规范化
     */
    @Override
    public void validate() {
        ticketNos = normalizeTicketNoList(ticketNos);
        ticketIds = normalizeTicketIdList(ticketIds);
        eventTypes = normalizeEventTypeList(eventTypes);

        lastEventId = normalizeNullableField(lastEventId, "lastEventId 不能为空",
                value -> value.length() <= 64,
                "lastEventId 长度不能超过 64 个字符");

    }

    /**
     * 规范化工单号列表并去重
     *
     * @param values 原始工单号列表
     * @return 规范化后的工单号列表
     */
    private static List<String> normalizeTicketNoList(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 100, "ticketNos 元素数量不能超过 100");

        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, "ticketNos 元素不能为空",
                    item -> item.length() >= 10 && item.length() <= 32,
                    "ticketNos 元素长度需在 10~32 个字符之间");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }

    /**
     * 规范化工单 ID 列表并去重
     *
     * @param values 原始工单 ID 列表
     * @return 规范化后的工单 ID 列表
     */
    private static List<Long> normalizeTicketIdList(@Nullable List<Long> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 100, "ticketIds 元素数量不能超过 100");

        Set<Long> dedup = new LinkedHashSet<>();
        for (Long value : values) {
            requireNotNull(value, "ticketIds 元素不能为空");
            require(value >= 1, "ticketIds 元素必须大于等于 1");
            dedup.add(value);
        }
        return new ArrayList<>(dedup);
    }

    /**
     * 规范化事件类型列表并去重
     *
     * @param values 原始事件类型列表
     * @return 规范化后的事件类型列表
     */
    private static List<String> normalizeEventTypeList(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 20, "eventTypes 元素数量不能超过 20");

        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, "eventTypes 元素不能为空",
                    CsWsSessionCreateRequest::isEventType,
                    "eventTypes 存在不支持的值").toUpperCase(Locale.ROOT);
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }

    /**
     * 判断是否为受支持的事件类型
     *
     * @param value 原始事件类型
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isEventType(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WS_CONNECTED", "MESSAGE_CREATED", "MESSAGE_UPDATED", "MESSAGE_RECALLED", "TICKET_READ_UPDATED", "TICKET_STATUS_CHANGED", "TICKET_ASSIGNMENT_CHANGED" ->
                    true;
            default -> false;
        };
    }
}

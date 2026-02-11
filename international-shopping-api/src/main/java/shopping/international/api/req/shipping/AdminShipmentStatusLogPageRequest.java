package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧物流状态日志分页查询请求对象 (AdminShipmentStatusLogPageRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShipmentStatusLogPageRequest implements Verifiable {
    /**
     * 物流单主键 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
    /**
     * 起始状态
     */
    @Nullable
    private String fromStatus;
    /**
     * 目标状态
     */
    @Nullable
    private String toStatus;
    /**
     * 事件来源
     */
    @Nullable
    private String sourceType;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 事件时间起始
     */
    @Nullable
    private LocalDateTime eventTimeFrom;
    /**
     * 事件时间结束
     */
    @Nullable
    private LocalDateTime eventTimeTo;
    /**
     * 创建时间起始
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间结束
     */
    @Nullable
    private LocalDateTime createdTo;
    /**
     * 页码, 默认 1
     */
    @Nullable
    private Integer page = 1;
    /**
     * 每页大小, 默认 20, 最大 200
     */
    @Nullable
    private Integer size = 20;
    /**
     * 排序字段与方向
     */
    @Nullable
    private String sort = "created_at,desc";

    /**
     * 对状态日志查询参数进行校验与规范化
     */
    @Override
    public void validate() {
        if (shipmentId != null)
            require(shipmentId >= 1, "shipmentId 必须大于等于 1");

        orderNo = normalizeNullableField(orderNo, "orderNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "orderNo 长度需在 10~32 个字符之间");

        fromStatus = normalizeEnumValue(fromStatus, "fromStatus", AdminShipmentStatusLogPageRequest::isShipmentStatus);
        toStatus = normalizeEnumValue(toStatus, "toStatus", AdminShipmentStatusLogPageRequest::isShipmentStatus);
        sourceType = normalizeEnumValue(sourceType, "sourceType", AdminShipmentStatusLogPageRequest::isSourceType);

        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空", s -> s.length() <= 128, "sourceRef 长度不能超过 128 个字符");
        carrierCode = normalizeNullableField(carrierCode, "carrierCode 不能为空", s -> s.length() <= 64, "carrierCode 长度不能超过 64 个字符");
        trackingNo = normalizeNullableField(trackingNo, "trackingNo 不能为空", s -> s.length() <= 128, "trackingNo 长度不能超过 128 个字符");

        if (eventTimeFrom != null && eventTimeTo != null)
            require(!eventTimeFrom.isAfter(eventTimeTo), "eventTimeFrom 不能晚于 eventTimeTo");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");

        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 20;
        if (size > 200)
            size = 200;

        sort = normalizeSort(sort, "created_at,desc");
    }

    /**
     * 规范化枚举值文本
     *
     * @param raw       原始值
     * @param fieldName 字段名
     * @param checker   校验函数
     * @return 规范化后的大写值, 若传入为空则返回 {@code null}
     */
    private static @Nullable String normalizeEnumValue(@Nullable String raw, String fieldName,
                                                       java.util.function.Function<String, Boolean> checker) {
        String value = normalizeNullableField(raw, fieldName + " 不能为空", checker, fieldName + " 值不支持");
        if (value == null)
            return null;
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * 判断是否为受支持的物流状态
     *
     * @param raw 原始状态值
     * @return 若状态受支持则返回 {@code true}, 否则返回 {@code false}
     */
    private static boolean isShipmentStatus(String raw) {
        String value = raw.strip().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "CREATED", "LABEL_CREATED", "PICKED_UP", "IN_TRANSIT", "CUSTOMS_PROCESSING", "CUSTOMS_HOLD", "CUSTOMS_RELEASED",
                    "HANDED_OVER", "OUT_FOR_DELIVERY", "DELIVERED", "EXCEPTION", "RETURNED", "CANCELLED", "LOST" -> true;
            default -> false;
        };
    }

    /**
     * 判断是否为受支持的事件来源
     *
     * @param raw 原始来源值
     * @return 若来源受支持则返回 {@code true}, 否则返回 {@code false}
     */
    private static boolean isSourceType(String raw) {
        String value = raw.strip().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "CARRIER_WEBHOOK", "CARRIER_POLL", "SYSTEM_JOB", "MANUAL", "API" -> true;
            default -> false;
        };
    }

    /**
     * 规范化排序参数
     *
     * @param rawSort      原始排序字符串
     * @param defaultValue 默认值
     * @return 规范化后的排序字符串
     */
    private static String normalizeSort(@Nullable String rawSort, String defaultValue) {
        if (rawSort == null || rawSort.isBlank())
            return defaultValue;

        String value = rawSort.strip();
        String[] parts = value.split(",");
        require(parts.length == 2, "sort 格式需为 field,asc|desc");

        String field = parts[0].strip();
        String direction = parts[1].strip().toLowerCase(Locale.ROOT);

        require(field.matches("^[a-zA-Z_]{2,32}$"), "sort 字段名格式不合法");
        require("asc".equals(direction) || "desc".equals(direction), "sort 排序方向仅支持 asc 或 desc");

        return field + "," + direction;
    }
}

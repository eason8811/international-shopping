package shopping.international.api.req.shipping;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧物流单分页查询请求对象 (AdminShipmentPageRequest)
 */
@Data
@NoArgsConstructor
public class AdminShipmentPageRequest implements Verifiable {
    /**
     * 物流单号
     */
    @Nullable
    private String shipmentNo;
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
    /**
     * 订单主键 ID
     */
    @Nullable
    private Long orderId;
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
     * 三方物流外部单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 物流状态筛选集合
     */
    @Nullable
    private List<String> statusIn;
    /**
     * 更新时间下限
     */
    @Nullable
    private LocalDateTime updatedFrom;
    /**
     * 更新时间上限
     */
    @Nullable
    private LocalDateTime updatedTo;
    /**
     * 创建时间下限
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间上限
     */
    @Nullable
    private LocalDateTime createdTo;
    /**
     * 页码, 默认 1
     */
    @NotNull
    private Integer page = 1;
    /**
     * 每页条数, 默认 20, 最大 200
     */
    @NotNull
    private Integer size = 20;
    /**
     * 排序字段与方向, 格式为 {@code field,asc|desc}
     */
    @NotNull
    private String sort = "updated_at,desc";

    /**
     * 管理侧物流分页查询请求对象, 用于构建分页查询物流单据的参数.
     * 支持通过多种条件进行筛选, 包括物流单号, 订单号, 物流状态等, 并允许指定排序方式和分页信息.
     *
     * @param shipmentNo    物流单号
     * @param orderNo       订单号
     * @param orderId       订单 ID
     * @param carrierCode   承运商代码
     * @param trackingNo    追踪编号
     * @param extExternalId 外部系统标识
     * @param statusIn      物流状态集合, 允许筛选多个状态
     * @param updatedFrom   更新时间起始点
     * @param updatedTo     更新时间结束点
     * @param createdFrom   创建时间起始点
     * @param createdTo     创建时间结束点
     * @param page          当前页码, 默认值为 1
     * @param size          每页条目数, 默认值为 20
     * @param sort          排序规则, 格式为 {@code field,asc|desc}, 默认按更新时间降序排列
     */
    public AdminShipmentPageRequest(@Nullable String shipmentNo,
                                    @Nullable String orderNo,
                                    @Nullable Long orderId,
                                    @Nullable String carrierCode,
                                    @Nullable String trackingNo,
                                    @Nullable String extExternalId,
                                    @Nullable List<String> statusIn,
                                    @Nullable LocalDateTime updatedFrom,
                                    @Nullable LocalDateTime updatedTo,
                                    @Nullable LocalDateTime createdFrom,
                                    @Nullable LocalDateTime createdTo,
                                    @Nullable Integer page,
                                    @Nullable Integer size,
                                    @Nullable String sort) {
        this.shipmentNo = shipmentNo;
        this.orderNo = orderNo;
        this.orderId = orderId;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.extExternalId = extExternalId;
        this.statusIn = statusIn;
        this.updatedFrom = updatedFrom;
        this.updatedTo = updatedTo;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.page = page == null ? 1 : page;
        this.size = size == null ? 20 : size;
        this.sort = sort == null || sort.isBlank() ? "updated_at,desc" : sort;
    }

    /**
     * 对管理侧物流分页查询参数进行校验与规范化
     */
    @Override
    public void validate() {
        shipmentNo = normalizeNullableField(shipmentNo, "shipmentNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "shipmentNo 长度需在 10~32 个字符之间");
        orderNo = normalizeNullableField(orderNo, "orderNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "orderNo 长度需在 10~32 个字符之间");

        if (orderId != null)
            require(orderId >= 1, "orderId 必须大于等于 1");

        carrierCode = normalizeNullableField(carrierCode, "carrierCode 不能为空", s -> s.length() <= 64, "carrierCode 长度不能超过 64 个字符");
        trackingNo = normalizeNullableField(trackingNo, "trackingNo 不能为空", s -> s.length() <= 128, "trackingNo 长度不能超过 128 个字符");
        extExternalId = normalizeNullableField(extExternalId, "extExternalId 不能为空", s -> s.length() <= 128, "extExternalId 长度不能超过 128 个字符");

        statusIn = normalizeStatusList(statusIn);

        if (updatedFrom != null && updatedTo != null)
            require(!updatedFrom.isAfter(updatedTo), "updatedFrom 不能晚于 updatedTo");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");

        if (page < 1)
            page = 1;
        if (size < 1)
            size = 20;
        if (size > 200)
            size = 200;

        sort = normalizeSort(sort);
    }

    /**
     * 规范化状态筛选集合
     *
     * @param rawStatus 原始状态集合
     * @return 规范化后的状态集合
     */
    private static List<String> normalizeStatusList(@Nullable List<String> rawStatus) {
        if (rawStatus == null || rawStatus.isEmpty())
            return List.of("CREATED", "LABEL_CREATED");

        Set<String> dedup = new LinkedHashSet<>();
        for (String status : rawStatus) {
            String normalized = normalizeNullableField(status, "statusIn 元素不能为空",
                    AdminShipmentPageRequest::isShipmentStatus,
                    "statusIn 存在不支持的状态值");
            if (normalized == null)
                continue;
            dedup.add(normalized.toUpperCase(Locale.ROOT));
        }

        return dedup.isEmpty() ? List.of("CREATED", "LABEL_CREATED") : new ArrayList<>(dedup);
    }

    /**
     * 判断是否为受支持的物流状态
     *
     * @param status 状态文本
     * @return 若状态受支持则返回 {@code true}, 否则返回 {@code false}
     */
    private static boolean isShipmentStatus(String status) {
        String normalized = status.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CREATED", "LABEL_CREATED", "PICKED_UP", "IN_TRANSIT", "CUSTOMS_PROCESSING", "CUSTOMS_HOLD",
                 "CUSTOMS_RELEASED",
                 "HANDED_OVER", "OUT_FOR_DELIVERY", "DELIVERED", "EXCEPTION", "RETURNED", "CANCELLED", "LOST" -> true;
            default -> false;
        };
    }

    /**
     * 规范化排序参数
     *
     * @param rawSort 原始排序字符串
     * @return 规范化后的排序字符串
     */
    private static String normalizeSort(@NotNull String rawSort) {
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

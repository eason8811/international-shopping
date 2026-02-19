package shopping.international.domain.model.enums.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 17Track 子状态枚举, 用于统一管理 sub_status 映射语义
 *
 * <p>说明, 非全部状态都会推进本地状态, 部分状态仅记录日志并保持当前状态</p>
 */
public enum SeventeenTrackSubStatus {
    /**
     * 单号无效, 仅在 CREATED, LABEL_CREATED 阶段建议推进为 EXCEPTION
     */
    NOT_FOUND_INVALID_CODE("NotFound_InvalidCode", MatchMode.EXACT, ShipmentStatus.EXCEPTION, false),
    /**
     * 未找到其他原因, 仅记录日志
     */
    NOT_FOUND_OTHER("NotFound_Other", MatchMode.EXACT, null, true),
    /**
     * 已收到预报
     */
    INFO_RECEIVED("InfoReceived", MatchMode.EXACT, ShipmentStatus.LABEL_CREATED, false),
    /**
     * 已揽收
     */
    IN_TRANSIT_PICKED_UP("InTransit_PickedUp", MatchMode.EXACT, ShipmentStatus.PICKED_UP, false),
    /**
     * 运输离开发车节点
     */
    IN_TRANSIT_DEPARTURE("InTransit_Departure", MatchMode.EXACT, ShipmentStatus.IN_TRANSIT, false),
    /**
     * 运输到达中转节点
     */
    IN_TRANSIT_ARRIVAL("InTransit_Arrival", MatchMode.EXACT, ShipmentStatus.HANDED_OVER, false),
    /**
     * 清关处理中
     */
    IN_TRANSIT_CUSTOMS_PROCESSING("InTransit_CustomsProcessing", MatchMode.EXACT, ShipmentStatus.CUSTOMS_PROCESSING, false),
    /**
     * 清关放行
     */
    IN_TRANSIT_CUSTOMS_RELEASED("InTransit_CustomsReleased", MatchMode.EXACT, ShipmentStatus.CUSTOMS_RELEASED, false),
    /**
     * 清关补充信息
     */
    IN_TRANSIT_CUSTOMS_REQUIRING_INFORMATION("InTransit_CustomsRequiringInformation", MatchMode.EXACT, ShipmentStatus.CUSTOMS_HOLD, false),
    /**
     * 运输其他状态
     */
    IN_TRANSIT_OTHER("InTransit_Other", MatchMode.EXACT, ShipmentStatus.IN_TRANSIT, false),
    /**
     * 超时其他状态, 仅记录日志
     */
    EXPIRED_OTHER("Expired_Other", MatchMode.EXACT, null, true),
    /**
     * 可自提, 归并为派送中
     */
    AVAILABLE_FOR_PICKUP_OTHER("AvailableForPickup_Other", MatchMode.EXACT, ShipmentStatus.OUT_FOR_DELIVERY, false),
    /**
     * 派送中
     */
    OUT_FOR_DELIVERY_OTHER("OutForDelivery_Other", MatchMode.EXACT, ShipmentStatus.OUT_FOR_DELIVERY, false),
    /**
     * 已签收
     */
    DELIVERED_OTHER("Delivered_Other", MatchMode.EXACT, ShipmentStatus.DELIVERED, false),
    /**
     * 派送失败前缀, 推进为 EXCEPTION
     */
    DELIVERY_FAILURE_ANY("DeliveryFailure_", MatchMode.PREFIX, ShipmentStatus.EXCEPTION, false),
    /**
     * 异常退回中
     */
    EXCEPTION_RETURNING("Exception_Returning", MatchMode.EXACT, ShipmentStatus.RETURNED, false),
    /**
     * 异常已退回
     */
    EXCEPTION_RETURNED("Exception_Returned", MatchMode.EXACT, ShipmentStatus.RETURNED, false),
    /**
     * 异常丢失
     */
    EXCEPTION_LOST("Exception_Lost", MatchMode.EXACT, ShipmentStatus.LOST, false),
    /**
     * 异常取消
     */
    EXCEPTION_CANCEL("Exception_Cancel", MatchMode.EXACT, ShipmentStatus.CANCELLED, false),
    /**
     * 异常延迟, 仅记录日志
     */
    EXCEPTION_DELAYED("Exception_Delayed", MatchMode.EXACT, null, true),
    /**
     * 异常销毁
     */
    EXCEPTION_DESTROYED("Exception_Destroyed", MatchMode.EXACT, ShipmentStatus.EXCEPTION, false),
    /**
     * 其他异常前缀, 推进为 EXCEPTION
     */
    EXCEPTION_ANY("Exception_", MatchMode.PREFIX, ShipmentStatus.EXCEPTION, false);

    /**
     * 匹配模式
     */
    private enum MatchMode {
        /**
         * 全量匹配
         */
        EXACT,
        /**
         * 前缀匹配
         */
        PREFIX
    }

    /**
     * 原始 sub_status 文本, 保留 17Track 命名
     */
    private final String raw;
    /**
     * 匹配模式
     */
    private final MatchMode matchMode;
    /**
     * 映射目标状态, 为空表示保持当前状态
     */
    @Nullable
    private final ShipmentStatus targetStatus;
    /**
     * 是否保持当前状态
     */
    private final boolean keepCurrent;

    /**
     * 构造枚举
     *
     * @param raw 原始 sub_status
     * @param matchMode 匹配模式
     * @param targetStatus 映射目标状态
     * @param keepCurrent 是否保持当前状态
     */
    SeventeenTrackSubStatus(@NotNull String raw,
                            @NotNull MatchMode matchMode,
                            @Nullable ShipmentStatus targetStatus,
                            boolean keepCurrent) {
        this.raw = raw;
        this.matchMode = matchMode;
        this.targetStatus = targetStatus;
        this.keepCurrent = keepCurrent;
    }

    /**
     * 解析 sub_status 文本
     *
     * @param rawStatus 原始状态文本
     * @return 匹配到的枚举
     */
    public static @NotNull Optional<SeventeenTrackSubStatus> resolve(@NotNull String rawStatus) {
        requireNotBlank(rawStatus, "rawStatus 不能为空");
        String normalized = rawStatus.strip();

        for (SeventeenTrackSubStatus status : Arrays.stream(values()).filter(it -> it.matchMode == MatchMode.EXACT).toList())
            if (status.raw.equalsIgnoreCase(normalized))
                return Optional.of(status);

        for (SeventeenTrackSubStatus status : Arrays.stream(values()).filter(it -> it.matchMode == MatchMode.PREFIX).toList())
            if (normalized.toLowerCase(Locale.ROOT).startsWith(status.raw.toLowerCase(Locale.ROOT)))
                return Optional.of(status);

        return Optional.empty();
    }

    /**
     * 返回映射目标状态
     *
     * @return 目标状态, keepCurrent 场景为 null
     */
    public @Nullable ShipmentStatus targetStatus() {
        return targetStatus;
    }

    /**
     * 是否保持当前状态
     *
     * @return true 表示仅记录日志
     */
    public boolean keepCurrent() {
        return keepCurrent;
    }

    /**
     * 是否为 NotFound_InvalidCode
     *
     * @return true 表示匹配到该状态
     */
    public boolean isNotFoundInvalidCode() {
        return this == NOT_FOUND_INVALID_CODE;
    }
}

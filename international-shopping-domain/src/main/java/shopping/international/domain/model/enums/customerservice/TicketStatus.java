package shopping.international.domain.model.enums.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单状态枚举, 对齐表 `cs_ticket.status`, 并内聚状态机流转规则
 * <ul>
 *     <li>{@code OPEN:} 新建待处理</li>
 *     <li>{@code IN_PROGRESS:} 处理中</li>
 *     <li>{@code AWAITING_USER:} 等待用户反馈</li>
 *     <li>{@code AWAITING_CARRIER:} 等待承运商反馈</li>
 *     <li>{@code ON_HOLD:} 暂挂</li>
 *     <li>{@code RESOLVED:} 已解决</li>
 *     <li>{@code REJECTED:} 已驳回</li>
 *     <li>{@code CLOSED:} 已关闭</li>
 * </ul>
 */
public enum TicketStatus {
    /**
     * 新建待处理
     */
    OPEN,
    /**
     * 处理中
     */
    IN_PROGRESS,
    /**
     * 等待用户反馈
     */
    AWAITING_USER,
    /**
     * 等待承运商反馈
     */
    AWAITING_CARRIER,
    /**
     * 暂挂
     */
    ON_HOLD,
    /**
     * 已解决
     */
    RESOLVED,
    /**
     * 已驳回
     */
    REJECTED,
    /**
     * 已关闭
     */
    CLOSED;

    /**
     * 将字符串转换为工单状态枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单状态枚举
     */
    public static TicketStatus fromValue(String value) {
        requireNotBlank(value, "ticketStatus 不能为空");
        return TicketStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前状态是否属于进行中状态
     *
     * @return 若属于进行中状态, 返回 true
     */
    public boolean isOpenState() {
        return switch (this) {
            case OPEN, IN_PROGRESS, AWAITING_USER, AWAITING_CARRIER, ON_HOLD -> true;
            default -> false;
        };
    }

    /**
     * 判断当前状态是否允许流转到目标状态
     *
     * @param toStatus        目标状态
     * @param statusChangedAt 当前状态进入时间, 仅用于 RESOLVED 或 REJECTED 的复开窗口判断
     * @param now             当前时间
     * @return 若允许流转, 返回 true
     */
    public boolean canTransitTo(@NotNull TicketStatus toStatus,
                                @Nullable LocalDateTime statusChangedAt,
                                @NotNull LocalDateTime now) {
        requireNotNull(toStatus, "toStatus 不能为空");
        requireNotNull(now, "now 不能为空");
        if (this == toStatus)
            return false;
        return switch (this) {
            case OPEN -> toStatus == IN_PROGRESS
                    || toStatus == AWAITING_USER
                    || toStatus == AWAITING_CARRIER
                    || toStatus == ON_HOLD
                    || toStatus == REJECTED
                    || toStatus == CLOSED;
            case IN_PROGRESS -> toStatus == AWAITING_USER
                    || toStatus == AWAITING_CARRIER
                    || toStatus == ON_HOLD
                    || toStatus == RESOLVED
                    || toStatus == REJECTED
                    || toStatus == CLOSED;
            case AWAITING_USER -> toStatus == IN_PROGRESS
                    || toStatus == ON_HOLD
                    || toStatus == RESOLVED
                    || toStatus == REJECTED
                    || toStatus == CLOSED;
            case AWAITING_CARRIER -> toStatus == IN_PROGRESS
                    || toStatus == ON_HOLD
                    || toStatus == RESOLVED
                    || toStatus == REJECTED
                    || toStatus == CLOSED;
            case ON_HOLD -> toStatus == IN_PROGRESS
                    || toStatus == AWAITING_USER
                    || toStatus == AWAITING_CARRIER
                    || toStatus == RESOLVED
                    || toStatus == REJECTED
                    || toStatus == CLOSED;
            case RESOLVED, REJECTED -> {
                if (toStatus == CLOSED)
                    yield true;
                if (toStatus == IN_PROGRESS && statusChangedAt != null)
                    yield !statusChangedAt.isBefore(now.minusDays(7));
                yield false;
            }
            case CLOSED -> false;
        };
    }
}

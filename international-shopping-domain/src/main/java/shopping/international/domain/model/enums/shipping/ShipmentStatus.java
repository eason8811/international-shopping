package shopping.international.domain.model.enums.shipping;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 物流单状态枚举
 *
 * <p>状态划分为三类: </p>
 * <ul>
 *     <li>主链路状态, 具备线性优先级, 用于防止状态回退</li>
 *     <li>旁路状态 {@link #EXCEPTION}, 允许从异常恢复回主链路</li>
 *     <li>强终态 {@link #DELIVERED}, {@link #RETURNED}, {@link #LOST}, {@link #CANCELLED}</li>
 * </ul>
 */
@Getter
public enum ShipmentStatus {
    /**
     * 已创建, 尚未生成真实面单
     */
    CREATED(1, false),
    /**
     * 已生成面单
     */
    LABEL_CREATED(2, false),
    /**
     * 已揽收
     */
    PICKED_UP(3, false),
    /**
     * 干线运输中
     */
    IN_TRANSIT(4, false),
    /**
     * 清关处理中
     */
    CUSTOMS_PROCESSING(5, false),
    /**
     * 清关暂扣
     */
    CUSTOMS_HOLD(6, false),
    /**
     * 清关放行
     */
    CUSTOMS_RELEASED(7, false),
    /**
     * 进入下一段运输
     */
    HANDED_OVER(8, false),
    /**
     * 派送中
     */
    OUT_FOR_DELIVERY(9, false),
    /**
     * 已签收, 强终态
     */
    DELIVERED(10, true),
    /**
     * 异常旁路状态, 不参与线性优先级
     */
    EXCEPTION(null, false),
    /**
     * 已退回, 强终态
     */
    RETURNED(null, true),
    /**
     * 已取消, 强终态
     */
    CANCELLED(null, true),
    /**
     * 已丢失, 强终态
     */
    LOST(null, true);

    /**
     * 主链路线性优先级, 为空表示不参与线性排序
     */
    private final Integer linearPriority;

    /**
     * 是否为强终态
     */
    private final boolean strongFinalState;

    /**
     * 构造物流状态枚举
     *
     * @param linearPriority 线性优先级, 不参与线性排序时可为空
     * @param strongFinalState 是否为强终态
     */
    ShipmentStatus(Integer linearPriority, boolean strongFinalState) {
        this.linearPriority = linearPriority;
        this.strongFinalState = strongFinalState;
    }

    /**
     * 判断当前状态是否参与主链路线性优先级比较
     *
     * @return 若参与线性优先级比较返回 {@code true}, 否则返回 {@code false}
     */
    public boolean hasLinearPriority() {
        return linearPriority != null;
    }

    /**
     * 获取线性优先级
     *
     * @return 线性优先级
     */
    public int linearPriority() {
        requireNotNull(linearPriority, "当前状态不参与线性优先级排序");
        return linearPriority;
    }

    /**
     * 判断当前状态相对给定来源状态是否属于回退
     *
     * @param fromStatus 来源状态
     * @return 若属于主链路回退返回 {@code true}, 否则返回 {@code false}
     */
    public boolean isRollbackComparedTo(@NotNull ShipmentStatus fromStatus) {
        requireNotNull(fromStatus, "fromStatus 不能为空");
        if (!hasLinearPriority() || !fromStatus.hasLinearPriority())
            return false;
        return linearPriority() < fromStatus.linearPriority();
    }
}

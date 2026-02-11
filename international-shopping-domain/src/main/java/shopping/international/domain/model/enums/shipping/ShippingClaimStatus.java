package shopping.international.domain.model.enums.shipping;

/**
 * 物流理赔状态枚举
 *
 * <p>状态流转建议: {@code FILED -> UNDER_REVIEW -> APPROVED/REJECTED -> PAID -> CLOSED}</p>
 */
public enum ShippingClaimStatus {
    /**
     * 已发起
     */
    FILED,
    /**
     * 审核中
     */
    UNDER_REVIEW,
    /**
     * 已通过
     */
    APPROVED,
    /**
     * 已拒绝
     */
    REJECTED,
    /**
     * 已打款
     */
    PAID,
    /**
     * 已关闭
     */
    CLOSED
}

package shopping.international.domain.model.enums.user;

/**
 * 地址校验状态
 * <ul>
 *     <li>{@code UNVALIDATED:} 不合法</li>
 *     <li>{@code ACCEPT:} 接受</li>
 *     <li>{@code REVIEW:} 需要审查</li>
 *     <li>{@code FIX:} 修复</li>
 *     <li>{@code REJECT:} 拒绝</li>
 * </ul>
 */
public enum AddressValidationStatus {
    UNVALIDATED,
    ACCEPT,
    REVIEW,
    FIX,
    REJECT
}

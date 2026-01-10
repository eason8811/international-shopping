package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 折扣码筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class DiscountCodeSearchCriteria implements Verifiable {
    /**
     * 折扣码文本过滤 (可空)
     */
    @Nullable
    private String keyword;
    /**
     * 策略ID过滤 (可空)
     */
    @Nullable
    private Long policyId;
    /**
     * 适用范围模式过滤 (ALL/INCLUDE/EXCLUDE), 可空
     */
    @Nullable
    private DiscountScopeMode scopeMode;
    /**
     * 过期时间起 (可空, 含)
     */
    @Nullable
    private LocalDateTime expiresFrom;
    /**
     * 过期时间止 (可空, 含)
     */
    @Nullable
    private LocalDateTime expiresTo;
    /**
     * 是否永久有效 (可空)
     */
    @Nullable
    private Boolean permanent;

    /**
     * 校验筛选条件
     *
     * <p>若同时提供 {@code expiresFrom/expiresTo}, 则保证起止时间不反转。</p>
     */
    @Override
    public void validate() {
        keyword = keyword == null || keyword.isBlank() ? null : keyword;
        if (expiresFrom != null && expiresTo != null)
            require(!expiresFrom.isAfter(expiresTo), "expiresFrom 不能晚于 expiresTo");
    }
}

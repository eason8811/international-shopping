package shopping.international.api.resp.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;

import java.time.LocalDateTime;

/**
 * 折扣码响应 (DiscountCodeRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeRespond {
    /**
     * 折扣码 ID
     */
    private Long id;
    /**
     * 折扣码
     */
    private String code;
    /**
     * 折扣策略 ID
     */
    private Long policyId;
    /**
     * 折扣码名称
     */
    private String name;
    /**
     * 适用范围模式
     */
    private DiscountScopeMode scopeMode;
    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}


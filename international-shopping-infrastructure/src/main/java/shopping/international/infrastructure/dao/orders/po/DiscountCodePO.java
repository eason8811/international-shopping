package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: discount_code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("discount_code")
public class DiscountCodePO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 折扣码 (6 位)
     */
    @TableField("code")
    private String code;
    /**
     * 策略 ID
     */
    @TableField("policy_id")
    private Long policyId;
    /**
     * 名称
     */
    @TableField("name")
    private String name;
    /**
     * 适用范围模式 (ALL/INCLUDE/EXCLUDE)
     */
    @TableField("scope_mode")
    private String scopeMode;
    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}


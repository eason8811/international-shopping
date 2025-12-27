package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: discount_code_product
 *
 * <p>折扣码与 SPU 的映射表 (复合主键)</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("discount_code_product")
public class DiscountCodeProductPO {

    /**
     * 折扣码 ID
     */
    @TableField("discount_code_id")
    private Long discountCodeId;
    /**
     * 商品 SPU ID
     */
    @TableField("product_id")
    private Long productId;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}


package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_like
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_like")
public class ProductLikePO {
    /**
     * 用户ID, 指向 user_account.id
     */
    @TableId("user_id")
    private Long userId;
    /**
     * SPU ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;
    /**
     * Like 时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

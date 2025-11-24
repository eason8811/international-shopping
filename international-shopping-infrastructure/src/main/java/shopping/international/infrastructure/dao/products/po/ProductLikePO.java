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
    @TableId("user_id")
    private Long userId;
    @TableField("product_id")
    private Long productId;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

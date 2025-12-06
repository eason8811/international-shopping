package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品点赞关系持久化对象, 对应表 product_like
 * <p>记录用户与商品的点赞映射</p>
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
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 商品ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 点赞时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

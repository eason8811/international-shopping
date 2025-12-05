package shopping.international.infrastructure.dao.products.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
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
@Table("product_like")
public class ProductLikePO {

    /**
     * 用户ID, 指向 user_account.id
     */
    @Id(keyType = KeyType.None)
    @Column("user_id")
    private Long userId;

    /**
     * 商品ID, 指向 product.id
     */
    @Id(keyType = KeyType.None)
    @Column("product_id")
    private Long productId;

    /**
     * 点赞时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}

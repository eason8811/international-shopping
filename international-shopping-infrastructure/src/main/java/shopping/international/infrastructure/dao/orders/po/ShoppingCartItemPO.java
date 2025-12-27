package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: shopping_cart_item
 *
 * <p>用户购物车条目表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"addedAt"})
@TableName("shopping_cart_item")
public class ShoppingCartItemPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * SKU ID
     */
    @TableField("sku_id")
    private Long skuId;

    /**
     * 数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 是否勾选
     */
    @TableField("selected")
    private Boolean selected;

    /**
     * 加购时间
     */
    @TableField(value = "added_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime addedAt;
}


package shopping.international.infrastructure.dao.orders.po;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 购物车条目展示视图 PO
 *
 * <p>用于联表查询 {@code shopping_cart_item + product + product_i18n + product_price} 的展示字段</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemViewPO {

    /**
     * 购物车条目 ID
     */
    private Long id;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 数量
     */
    private Integer quantity;
    /**
     * 是否勾选
     */
    private Boolean selected;
    /**
     * 加购时间
     */
    private LocalDateTime addedAt;
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 封面图
     */
    private String coverImageUrl;
    /**
     * 展示币种
     */
    private String currency;
    /**
     * 展示单价
     */
    private Long unitPrice;
}

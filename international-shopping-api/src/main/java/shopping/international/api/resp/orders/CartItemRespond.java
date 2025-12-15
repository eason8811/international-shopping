package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车条目响应 (CartItemRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRespond {
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
     * 商品 ID (可选冗余展示字段)
     */
    private Long productId;
    /**
     * 商品标题 (可选冗余展示字段)
     */
    private String title;
    /**
     * 商品封面图 URL (可选冗余展示字段)
     */
    private String coverImageUrl;
    /**
     * 展示币种 (可选)
     */
    private String currency;
    /**
     * 展示单价 (可选, 金额字符串)
     */
    private String unitPrice;
}


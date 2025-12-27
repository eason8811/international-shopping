package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 订单明细响应 (OrderItemRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRespond {
    /**
     * 明细 ID
     */
    private Long id;
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 折扣码 ID (可为空)
     */
    private Long discountCodeId;
    /**
     * 商品标题
     */
    private String title;
    /**
     * SKU 属性快照 (可为空, 动态字段)
     */
    private Map<String, Object> skuAttrs;
    /**
     * 封面图 URL (可为空)
     */
    private String coverImageUrl;
    /**
     * 单价 (金额字符串)
     */
    private String unitPrice;
    /**
     * 数量
     */
    private Integer quantity;
    /**
     * 小计金额 (金额字符串)
     */
    private String subtotalAmount;
}


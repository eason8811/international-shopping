package shopping.international.infrastructure.dao.orders.po;

import lombok.*;

import java.math.BigDecimal;

/**
 * SKU 可售快照 PO
 *
 * <p>用于下单/试算读取 SKU 的库存、价格与标题等快照字段</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuSaleSnapshotPO {
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品封面图
     */
    private String coverImageUrl;
    /**
     * SKU 属性 JSON (可为空)
     */
    private String skuAttrsJson;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 当前库存
     */
    private Integer stock;
}


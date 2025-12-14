package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.SkuStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公共 SKU 响应 ProductSkuRespond
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRespond {
    /**
     * SKU ID
     */
    private Long id;
    /**
     * SKU 编码
     */
    private String skuCode;
    /**
     * 库存
     */
    private Integer stock;
    /**
     * 重量
     */
    private BigDecimal weight;
    /**
     * 状态
     */
    private SkuStatus status;
    /**
     * 是否默认
     */
    private Boolean isDefault;
    /**
     * 条码
     */
    private String barcode;
    /**
     * 价格列表
     */
    private List<ProductPriceRespond> prices;
    /**
     * 规格绑定
     */
    private List<ProductSkuSpecRespond> specs;
    /**
     * 图片列表
     */
    private List<ProductImageRespond> images;

    /**
     * 用户侧价格响应 ProductPriceRespond
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceRespond {
        /**
         * 币种
         */
        private String currency;
        /**
         * 标价
         */
        private BigDecimal listPrice;
        /**
         * 促销价
         */
        private BigDecimal salePrice;
        /**
         * 是否启用
         */
        private Boolean isActive;
    }

    /**
     * 用户侧 SKU 规格绑定响应 ProductSkuSpecRespond
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSkuSpecRespond {
        /**
         * 规格 ID
         */
        private Long specId;
        /**
         * 规格编码
         */
        private String specCode;
        /**
         * 规格名称
         */
        private String specName;
        /**
         * 规格值 ID
         */
        private Long valueId;
        /**
         * 规格值编码
         */
        private String valueCode;
        /**
         * 规格值名称
         */
        private String valueName;
    }
}

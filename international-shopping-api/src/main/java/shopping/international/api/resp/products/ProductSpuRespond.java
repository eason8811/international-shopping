package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpuRespond {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 商品别名(SEO/路由)
     */
    private String slug;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 副标题
     */
    private String subtitle;
    /**
     * 商品描述
     */
    private String description;
    /**
     * 所属分类ID
     */
    private Long categoryId;
    /**
     * 分类别名(SEO/路由)
     */
    private String categorySlug;
    /**
     * 品牌文案(本地化)
     */
    private String brand;
    /**
     * 主图URL
     */
    private String coverImageUrl;
    /**
     * 总库存(聚合)
     */
    private Integer stockTotal;
    /**
     * 销量(聚合)
     */
    private Integer saleCount;
    /**
     * 规格类型(单/多规格)
     * <ul>
     *     <li>{@code SINGLE}: 单规格</li>
     *     <li>{@code VARIANT}: 多规格</li>
     * </ul>
     */
    private SkuType skuType;
    /**
     * 商品状态
     * <ul>
     *     <li>{@code DRAFT}: 草稿</li>
     *     <li>{@code ON_SALE}: 上架</li>
     *     <li>{@code OFF_SHELF}: 下架</li>
     *     <li>{@code DELETED}: 已删除</li>
     * </ul>
     */
    private ProductStatus status;
    /**
     * 标签(JSON)
     */
    private List<String> tags;
    /**
     * SPU 价格区间
     */
    private ProductPriceRangeRespond priceRange;
    /**
     * 商品图片列表
     */
    private List<ProductImageRespond> gallery;
    /**
     * 收藏时间
     */
    private LocalDateTime likedAt;

    /**
     * 价格区间响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceRangeRespond {
        /**
         * 价格结算货币
         */
        private String currency;
        /**
         * 商品原价最小值
         */
        private BigDecimal listPriceMin;
        /**
         * 商品原价最大值
         */
        private BigDecimal listPriceMax;
        /**
         * 商品促销价最小值
         */
        private BigDecimal salePriceMin;
        /**
         * 商品促销价最大值
         */
        private BigDecimal salePriceMax;
    }

}

package shopping.international.domain.model.vo.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户侧商品快照视图
 *
 * <p>承载列表/点赞查询返回的本地化商品概要信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicSnapshot implements Verifiable {
    /**
     * 商品 ID
     */
    @NotNull
    private Long id;
    /**
     * 商品 slug(已按 locale 覆盖)
     */
    @NotNull
    private String slug;
    /**
     * 商品标题(已按 locale 覆盖)
     */
    @NotNull
    private String title;
    /**
     * 商品副标题(已按 locale 覆盖)
     */
    @Nullable
    private String subtitle;
    /**
     * 商品描述(已按 locale 覆盖)
     */
    @Nullable
    private String description;
    /**
     * 分类 ID
     */
    @NotNull
    private Long categoryId;
    /**
     * 分类 slug(已按 locale 覆盖)
     */
    @NotNull
    private String categorySlug;
    /**
     * 品牌文案
     */
    @Nullable
    private String brand;
    /**
     * 主图 URL
     */
    @Nullable
    private String coverImageUrl;
    /**
     * 聚合库存
     */
    @NotNull
    private Integer stockTotal;
    /**
     * 聚合销量
     */
    @NotNull
    private Integer saleCount;
    /**
     * 规格类型
     */
    @NotNull
    private SkuType skuType;
    /**
     * 商品状态
     */
    @NotNull
    private ProductStatus status;
    /**
     * 标签列表(已去重)
     */
    @NotNull
    private List<String> tags = Collections.emptyList();
    /**
     * 商品图库
     */
    @NotNull
    private List<ProductImage> gallery = Collections.emptyList();
    /**
     * 价格区间
     */
    @NotNull
    private ProductPriceRangeView priceRange;
    /**
     * 点赞时间, 未点赞时为空
     */
    @Nullable
    private LocalDateTime likedAt;

    /**
     * 校验必填字段
     */
    @Override
    public void validate() {
        requireNotNull(id, "商品 ID 不能为空");
        requireNotNull(slug, "商品 slug 不能为空");
        requireNotNull(title, "商品标题不能为空");
        requireNotNull(categoryId, "分类 ID 不能为空");
        requireNotNull(categorySlug, "分类 slug 不能为空");
        requireNotNull(stockTotal, "库存不能为空");
        requireNotNull(saleCount, "销量不能为空");
        requireNotNull(skuType, "规格类型不能为空");
        requireNotNull(status, "商品状态不能为空");
        requireNotNull(priceRange, "价格区间不能为空");
        priceRange.validate();
    }

    /**
     * 价格区间视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceRangeView implements Verifiable {
        /**
         * 价格币种
         */
        @NotNull
        private String currency;
        /**
         * 标价最小值（最小货币单位）
         */
        @Nullable
        private Long listPriceMin;
        /**
         * 标价最大值（最小货币单位）
         */
        @Nullable
        private Long listPriceMax;
        /**
         * 促销价最小值（最小货币单位）
         */
        @Nullable
        private Long salePriceMin;
        /**
         * 促销价最大值（最小货币单位）
         */
        @Nullable
        private Long salePriceMax;

        /**
         * 校验币种字段
         */
        @Override
        public void validate() {
            requireNotNull(currency, "价格币种不能为空");
        }
    }
}

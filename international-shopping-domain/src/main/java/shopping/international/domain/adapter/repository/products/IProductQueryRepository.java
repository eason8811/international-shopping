package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductQueryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 商品读取仓储接口
 *
 * <p>职责：提供上架商品的查询能力, 聚合 SPU/SKU/价格/规格等数据</p>
 */
public interface IProductQueryRepository {

    /**
     * 按条件分页查询上架商品
     *
     * @param page       页码, 从1开始
     * @param size       每页数量
     * @param categoryId 分类ID, 可空
     * @param keyword    关键词, 可空
     * @param tags       标签, 可空
     * @param locale     语言, 用于关键字/标签匹配 i18n, 可空
     * @param currency   价格币种, 可空
     * @param priceMin   价格下限, 可空
     * @param priceMax   价格上限, 可空
     * @param sortBy     排序
     * @return 分页结果
     */
    @NotNull
    IProductQueryService.PageResult<Product> pageOnSaleProducts(int page,
                                                                int size,
                                                                @Nullable Long categoryId,
                                                                @Nullable String keyword,
                                                                @Nullable List<String> tags,
                                                                @Nullable String locale,
                                                                @Nullable String currency,
                                                                @Nullable BigDecimal priceMin,
                                                                @Nullable BigDecimal priceMax,
                                                                @NotNull ProductSort sortBy);

    /**
     * 按 slug 查询上架商品
     *
     * @param slug slug
     * @return 商品
     */
    @NotNull
    Optional<Product> findOnSaleBySlug(@NotNull String slug);

    /**
     * 按指定语言的 slug 查询上架商品
     *
     * @param slug   多语言 slug
     * @param locale 语言
     * @return 商品
     */
    @NotNull
    Optional<Product> findOnSaleByLocalizedSlug(@NotNull String slug, @NotNull String locale);

    /**
     * 按ID查询商品(不限制状态)
     *
     * @param productId 商品ID
     * @return 商品
     */
    @NotNull
    Optional<Product> findById(@NotNull Long productId);

    /**
     * 批量查询商品(不限制状态)
     *
     * @param productIds 商品ID集合
     * @return productId -> 商品
     */
    @NotNull
    Map<Long, Product> mapByIds(@NotNull Set<Long> productIds);

    /**
     * 批量查询商品 i18n
     *
     * @param productIds 商品ID集合
     * @param locale     语言
     * @return productId -> i18n
     */
    @NotNull
    Map<Long, ProductI18n> mapI18nByLocale(@NotNull Set<Long> productIds, @NotNull String locale);

    /**
     * 批量查询商品图片
     *
     * @param productIds 商品ID集合
     * @return productId -> 图片列表
     */
    @NotNull
    Map<Long, List<ProductImage>> mapProductImages(@NotNull Set<Long> productIds);

    /**
     * 按币种计算商品价格区间 (仅启用 SKU + 价格)
     *
     * @param productIds 商品ID集合
     * @param currency   币种
     * @return productId -> 价格区间
     */
    @NotNull
    Map<Long, ProductPriceRange> mapPriceRangeByProductIds(@NotNull Set<Long> productIds, @NotNull String currency);

    /**
     * 查询商品规格
     *
     * @param productId 商品ID
     * @return 规格列表
     */
    @NotNull
    List<ProductSpec> listSpecs(@NotNull Long productId);

    /**
     * 查询规格值
     *
     * @param productId 商品ID
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listSpecValues(@NotNull Long productId);

    /**
     * 规格 i18n
     *
     * @param specIds 规格ID集合
     * @param locale  语言
     * @return specId -> 名称
     */
    @NotNull
    Map<Long, String> mapSpecI18n(@NotNull Set<Long> specIds, @NotNull String locale);

    /**
     * 规格值 i18n
     *
     * @param valueIds 规格值ID集合
     * @param locale   语言
     * @return valueId -> 名称
     */
    @NotNull
    Map<Long, String> mapSpecValueI18n(@NotNull Set<Long> valueIds, @NotNull String locale);

    /**
     * 查询启用 SKU
     *
     * @param productId 商品ID
     * @return SKU 列表
     */
    @NotNull
    List<ProductSku> listEnabledSkus(@NotNull Long productId);

    /**
     * 批量查询 SKU 价格 (指定币种)
     *
     * @param skuIds   SKU ID 集合
     * @param currency 币种
     * @return skuId -> 价格列表
     */
    @NotNull
    Map<Long, List<ProductPrice>> mapPricesBySkuIds(@NotNull Set<Long> skuIds, @Nullable String currency);

    /**
     * SKU 图片
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 图片列表
     */
    @NotNull
    Map<Long, List<ProductImage>> mapSkuImages(@NotNull Set<Long> skuIds);

    /**
     * SKU 规格映射
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 规格值列表
     */
    @NotNull
    Map<Long, List<ProductSkuSpec>> mapSkuSpecs(@NotNull Set<Long> skuIds);
}

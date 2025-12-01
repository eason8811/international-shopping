package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.vo.products.*;

import java.util.*;

/**
 * 商品管理仓储接口
 *
 * <p>负责 product 及其 i18n、图库的持久化读写, 供管理端使用</p>
 */
public interface IProductAdminRepository {
    /**
     * 分页查询商品
     *
     * @param page           页码
     * @param size           每页大小
     * @param status         状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词, 可空
     * @param tag            标签, 可空
     * @param includeDeleted 是否包含已删除
     * @return 分页结果
     */
    @NotNull
    PageResult<Product> page(int page, int size, String status, String skuType, Long categoryId, String keyword, String tag, boolean includeDeleted);

    /**
     * 按 ID 查询商品
     *
     * @param productId 商品 ID
     * @return 商品
     */
    @NotNull
    Optional<Product> findById(@NotNull Long productId);

    /**
     * 批量查询商品
     *
     * @param productIds 商品 ID 集合
     * @return 商品映射
     */
    @NotNull
    Map<Long, Product> mapByIds(@NotNull Set<Long> productIds);

    /**
     * 批量查询商品多语言
     *
     * @param productIds 商品 ID 集合
     * @return productId -> 多语言列表
     */
    @NotNull
    Map<Long, List<ProductI18n>> mapI18n(@NotNull Collection<Long> productIds);

    /**
     * 获取商品图库
     *
     * @param productId 商品 ID
     * @return 图库列表
     */
    @NotNull
    List<ProductImage> listGallery(@NotNull Long productId);

    /**
     * 批量获取商品图库
     *
     * @param productIds 商品 ID
     * @return productId -> 图库
     */
    @NotNull
    Map<Long, List<ProductImage>> mapGallery(@NotNull Collection<Long> productIds);

    /**
     * 新增商品
     *
     * @param product 商品实体
     * @return 生成的主键 ID
     */
    @NotNull
    Long insert(@NotNull Product product);

    /**
     * 更新商品
     *
     * @param product 商品实体
     */
    void update(@NotNull Product product);

    /**
     * 批量 upsert 商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     */
    void upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads);

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图库列表
     */
    void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery);

    /**
     * 判断基础 slug 是否重复
     *
     * @param slug      slug
     * @param excludeId 排除 ID
     * @return 是否存在
     */
    boolean existsSlug(@NotNull String slug, Long excludeId);

    /**
     * 判断 locale + slug 是否冲突
     *
     * @param locale           语言
     * @param slug             slug
     * @param excludeProductId 排除商品 ID
     * @return 是否存在
     */
    boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeProductId);

    /**
     * 查询商品规格
     *
     * @param productId       商品 ID
     * @param includeDisabled 是否包含禁用项
     * @return 规格列表
     */
    @NotNull
    List<ProductSpec> listSpecs(@NotNull Long productId, boolean includeDisabled);

    /**
     * 查询规格值
     *
     * @param productId       商品 ID
     * @param includeDisabled 是否包含禁用项
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listSpecValues(@NotNull Long productId, boolean includeDisabled);

    /**
     * 查询规格多语言
     *
     * @param specIds 规格 ID 集合
     * @return 规格 I18N 映射
     */
    @NotNull
    Map<Long, List<ProductSpecI18n>> mapSpecI18n(@NotNull Set<Long> specIds);

    /**
     * 查询规格值多语言
     *
     * @param valueIds 规格值 ID 集合
     * @return 规格值 I18N 映射
     */
    @NotNull
    Map<Long, List<ProductSpecValueI18n>> mapSpecValueI18n(@NotNull Set<Long> valueIds);

    /**
     * 增量维护规格与规格值
     *
     * @param productId   商品 ID
     * @param commandList 规格命令列表
     */
    void upsertSpecs(@NotNull Long productId, @NotNull List<ProductSpecUpsertCommand> commandList);

    /**
     * 查询商品下所有 SKU
     *
     * @param productId 商品 ID
     * @return SKU 列表
     */
    @NotNull
    List<ProductSku> listSkus(@NotNull Long productId);

    /**
     * 查询 SKU 的最新可用价格
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 价格
     */
    @NotNull
    Map<Long, List<ProductPrice>> mapActivePrices(@NotNull Set<Long> skuIds);

    /**
     * 查询 SKU 图片
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 图片列表
     */
    @NotNull
    Map<Long, List<ProductImage>> mapSkuImages(@NotNull Set<Long> skuIds);

    /**
     * 查询 SKU 规格绑定
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 规格绑定列表
     */
    @NotNull
    Map<Long, List<ProductSkuSpec>> mapSkuSpecs(@NotNull Set<Long> skuIds);

    /**
     * 批量创建 SKU
     *
     * @param productId 商品 ID
     * @param commands  SKU 创建命令
     * @return 创建后的 SKU 列表
     */
    @NotNull
    List<ProductSku> createSkus(@NotNull Long productId, @NotNull List<ProductSkuUpsertItemCommand> commands);

    /**
     * 批量更新 SKU
     *
     * @param productId 商品 ID
     * @param commands  SKU 更新命令
     * @return 更新后的 SKU 列表
     */
    @NotNull
    List<ProductSku> updateSkus(@NotNull Long productId, @NotNull List<ProductSkuUpsertItemCommand> commands);

    /**
     * 更新或新增 SKU 价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param commands  价格命令列表
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPriceUpsertCommand> commands);

    /**
     * 调整 SKU 库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   库存调整命令
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku adjustStock(@NotNull Long productId, @NotNull Long skuId, @NotNull StockAdjustCommand command);

    /**
     * 计算商品的库存总量
     *
     * @param productId 商品 ID
     * @return 库存总量
     */
    int sumStock(@NotNull Long productId);

    /**
     * 更新商品库存与默认 SKU
     *
     * @param productId    商品 ID
     * @param stockTotal   库存总量
     * @param defaultSkuId 默认 SKU
     */
    void updateProductStockAndDefault(@NotNull Long productId, int stockTotal, Long defaultSkuId);

    /**
     * 判断 SKU 编码是否存在
     *
     * @param skuCode      SKU 编码
     * @param excludeSkuId 需排除的 SKU ID
     * @return true 表示存在
     */
    boolean existsSkuCode(@NotNull String skuCode, Long excludeSkuId);

    /**
     * 查询单个 SKU
     *
     * @param skuId SKU ID
     * @return SKU
     */
    @NotNull
    Optional<ProductSku> findSkuById(@NotNull Long skuId);

    /**
     * 增量更新 SKU 基础信息
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   基础信息命令
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku patchSku(@NotNull Long productId, @NotNull Long skuId, @NotNull ProductSkuPatchCommand command);

    /**
     * 检查规格是否被 SKU 绑定
     *
     * @param specId 规格 ID
     * @return true 表示存在绑定
     */
    boolean hasSkuBindingWithSpec(@NotNull Long specId);

    /**
     * 检查规格值是否被 SKU 绑定
     *
     * @param valueId 规格值 ID
     * @return true 表示存在绑定
     */
    boolean hasSkuBindingWithSpecValue(@NotNull Long valueId);

    /**
     * 删除规格及其多语言、取值
     *
     * @param specId 规格 ID
     */
    void deleteSpec(@NotNull Long specId);

    /**
     * 删除规格值及其多语言
     *
     * @param valueId 规格值 ID
     */
    void deleteSpecValue(@NotNull Long valueId);

    /**
     * 按规格查询规格值
     *
     * @param productId       商品 ID
     * @param specId          规格 ID
     * @param includeDisabled 是否包含禁用
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listSpecValues(@NotNull Long productId, @NotNull Long specId, boolean includeDisabled);

    /**
     * 查询规格
     *
     * @param specId 规格 ID
     * @return 规格
     */
    @NotNull
    Optional<ProductSpec> findSpecById(@NotNull Long specId);

    /**
     * 查询规格值
     *
     * @param valueId 规格值 ID
     * @return 规格值
     */
    @NotNull
    Optional<ProductSpecValue> findSpecValueById(@NotNull Long valueId);

    /**
     * 简单分页结果
     *
     * @param items 数据列表
     * @param total 总数
     * @param <T>   数据类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}

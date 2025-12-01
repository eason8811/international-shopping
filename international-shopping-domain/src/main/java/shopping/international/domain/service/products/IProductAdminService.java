package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.*;

import java.util.List;

/**
 * 商品管理服务
 *
 * <p>面向管理端的 SPU 查询与基础信息维护能力, 负责编排仓储与校验逻辑</p>
 */
public interface IProductAdminService {
    /**
     * 按条件分页筛选商品
     *
     * @param page           页码, 从 1 开始
     * @param size           每页数量, 最大 100
     * @param status         状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词, 可空
     * @param tag            标签, 可空
     * @param includeDeleted 是否包含已删除状态
     * @return 商品概要分页结果
     */
    @NotNull
    PageResult<ProductSummary> page(int page, int size, ProductStatus status, SkuType skuType, Long categoryId, String keyword, String tag, boolean includeDeleted);

    /**
     * 创建商品基础信息
     *
     * @param command 保存命令
     * @return 创建后的商品详情
     */
    @NotNull
    ProductDetail create(@NotNull ProductSaveCommand command);

    /**
     * 更新商品基础信息
     *
     * @param productId 商品 ID
     * @param command   保存命令
     * @return 更新后的商品详情
     */
    @NotNull
    ProductDetail update(@NotNull Long productId, @NotNull ProductSaveCommand command);

    /**
     * 查询商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    @NotNull
    ProductDetail detail(@NotNull Long productId);

    /**
     * 更新商品状态
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 更新后的详情
     */
    @NotNull
    ProductDetail updateStatus(@NotNull Long productId, @NotNull ProductStatus status);

    /**
     * 批量 upsert 商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     * @return 更新后的详情
     */
    @NotNull
    ProductDetail upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads);

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图片列表
     * @return 更新后的详情
     */
    @NotNull
    ProductDetail replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery);

    /**
     * 增量维护商品规格与规格值
     *
     * @param productId 商品 ID
     * @param specList     规格与规格值的维护命令列表
     * @return 更新后的商品详情
     */
    @NotNull
    ProductDetail upsertSpecs(@NotNull Long productId, @NotNull List<ProductSpecUpsertCommand> specList);

    /**
     * 批量创建 SKU
     *
     * @param productId 商品 ID
     * @param command   SKU 创建命令
     * @return 创建后的商品详情
     */
    @NotNull
    ProductDetail createSkus(@NotNull Long productId, @NotNull ProductSkuUpsertCommand command);

    /**
     * 批量更新 SKU
     *
     * @param productId 商品 ID
     * @param command   SKU 更新命令
     * @return 更新后的商品详情
     */
    @NotNull
    ProductDetail updateSkus(@NotNull Long productId, @NotNull ProductSkuUpsertCommand command);

    /**
     * 增量更新单个 SKU 基础信息
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   基础信息命令
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku patchSku(@NotNull Long productId, @NotNull Long skuId, @NotNull ProductSkuPatchCommand command);

    /**
     * 更新 SKU 价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param commands  价格维护命令列表
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku updateSkuPrice(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPriceUpsertCommand> commands);

    /**
     * 增量更新规格（不含规格值）
     *
     * @param productId 商品 ID
     * @param commands  规格命令列表
     * @return 受影响的规格 ID 列表
     */
    @NotNull
    List<Long> patchSpecs(@NotNull Long productId, @NotNull List<ProductSpecPatchCommand> commands);

    /**
     * 查询规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listSpecValues(@NotNull Long productId, @NotNull Long specId);

    /**
     * 删除规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     */
    void deleteSpec(@NotNull Long productId, @NotNull Long specId);

    /**
     * 删除规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param valueId   规格值 ID
     */
    void deleteSpecValue(@NotNull Long productId, @NotNull Long specId, @NotNull Long valueId);

    /**
     * 调整 SKU 库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   库存调整命令
     * @return 更新后的 SKU
     */
    @NotNull
    ProductSku adjustSkuStock(@NotNull Long productId, @NotNull Long skuId, @NotNull StockAdjustCommand command);

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

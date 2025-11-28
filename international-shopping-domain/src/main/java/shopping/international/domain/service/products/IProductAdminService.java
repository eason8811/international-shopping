package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
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
     * 简单分页结果
     *
     * @param items 数据列表
     * @param total 总数
     * @param <T>   数据类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}

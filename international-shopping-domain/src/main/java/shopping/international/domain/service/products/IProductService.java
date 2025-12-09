package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;

import java.util.List;

/**
 * 商品管理领域服务接口
 *
 * <p>覆盖管理侧商品的分页检索、基础信息维护、状态流转、多语言与图库维护等用例编排。</p>
 */
public interface IProductService {

    /**
     * 分页查询商品
     *
     * @param page           页码, 从 1 开始
     * @param size           页大小
     * @param status         状态过滤, 可空
     * @param skuType        规格类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词, 匹配标题/slug/品牌, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @return 分页结果
     */
    @NotNull
    PageResult page(int page, int size, @Nullable ProductStatus status, @Nullable SkuType skuType,
                    @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted);

    /**
     * 查询管理侧商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情读模型
     */
    @NotNull
    ProductDetail detail(@NotNull Long productId);

    /**
     * 创建商品基础信息
     *
     * @param slug          商品 slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类 ID
     * @param brand         品牌
     * @param coverImageUrl 主图
     * @param skuType       规格类型
     * @param status        状态
     * @param tags          标签
     * @return 新建商品聚合
     */
    @NotNull
    Product createBasic(@NotNull String slug, @NotNull String title, @Nullable String subtitle,
                        @Nullable String description, @NotNull Long categoryId, @Nullable String brand,
                        @Nullable String coverImageUrl, @NotNull SkuType skuType,
                        @NotNull ProductStatus status, @NotNull List<String> tags);

    /**
     * 更新商品基础信息
     *
     * @param productId     商品 ID
     * @param slug          新 slug, 可空
     * @param title         新标题, 可空
     * @param subtitle      新副标题, 可空
     * @param description   新描述, 可空
     * @param categoryId    新分类, 可空
     * @param brand         新品牌, 可空
     * @param coverImageUrl 新主图, 可空
     * @param skuType       新规格类型, 可空
     * @param status        新状态, 可空
     * @param tags          新标签, 可空
     * @return 更新后的商品聚合
     */
    @NotNull
    Product updateBasic(@NotNull Long productId, @Nullable String slug, @Nullable String title,
                        @Nullable String subtitle, @Nullable String description, @Nullable Long categoryId,
                        @Nullable String brand, @Nullable String coverImageUrl, @Nullable SkuType skuType,
                        @Nullable ProductStatus status, @Nullable List<String> tags);

    /**
     * 按合法状态机流转商品状态
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 更新后的状态
     */
    @NotNull
    ProductStatus changeStatus(@NotNull Long productId, @NotNull ProductStatus status);

    /**
     * 新增商品多语言
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
     * @return 新增后的多语言
     */
    @NotNull
    ProductI18n addI18n(@NotNull Long productId, @NotNull ProductI18n i18n);

    /**
     * 更新商品多语言
     *
     * @param productId  商品 ID
     * @param locale     语言代码
     * @param title      新标题, 可空
     * @param subtitle   新副标题, 可空
     * @param description 新描述, 可空
     * @param slug       新 slug, 可空
     * @param tags       新标签, 可空
     * @return 更新后的多语言
     */
    @NotNull
    ProductI18n updateI18n(@NotNull Long productId, @NotNull String locale, @Nullable String title,
                           @Nullable String subtitle, @Nullable String description, @Nullable String slug,
                           @Nullable List<String> tags);

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   新图库
     * @return 新图库数量
     */
    int replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery);

    /**
     * 商品详情读模型
     *
     * @param product      商品聚合
     * @param categorySlug 分类 slug
     * @param skus         SKU 列表
     */
    record ProductDetail(@NotNull Product product, @Nullable String categorySlug, @NotNull List<Sku> skus) {
    }

    /**
     * 简单分页结果
     *
     * @param items 当前页元素
     * @param total 总数
     */
    record PageResult(@NotNull List<Product> items, long total) {
    }
}

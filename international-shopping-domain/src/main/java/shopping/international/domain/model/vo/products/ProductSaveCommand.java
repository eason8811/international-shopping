package shopping.international.domain.model.vo.products;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.util.List;

/**
 * 商品基础信息保存命令 (ProductSaveCommand)
 *
 * <p>承载管理端创建或更新 SPU 时的基础字段, 由触发层请求转换而来</p>
 */
@Getter
@ToString
@AllArgsConstructor
public class ProductSaveCommand {
    /**
     * 商品 slug
     */
    private final String slug;
    /**
     * 商品标题
     */
    private final String title;
    /**
     * 商品副标题
     */
    @Nullable
    private final String subtitle;
    /**
     * 商品描述
     */
    @Nullable
    private final String description;
    /**
     * 分类 ID
     */
    private final Long categoryId;
    /**
     * 品牌文案
     */
    @Nullable
    private final String brand;
    /**
     * 主图 URL
     */
    @Nullable
    private final String coverImageUrl;
    /**
     * SKU 类型
     */
    private final SkuType skuType;
    /**
     * 商品状态
     */
    private final ProductStatus status;
    /**
     * 标签列表
     */
    @Nullable
    private final List<String> tags;
}

package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品基础信息保存请求 (ProductSaveRequest)
 *
 * <p>用于管理端创建或更新 SPU 的基础字段, 不包含图库、规格与 SKU</p>
 */
@Data
public class ProductSaveRequest {
    /**
     * 商品别名 (SEO/路由), 长度不超过 120
     */
    private String slug;
    /**
     * 商品标题, 长度不超过 255
     */
    private String title;
    /**
     * 商品副标题, 长度不超过 255, 可选
     */
    @Nullable
    private String subtitle;
    /**
     * 商品描述, 可选
     */
    @Nullable
    private String description;
    /**
     * 分类 ID, 必填
     */
    private Long categoryId;
    /**
     * 品牌文案, 长度不超过 120, 可选
     */
    @Nullable
    private String brand;
    /**
     * 主图 URL, 长度不超过 500, 可选
     */
    @Nullable
    private String coverImageUrl;
    /**
     * SKU 类型, 必填, 默认 {@link SkuType#SINGLE}
     */
    @Nullable
    private SkuType skuType;
    /**
     * 商品状态, 可选, 默认 {@link ProductStatus#DRAFT}
     */
    @Nullable
    private ProductStatus status;
    /**
     * 标签列表, 可选
     */
    @Nullable
    private List<String> tags;

    /**
     * 请求校验与规范化
     *
     * @throws IllegalParamException 当必填字段缺失或长度超限时抛出
     */
    public void validate() {
        requireNotBlank(slug, "商品 slug 不能为空");
        slug = slug.strip();
        if (slug.length() > 120)
            throw new IllegalParamException("商品 slug 长度不能超过 120 个字符");

        requireNotBlank(title, "商品标题不能为空");
        title = title.strip();
        if (title.length() > 255)
            throw new IllegalParamException("商品标题长度不能超过 255 个字符");

        if (subtitle != null) {
            subtitle = subtitle.strip();
            if (subtitle.length() > 255)
                throw new IllegalParamException("商品副标题长度不能超过 255 个字符");
        }
        if (description != null)
            description = description.strip();

        requireNotNull(categoryId, "分类 ID 不能为空");

        if (brand != null) {
            brand = brand.strip();
            if (brand.length() > 120)
                throw new IllegalParamException("品牌文案长度不能超过 120 个字符");
        }

        if (coverImageUrl != null) {
            coverImageUrl = coverImageUrl.strip();
            if (coverImageUrl.length() > 500)
                throw new IllegalParamException("主图 URL 长度不能超过 500 个字符");
        }

        if (tags == null)
            return;
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null)
                continue;
            String trimmed = tag.strip();
            if (trimmed.isEmpty())
                continue;
            if (trimmed.length() > 120)
                throw new IllegalParamException("标签长度不能超过 120 个字符");
            if (seen.add(trimmed))
                normalized.add(trimmed);
        }
        tags = normalized;
    }
}

package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 商品基础信息保存请求 (ProductSpuBasicUpsertRequest)
 *
 * <p>用于管理端创建或更新 SPU 的基础字段, 不包含图库、规格与 SKU</p>
 */
@Data
public class ProductSpuBasicUpsertRequest implements Verifiable {
    /**
     * 商品别名 (SEO/路由), 长度不超过 120
     */
    @Nullable
    private String slug;
    /**
     * 商品标题, 长度不超过 255
     */
    @Nullable
    private String title;
    /**
     * 商品副标题, 长度不超过 255
     */
    @Nullable
    private String subtitle;
    /**
     * 商品描述
     */
    @Nullable
    private String description;
    /**
     * 分类 ID
     */
    @Nullable
    private Long categoryId;
    /**
     * 品牌文案, 长度不超过 120
     */
    @Nullable
    private String brand;
    /**
     * 主图 URL, 长度不超过 500
     */
    @Nullable
    private String coverImageUrl;
    /**
     * SKU 类型, 默认 {@link SkuType#SINGLE}
     */
    @Nullable
    private SkuType skuType;
    /**
     * 商品状态, 默认 {@link ProductStatus#DRAFT}
     */
    @Nullable
    private ProductStatus status;
    /**
     * 标签列表
     */
    @Nullable
    private List<String> tags;

    /**
     * 验证当前对象是否符合预定义的规则或条件
     *
     * <p>此方法用于确保对象的状态或属性满足特定的要求, 如果验证失败, 则可能抛出异常来指示问题所在</p>
     *
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    @Override
    public void validate() {
        subtitle = normalizeNullableField(subtitle, "subtitle 不能为空", subtitle -> subtitle.length() <= 255, "商品副标题长度不能超过 255 个字符");
        description = normalizeNullableField(description, "description 不能为空", d -> true, null);
        brand = normalizeNullableField(brand, "brand 不能为空", brand -> brand.length() <= 120, "品牌文案长度不能超过 120 个字符");
        coverImageUrl = normalizeNullableField(coverImageUrl, "coverImageUrl 不能为空", coverImageUrl -> coverImageUrl.length() <= 500, "主图 URL 长度不能超过 500 个字符");
    }

    /**
     * 创建商品时的字段校验与规范化
     *
     * <p>此方法会执行以下操作:</p>
     * <ul>
     *     <li>规范化 {@code slug} 字段, 确保其不为空且长度不超过 120 个字符</li>
     *     <li>规范化 {@code title} 字段, 确保其不为空且长度不超过 255 个字符</li>
     *     <li>确保 {@code categoryId} 字段不为空</li>
     *     <li>调用 {@link #validate()} 方法验证当前对象是否符合预定义的规则或条件</li>
     *     <li>调用 {@link #normalizeTags()} 方法规范化标签列表, 确保每个标签都是唯一的, 并且去除首尾空白</li>
     * </ul>
     *
     * @throws IllegalParamException 当必填字段缺失或长度超限时抛出
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    public void createValidate() {
        slug = normalizeNotNullField(slug, "商品 slug 不能为空", slug -> slug.length() <= 120, "商品 slug 长度不能超过 120 个字符");
        title = normalizeNotNullField(title, "商品标题不能为空", title -> title.length() <= 255, "商品标题长度不能超过 255 个字符");
        requireNotNull(categoryId, "分类 ID 不能为空");
        requireNotNull(skuType, "skuType 不能为空");
        requireNotNull(status, "status 不能为空");
        validate();
        normalizeTags();
    }

    /**
     * 更新商品基本信息时的字段校验与规范化
     *
     * <p>此方法会执行以下操作:</p>
     * <ul>
     *     <li>规范化 {@code slug} 字段, 确保其不为空且长度不超过 120 个字符</li>
     *     <li>规范化 {@code title} 字段, 确保其不为空且长度不超过 255 个字符</li>
     *     <li>调用 {@link #validate()} 方法验证当前对象是否符合预定义的规则或条件</li>
     *     <li>调用 {@link #normalizeTags()} 方法规范化标签列表, 确保每个标签都是唯一的, 并且去除首尾空白</li>
     * </ul>
     *
     * @throws IllegalParamException 当必填字段缺失或长度超限时抛出
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    public void updateValidate() {
        slug = normalizeNullableField(slug, "slug 不能为空", slug -> slug.length() <= 120, "商品 slug 长度不能超过 120 个字符");
        title = normalizeNullableField(title, "title 不能为空", title -> title.length() <= 255, "商品标题长度不能超过 255 个字符");
        validate();
        normalizeTags();
    }

    /**
     * 规范化标签列表, 确保每个标签都是唯一的, 并且去除首尾空白
     *
     * <p>此方法会对 {@code tags} 字段中的每一个标签执行以下操作:</p>
     * <ul>
     *     <li>如果标签为 null 或者空字符串(去除首尾空白后), 则跳过该标签</li>
     *     <li>检查标签长度是否超过 120 个字符, 如果超过则抛出异常</li>
     *     <li>确保所有标签唯一, 重复的标签将被忽略</li>
     * </ul>
     *
     * @throws IllegalParamException 当存在长度超过 120 个字符的标签时抛出
     */
    private void normalizeTags() {
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

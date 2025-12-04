package shopping.international.api.req.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 分类创建或更新请求体 ( CategoryUpsertRequest )
 *
 * <p>管理端用于新增或修改商品分类的基础信息以及可选的多语言覆盖</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpsertRequest implements Verifiable {
    /**
     * 分类名称
     */
    @Nullable
    private String name;
    /**
     * 路由 slug
     */
    @Nullable
    private String slug;
    /**
     * 父级分类 ID ( 根为 null )
     */
    @Nullable
    private Long parentId;
    /**
     * 排序权重
     */
    @Nullable
    private Integer sortOrder;
    /**
     * 是否启用 ( 默认 true )
     */
    @Nullable
    private Boolean isEnabled;
    /**
     * 多语言覆盖列表 ( 可空 )
     */
    @Nullable
    private List<CategoryI18nPayload> i18n;

    /**
     * 验证并规范化当前对象的字段
     *
     * <p>此方法用于确保 {@code sortOrder} 字段存在一个默认值 0, 如果其为空则设置为 0;
     * 同时确保 {@code isEnabled} 字段为布尔值, 如果其为空则设置为 true</p>
     */
    public void validate() {
        sortOrder = sortOrder == null ? 0 : sortOrder;
        isEnabled = isEnabled == null || isEnabled;
    }

    /**
     * 调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void createValidate() {
        validate();
        name = normalizeNotNullField(name, "分类名称不能为空", s -> s.length() <= 64, "分类名称长度不能超过 64 个字符");
        slug = normalizeNotNullField(slug, "分类 slug 不能为空", s -> s.length() <= 64, "分类 slug 长度不能超过 64 个字符");
        i18n = normalizeDistinctList(i18n, CategoryI18nPayload::createValidate, CategoryI18nPayload::getLocale, "重复的多语言 locale");
    }

    /**
     * 调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void updateValidate() {
        validate();
        name = normalizeNullableField(name, "分类名称不能为空", s -> s.length() <= 64, "分类名称长度不能超过 64 个字符");
        slug = normalizeNullableField(slug, "分类 slug 不能为空", s -> s.length() <= 64, "分类 slug 长度不能超过 64 个字符");
        i18n = normalizeDistinctList(i18n, CategoryI18nPayload::updateValidate, CategoryI18nPayload::getLocale, "重复的多语言 locale");
    }
}

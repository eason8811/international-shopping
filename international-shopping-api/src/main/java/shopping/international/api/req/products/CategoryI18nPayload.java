package shopping.international.api.req.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 分类多语言请求体 ( CategoryI18nPayload )
 *
 * <p>用于在管理侧为商品分类追加或更新指定语言的名称, 路由 slug 以及品牌文案</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryI18nPayload implements Verifiable {
    /**
     * 语言代码 ( 如 en-US )
     */
    private String locale;
    /**
     * 分类名称
     */
    @Nullable
    private String name;
    /**
     * 分类别名
     */
    @Nullable
    private String slug;
    /**
     * 品牌文案 ( 可空 )
     */
    @Nullable
    private String brand;

    /**
     * 验证当前对象是否符合预定义的规则或条件
     *
     * <p>此方法主要用于验证语言代码的有效性, 确保其不为空且格式正确</p>
     *
     * @throws IllegalParamException 如果语言代码为空或格式不正确, 则抛出该异常
     */
    public void validate() {
        locale = normalizeNotNullField(locale, "规格多语言的 locale 不能为空", l -> l.length() <= 16, "locale 长度不能超过 16 个字符");
        locale = normalizeLocale(locale);
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void createValidate() {
        validate();
        name = normalizeNotNullField(name, "分类名称不能为空", s -> s.length() <= 64, "分类名称长度不能超过 64 个字符");
        slug = normalizeNotNullField(slug, "分类 slug 不能为空", s -> s.length() <= 64, "分类 slug 长度不能超过 64 个字符");
        brand = normalizeNullableField(brand, "品牌文案不能为空", s -> s.length() <= 120, "品牌文案长度不能超过 120 个字符");
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void updateValidate() {
        validate();
        name = normalizeNullableField(name, "分类名称不能为空", s -> s.length() <= 64, "分类名称长度不能超过 64 个字符");
        slug = normalizeNullableField(slug, "分类 slug 不能为空", s -> s.length() <= 64, "分类 slug 长度不能超过 64 个字符");
        brand = normalizeNullableField(brand, "品牌文案不能为空", s -> s.length() <= 120, "品牌文案长度不能超过 120 个字符");
    }
}

package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 分类多语言值对象, 对应表 {@code product_category_i18n}.
 *
 * <p>无独立生命周期, 通过 locale 唯一定位。</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "locale")
public class CategoryI18n implements Verifiable {
    /**
     * 语言代码 (如 zh-CN / en-US)
     */
    private final String locale;
    /**
     * 本地化名称
     */
    private final String name;
    /**
     * 本地化 slug
     */
    private final String slug;
    /**
     * 本地化品牌文案
     */
    private final String brand;

    /**
     * 构造函数
     *
     * @param locale 语言代码
     * @param name   本地化名称
     * @param slug   本地化 slug
     * @param brand  品牌文案
     */
    private CategoryI18n(String locale, String name, String slug, String brand) {
        this.locale = locale;
        this.name = name;
        this.slug = slug;
        this.brand = brand;
    }

    /**
     * 创建分类多语言值对象
     *
     * @param locale 语言代码, 必填
     * @param name   本地化名称, 必填
     * @param slug   本地化 slug, 必填
     * @param brand  本地化品牌文案, 可空
     * @return 规范化后的 {@link CategoryI18n}
     */
    public static CategoryI18n of(String locale, String name, String slug, String brand) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
        return new CategoryI18n(normalizedLocale, name.strip(), slug.strip(), brand == null ? null : brand.strip());
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotNull(locale, "locale 不能为空");
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
    }
}

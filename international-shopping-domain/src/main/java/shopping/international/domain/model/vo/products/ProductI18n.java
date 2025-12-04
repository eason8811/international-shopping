package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 商品 SPU 多语言值对象, 对应表 {@code product_i18n}.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "locale")
public class ProductI18n implements Verifiable {
    /**
     * 语言代码
     */
    private final String locale;
    /**
     * 本地化标题
     */
    private final String title;
    /**
     * 本地化副标题
     */
    private final String subtitle;
    /**
     * 本地化描述
     */
    private final String description;
    /**
     * 本地化 slug
     */
    private final String slug;
    /**
     * 本地化标签列表
     */
    private final List<String> tags;

    /**
     * 构造函数
     *
     * @param locale      语言代码
     * @param title       标题
     * @param subtitle    副标题
     * @param description 描述
     * @param slug        slug
     * @param tags        标签
     */
    private ProductI18n(String locale, String title, String subtitle, String description, String slug, List<String> tags) {
        this.locale = locale;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.slug = slug;
        this.tags = tags;
    }

    /**
     * 创建多语言值对象
     *
     * @param locale      语言代码, 必填
     * @param title       标题, 必填
     * @param subtitle    副标题, 可空
     * @param description 描述, 可空
     * @param slug        slug, 必填
     * @param tags        标签列表, 可空
     * @return 规范化后的 {@link ProductI18n}
     */
    public static ProductI18n of(String locale, String title, String subtitle, String description, String slug, List<String> tags) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        requireNotBlank(title, "多语言标题不能为空");
        requireNotBlank(slug, "多语言 slug 不能为空");
        return new ProductI18n(normalizedLocale, title.strip(),
                subtitle == null ? null : subtitle.strip(),
                description == null ? null : description.strip(),
                slug.strip(), normalizeTags(tags));
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotNull(locale, "locale 不能为空");
        requireNotBlank(title, "多语言标题不能为空");
        requireNotBlank(slug, "多语言 slug 不能为空");
    }
}

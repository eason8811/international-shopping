package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品多语言覆盖
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"locale", "slug"})
public class ProductI18n {
    /**
     * 语言
     */
    private final String locale;
    /**
     * 标题
     */
    private final String title;
    /**
     * 副标题
     */
    private final String subtitle;
    /**
     * 描述
     */
    private final String description;
    /**
     * slug
     */
    private final String slug;
    /**
     * 标签
     */
    private final List<String> tags;

    private ProductI18n(String locale, String title, String subtitle, String description, String slug, List<String> tags) {
        this.locale = locale;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.slug = slug;
        this.tags = tags == null ? Collections.emptyList() : new ArrayList<>(tags);
    }

    /**
     * 构建多语言覆盖
     *
     * @param locale      语言
     * @param title       标题
     * @param subtitle    副标题
     * @param description 描述
     * @param slug        slug
     * @param tags        标签
     * @return i18n 对象
     */
    public static ProductI18n of(@NotNull String locale,
                                 @NotNull String title,
                                 String subtitle,
                                 String description,
                                 @NotNull String slug,
                                 List<String> tags) {
        requireNotBlank(locale, "locale 不能为空");
        requireNotBlank(title, "title 不能为空");
        requireNotBlank(slug, "slug 不能为空");
        return new ProductI18n(locale, title, subtitle, description, slug, tags);
    }
}

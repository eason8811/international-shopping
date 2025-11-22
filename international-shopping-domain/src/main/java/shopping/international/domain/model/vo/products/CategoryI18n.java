package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品分类的本地化覆盖
 */
@Getter
@ToString
@EqualsAndHashCode
public class CategoryI18n {
    /**
     * 语言代码，如 en-US
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
     * 品牌文案 (可空)
     */
    private final String brand;

    /**
     * 私有构造函数, 构造一个新的 CategoryI18n 实例, 用于存储商品分类的本地化信息
     *
     * @param locale 语言代码, 如 en-US
     * @param name   本地化名称
     * @param slug   本地化路由 slug
     * @param brand  品牌文案 (可空)
     */
    private CategoryI18n(String locale, String name, String slug, String brand) {
        this.locale = locale;
        this.name = name;
        this.slug = slug;
        this.brand = brand;
    }

    /**
     * 创建一个本地化覆盖
     *
     * @param locale 语言代码
     * @param name   名称
     * @param slug   路由 slug
     * @param brand  品牌文案
     * @return 值对象
     */
    public static CategoryI18n of(@NotNull String locale, @NotNull String name, @NotNull String slug, String brand) {
        requireNotBlank(locale, "locale 不能为空");
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
        return new CategoryI18n(locale, name, slug, brand);
    }
}

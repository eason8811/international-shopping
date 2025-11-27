package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 分类多语言请求体 ( CategoryI18nPayload )
 *
 * <p>用于在管理侧为商品分类追加或更新指定语言的名称, 路由 slug 以及品牌文案</p>
 */
@Data
public class CategoryI18nPayload {
    /**
     * 语言代码 ( 如 en-US )
     */
    private String locale;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 分类别名
     */
    private String slug;
    /**
     * 品牌文案 ( 可空 )
     */
    private String brand;

    /**
     * 入参校验并去除首尾空白
     *
     * @throws IllegalParamException 当必填字段为空时抛出
     */
    public void validate() {
        requireNotBlank(locale, "语言代码不能为空");
        locale = locale.strip();
        if (locale.length() > 16)
            throw new IllegalParamException("语言代码长度不能超过 16 个字符");
        requireNotBlank(name, "分类名称不能为空");
        name = name.strip();
        requireNotBlank(slug, "分类 slug 不能为空");
        slug = slug.strip();
        if (brand != null)
            brand = brand.strip();
    }
}

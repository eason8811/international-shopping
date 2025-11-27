package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 分类创建或更新请求体 ( CategoryUpsertRequest )
 *
 * <p>管理端用于新增或修改商品分类的基础信息以及可选的多语言覆盖</p>
 */
@Data
public class CategoryUpsertRequest {
    /**
     * 分类名称
     */
    private String name;
    /**
     * 路由 slug
     */
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
     * 默认品牌文案 ( 可空 )
     */
    @Nullable
    private String brand;
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
     * 入参规范化与校验
     *
     * @throws IllegalParamException 当必填字段缺失或重复 locale 时抛出
     */
    public void validate() {
        requireNotBlank(name, "分类名称不能为空");
        name = name.strip();
        requireNotBlank(slug, "分类 slug 不能为空");
        slug = slug.strip();
        if (sortOrder == null)
            sortOrder = 0;
        if (brand != null)
            brand = brand.strip();

        if (i18n == null)
            return;
        List<CategoryI18nPayload> normalized = new ArrayList<>();
        Set<String> locales = new LinkedHashSet<>();
        for (CategoryI18nPayload payload : i18n) {
            if (payload == null)
                continue;
            payload.validate();
            if (!locales.add(payload.getLocale()))
                throw new IllegalParamException("重复的多语言 locale");
            normalized.add(payload);
        }
        this.i18n = normalized;
    }
}

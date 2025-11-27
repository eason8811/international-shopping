package shopping.international.domain.model.vo.products;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 分类保存命令 ( CategoryUpsertCommand )
 *
 * <p>封装管理端新增或更新分类时需要的基础信息与多语言覆盖</p>
 */
@Getter
public class CategoryUpsertCommand {
    /**
     * 分类名称
     */
    private final String name;
    /**
     * 分类 slug
     */
    private final String slug;
    /**
     * 父级分类 ID ( 根为 null )
     */
    @Nullable
    private final Long parentId;
    /**
     * 排序值
     */
    private final Integer sortOrder;
    /**
     * 默认品牌文案 ( 可空 )
     */
    @Nullable
    private final String brand;
    /**
     * 是否启用
     */
    @Nullable
    private final Boolean isEnabled;
    /**
     * 多语言列表 ( 可空 )
     */
    @Nullable
    private final List<CategoryI18n> i18nList;

    /**
     * 构造分类保存命令
     *
     * @param name       分类名称
     * @param slug       分类 slug
     * @param parentId   父级 ID
     * @param sortOrder  排序值
     * @param brand      品牌文案
     * @param isEnabled  是否启用
     * @param i18nList   多语言覆盖列表
     */
    public CategoryUpsertCommand(String name, String slug, @Nullable Long parentId, Integer sortOrder, @Nullable String brand,
                                 @Nullable Boolean isEnabled, List<CategoryI18n> i18nList) {
        this.name = name;
        this.slug = slug;
        this.parentId = parentId;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.brand = brand;
        this.isEnabled = isEnabled;
        this.i18nList = i18nList == null ? null : Collections.unmodifiableList(i18nList);
    }
}

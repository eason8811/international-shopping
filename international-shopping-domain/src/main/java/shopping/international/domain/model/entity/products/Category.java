package shopping.international.domain.model.entity.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品分类实体, 对应 product_category
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Category {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 父分类ID (根为 null)
     */
    private Long parentId;
    /**
     * 名称
     */
    private String name;
    /**
     * 路由 slug
     */
    private String slug;
    /**
     * 层级
     */
    private int level;
    /**
     * 祖先路径, 形如 /1/3/
     */
    private String path;
    /**
     * 排序
     */
    private int sortOrder;
    /**
     * 状态
     */
    private CategoryStatus status;

    private Category() {
    }

    /**
     * 从持久层数据重建分类实体
     *
     * @param id        主键
     * @param parentId  父ID
     * @param name      名称
     * @param slug      路由 slug
     * @param level     层级, 从 1 开始
     * @param path      路径
     * @param sortOrder 排序
     * @param status    状态
     * @return 分类实体
     */
    public static Category reconstitute(Long id, Long parentId, @NotNull String name, @NotNull String slug, int level,
                                        String path, int sortOrder, CategoryStatus status) {
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
        if (level <= 0)
            throw new IllegalParamException("分类层级必须大于 0");

        Category category = new Category();
        category.id = id;
        category.parentId = parentId;
        category.name = name;
        category.slug = slug;
        category.level = level;
        category.path = path;
        category.sortOrder = sortOrder;
        category.status = status == null ? CategoryStatus.DISABLED : status;
        return category;
    }

    /**
     * 是否启用
     *
     * @return true 为启用
     */
    public boolean isEnabled() {
        return status == CategoryStatus.ENABLED;
    }
}

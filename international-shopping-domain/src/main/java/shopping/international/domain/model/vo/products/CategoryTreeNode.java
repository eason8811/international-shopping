package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 用户侧分类树节点 CategoryTreeNode
 *
 * <p>名称、slug、brand 已按 locale 做过本地化覆盖</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class CategoryTreeNode {
    /**
     * 分类 ID
     */
    private final Long id;
    /**
     * 父级分类 ID
     */
    private final Long parentId;
    /**
     * 分类名称
     */
    private final String name;
    /**
     * 分类 slug
     */
    private final String slug;
    /**
     * 分类层级
     */
    private final int level;
    /**
     * 分类路径
     */
    private final String path;
    /**
     * 排序权重
     */
    private final int sortOrder;
    /**
     * 品牌文案
     */
    private final String brand;
    /**
     * 当前语言
     */
    private final String locale;
    /**
     * 分类状态
     */
    private final CategoryStatus status;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;
    /**
     * 子节点
     */
    private final List<CategoryTreeNode> children = new ArrayList<>();

    private CategoryTreeNode(Long id,
                             Long parentId,
                             String name,
                             String slug,
                             int level,
                             String path,
                             int sortOrder,
                             String brand,
                             String locale,
                             CategoryStatus status,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.slug = slug;
        this.level = level;
        this.path = path;
        this.sortOrder = sortOrder;
        this.brand = brand;
        this.locale = locale;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 从分类实体和本地化覆盖构建分类树节点
     *
     * @param category 分类实体
     * @param override 本地化覆盖
     * @return 分类树节点
     */
    public static CategoryTreeNode from(Category category, CategoryI18n override) {
        String displayName = category.getName();
        String displaySlug = category.getSlug();
        String displayBrand = null;
        String displayLocale = null;
        if (override != null) {
            displayName = override.getName();
            displaySlug = override.getSlug();
            displayBrand = override.getBrand();
            displayLocale = override.getLocale();
        }
        return new CategoryTreeNode(
                category.getId(),
                category.getParentId(),
                displayName,
                displaySlug,
                category.getLevel(),
                category.getPath(),
                category.getSortOrder(),
                displayBrand,
                displayLocale,
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    /**
     * 添加子节点
     *
     * @param child 子节点
     */
    public void addChild(CategoryTreeNode child) {
        if (child != null)
            this.children.add(child);
    }

    /**
     * 递归排序子节点
     *
     * @param comparator 排序规则
     */
    public void sortChildrenRecursively(Comparator<CategoryTreeNode> comparator) {
        this.children.sort(comparator);
        for (CategoryTreeNode child : children)
            child.sortChildrenRecursively(comparator);
    }
}

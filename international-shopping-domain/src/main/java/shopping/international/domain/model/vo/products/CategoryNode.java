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
import java.util.Optional;

/**
 * 分类树节点 (供读取树形结构使用)
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class CategoryNode {
    /**
     * 分类ID
     */
    private final Long id;
    /**
     * 父分类ID
     */
    private final Long parentId;
    /**
     * 分类名称
     */
    private final String name;
    /**
     * 分类别名
     */
    private final String slug;
    /**
     * 分类级别
     */
    private final int level;
    /**
     * 分类路径
     */
    private final String path;
    /**
     * 分类排序
     */
    private final int sortOrder;
    /**
     * 品牌名称
     */
    private final String brand;
    /**
     * 本地化
     */
    private final String locale;
    /**
     * 本地化
     */
    private final CategoryI18n i18n;
    /**
     * 本地化列表
     */
    private final List<CategoryI18n> i18nList;
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
     * 子节点列表
     */
    private final List<CategoryNode> children = new ArrayList<>();

    /**
     * 构造一个新的 CategoryNode 实例
     *
     * @param id        分类ID
     * @param parentId  父分类ID
     * @param name      分类名称
     * @param slug      分类别名
     * @param level     分类级别
     * @param path      分类路径
     * @param sortOrder 分类排序
     * @param brand     品牌名称
     * @param locale    本地化设置
     * @param i18n      分类的本地化信息
     * @param i18nList  分类的全部本地化信息
     * @param status    分类状态
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    private CategoryNode(Long id, Long parentId, String name, String slug, int level, String path, int sortOrder,
                         String brand, String locale, CategoryI18n i18n, List<CategoryI18n> i18nList,
                         CategoryStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.slug = slug;
        this.level = level;
        this.path = path;
        this.sortOrder = sortOrder;
        this.brand = brand;
        this.locale = locale;
        this.i18n = i18n;
        this.i18nList = i18nList == null ? List.of() : i18nList;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 将分类实体转换为树节点, 按照传入的本地化覆盖决定展示字段
     *
     * @param category 分类实体
     * @param override 本地化覆盖, 可空
     * @return 树节点
     */
    public static CategoryNode from(Category category, CategoryI18n override) {
        return from(category, override, null);
    }

    /**
     * 将分类实体转换为树节点, 按照传入的本地化覆盖决定展示字段, 并填充完整 i18n 列表
     *
     * @param category 分类实体
     * @param override 本地化覆盖, 可空
     * @param i18nList 全量本地化列表, 可空
     * @return 树节点
     */
    public static CategoryNode from(Category category, CategoryI18n override, List<CategoryI18n> i18nList) {
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

        return new CategoryNode(
                category.getId(),
                category.getParentId(),
                displayName,
                displaySlug,
                category.getLevel(),
                category.getPath(),
                category.getSortOrder(),
                displayBrand,
                displayLocale,
                override,
                Optional.ofNullable(i18nList).orElseGet(List::of),
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
    public void addChild(CategoryNode child) {
        if (child != null)
            this.children.add(child);
    }

    /**
     * 递归排序子节点
     *
     * @param comparator 排序规则
     */
    public void sortChildrenRecursively(Comparator<CategoryNode> comparator) {
        this.children.sort(comparator);
        for (CategoryNode child : children)
            child.sortChildrenRecursively(comparator);
    }
}

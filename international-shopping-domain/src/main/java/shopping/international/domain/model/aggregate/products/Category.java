package shopping.international.domain.model.aggregate.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 分类聚合根, 对应 product_category 主表
 *
 * <p>职责: 保证分类基础信息完整性 (name/slug/level/path), 维护启停状态与排序, 管理多语言覆盖</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class Category implements Verifiable {
    /**
     * 主键 ID (可空表示未持久化)
     */
    private Long id;
    /**
     * 父分类 ID, 根分类为空
     */
    private Long parentId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 分类 slug, 用于路由/SEO
     */
    private String slug;
    /**
     * 分类层级 (根从 1 开始)
     */
    private int level;
    /**
     * 祖先路径 (如 /1/3/), 根为空
     */
    private String path;
    /**
     * 排序值 (小在前)
     */
    private int sortOrder;
    /**
     * 启用状态
     */
    private CategoryStatus status;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 多语言覆盖列表, locale 唯一
     */
    private List<CategoryI18n> i18nList;
    /**
     * 创建时间快照
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间快照
     */
    private LocalDateTime updatedAt;

    /**
     * 私有构造函数
     *
     * @param id        分类 ID
     * @param parentId  父分类 ID
     * @param name      分类名称
     * @param slug      分类 slug
     * @param level     分类层级
     * @param path      分类路径
     * @param sortOrder 排序值
     * @param status    启用状态
     * @param brand     品牌文案
     * @param i18nList  多语言列表
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    private Category(Long id, Long parentId, String name, String slug, int level, String path, int sortOrder,
                     CategoryStatus status, String brand, List<CategoryI18n> i18nList,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
        require(level > 0, "分类层级必须大于 0");
        this.id = id;
        this.parentId = parentId;
        this.name = name.strip();
        this.slug = slug.strip();
        this.level = level;
        this.path = path;
        this.sortOrder = sortOrder;
        this.status = status == null ? CategoryStatus.ENABLED : status;
        this.brand = brand == null ? null : brand.strip();
        this.i18nList = normalizeDistinctList(i18nList, CategoryI18n::validate, CategoryI18n::getLocale, "分类多语言 locale 不能重复");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的分类 (默认启用)
     *
     * @param parentId  父分类 ID, 根分类可为 null
     * @param level     分类层级, 根为 1
     * @param name      分类名称, 不可为空
     * @param slug      分类 slug, 不可为空
     * @param sortOrder 排序值, 默认 0
     * @param brand     品牌文案, 可空
     * @param i18nList  多语言列表, locale 唯一
     * @return 新建的分类聚合根, id 为 null 表示未持久化
     */
    public static Category create(Long parentId, int level, String name, String slug,
                                  int sortOrder, String brand, List<CategoryI18n> i18nList) {
        return new Category(null, parentId, name, slug, level, null, sortOrder, CategoryStatus.ENABLED,
                brand, i18nList, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 从持久化层重建分类
     *
     * @param id        分类 ID
     * @param parentId  父分类 ID
     * @param name      分类名称
     * @param slug      分类 slug
     * @param level     分类层级
     * @param path      分类路径
     * @param sortOrder 排序值
     * @param status    启用状态
     * @param brand     品牌文案
     * @param i18nList  多语言列表
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 重建后的分类聚合根
     */
    public static Category reconstitute(Long id, Long parentId, String name, String slug, int level, String path,
                                        int sortOrder, CategoryStatus status, String brand,
                                        List<CategoryI18n> i18nList, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Category(id, parentId, name, slug, level, path, sortOrder, status, brand, i18nList, createdAt, updatedAt);
    }

    /**
     * 更新分类基本信息 (名称/slug/品牌)
     *
     * @param name  新分类名称, 为空则忽略
     * @param slug  新 slug, 为空则忽略
     * @param brand 新品牌文案, 为空则忽略
     */
    public void updateBasic(String name, String slug, String brand) {
        if (name != null) {
            requireNotBlank(name, "分类名称不能为空");
            this.name = name.strip();
        }
        if (slug != null) {
            requireNotBlank(slug, "分类 slug 不能为空");
            this.slug = slug.strip();
        }
        if (brand != null)
            this.brand = brand.strip();
    }

    /**
     * 更新排序值
     *
     * @param sortOrder 新排序, 可为负数以提前排序
     */
    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 更新启用状态
     *
     * @param status 新状态, 不可为 null
     */
    public void changeStatus(CategoryStatus status) {
        requireNotNull(status, "分类状态不能为空");
        this.status = status;
    }

    /**
     * 调整分类的层级与父子关系
     *
     * @param newParentId 新父分类 ID, 根分类为 null
     * @param newLevel    新层级, 必须大于 0
     * @param newPath     新路径 (可空)
     */
    public void moveTo(Long newParentId, int newLevel, String newPath) {
        require(newLevel > 0, "分类层级必须大于 0");
        this.parentId = newParentId;
        this.level = newLevel;
        this.path = newPath;
    }

    /**
     * 替换多语言列表
     *
     * @param i18nList 新多语言列表, locale 需唯一
     */
    public void replaceI18n(List<CategoryI18n> i18nList) {
        this.i18nList = normalizeDistinctList(i18nList, CategoryI18n::validate, CategoryI18n::getLocale, "分类多语言 locale 不能重复");
    }

    /**
     * 为分类分配 ID (幂等)
     *
     * @param id 新 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "分类 ID 不能为空");
        if (this.id != null && !this.id.equals(id))
            throw new IllegalStateException("分类已存在 ID, 不允许覆盖, current=" + this.id + ", new=" + id);
        this.id = id;
    }

    /**
     * 校验分类聚合根
     */
    @Override
    public void validate() {
        requireNotBlank(name, "分类名称不能为空");
        requireNotBlank(slug, "分类 slug 不能为空");
        require(level > 0, "分类层级必须大于 0");
        requireNotNull(status, "分类状态不能为空");
    }
}

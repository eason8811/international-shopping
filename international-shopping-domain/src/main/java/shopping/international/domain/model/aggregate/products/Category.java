package shopping.international.domain.model.aggregate.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @NotNull
    private List<CategoryI18n> i18nList;
    /**
     * 创建时间快照
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间快照
     */
    private final LocalDateTime updatedAt;

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
     * 新增多语言条目 (locale 不可重复, name/slug 必填)
     *
     * @param i18n 新增多语言
     */
    public void addI18n(CategoryI18n i18n) {
        requireNotNull(i18n, "分类多语言不能为空");
        i18n.validate();
        List<CategoryI18n> mutable = new ArrayList<>(i18nList);
        boolean exists = mutable.stream().anyMatch(item -> item.getLocale().equals(i18n.getLocale()));
        require(!exists, "分类多语言 locale 已存在: " + i18n.getLocale());
        mutable.add(i18n);
        this.i18nList = normalizeDistinctList(mutable, CategoryI18n::validate, CategoryI18n::getLocale, "分类多语言 locale 不能重复");
    }

    /**
     * 批量更新分类的多语言信息
     *
     * <p>此方法会根据传入的多语言列表, 更新当前分类下的多语言条目, 如果某个语言代码在当前分类中已存在,
     * 则会合并新的值与现有值, 如果新值为空, 则保留现有值; 如果不存在, 则忽略该条目</p>
     *
     * <p>对于每个多语言条目, 必须提供有效的 locale, 并且 name 和 slug 字段不能为空
     * 如果提供的 locale 不合法或过长, 将抛出异常</p>
     *
     * @param i18nList 待更新的多语言列表
     * @throws IllegalParamException 如果有无效的参数 (如空的 locale, 空的 name 或 slug)
     */
    public void updateI18nBatch(List<CategoryI18n> i18nList) {
        List<CategoryI18n> mutable = new ArrayList<>(this.i18nList);
        Map<String, CategoryI18n> exsistingI18nByLocaleMap = mutable.stream()
                .collect(Collectors.toMap(CategoryI18n::getLocale, item -> item));
        for (CategoryI18n i18n : i18nList) {
            String normalizedLocale = normalizeLocale(i18n.getLocale());
            requireNotNull(normalizedLocale, "locale 不能为空");

            CategoryI18n existingI18n = exsistingI18nByLocaleMap.get(normalizedLocale);
            if (existingI18n == null) {
                i18n.validate();
                mutable.add(i18n);
                continue;
            }
            String mergedName = i18n.getName() != null ? i18n.getName().strip() : existingI18n.getName();
            String mergedSlug = i18n.getSlug() != null ? i18n.getSlug().strip() : existingI18n.getSlug();
            String mergedBrand = i18n.getBrand() != null ? i18n.getBrand().strip() : existingI18n.getBrand();
            requireNotBlank(mergedName, "分类名称不能为空");
            requireNotBlank(mergedSlug, "分类 slug 不能为空");

            CategoryI18n patched = CategoryI18n.of(normalizedLocale, mergedName, mergedSlug, mergedBrand);
            mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
            mutable.add(patched);
        }
        replaceI18n(mutable);
    }

    /**
     * 更新已存在的多语言条目 (locale 必须存在, 为空字段不更新)
     *
     * @param locale 语言代码
     * @param name   新名称, null 则保留
     * @param slug   新 slug, null 则保留
     * @param brand  新品牌文案, null 则保留
     */
    public void updateI18n(String locale, String name, String slug, String brand) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        List<CategoryI18n> mutable = new ArrayList<>(i18nList);
        CategoryI18n existing = mutable.stream()
                .filter(item -> item.getLocale().equals(normalizedLocale))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("分类多语言不存在: " + normalizedLocale));
        String mergedName = name != null ? name.strip() : existing.getName();
        String mergedSlug = slug != null ? slug.strip() : existing.getSlug();
        String mergedBrand = brand != null ? brand.strip() : existing.getBrand();
        requireNotBlank(mergedName, "分类名称不能为空");
        requireNotBlank(mergedSlug, "分类 slug 不能为空");

        CategoryI18n patched = CategoryI18n.of(normalizedLocale, mergedName, mergedSlug, mergedBrand);
        mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
        mutable.add(patched);
        replaceI18n(mutable);
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
     * 从当前分类的多语言列表中移除指定 locale 的条目
     *
     * @param locale 要移除的语言代码, 必须提供有效的值
     * @throws IllegalParamException 如果提供的 locale 为空或不合法
     */
    public void removeI18n(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        List<CategoryI18n> mutable = new ArrayList<>(i18nList);
        mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
        replaceI18n(mutable);
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

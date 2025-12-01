package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryWithI18n;

import java.util.*;

/**
 * 商品分类仓储接口
 *
 * <p>职责: 读取 product_category 及其 i18n 覆盖, 向领域层提供启用分类的读取能力</p>
 */
public interface IProductCategoryRepository {

    /**
     * 列出所有启用状态的分类
     *
     * @return 启用分类列表
     */
    @NotNull
    List<Category> listEnabledCategories();

    /**
     * 按 slug 查询分类
     *
     * @param slug slug
     * @return 分类
     */
    @NotNull
    Optional<Category> findBySlug(@NotNull String slug);

    /**
     * 按语言 slug 查询分类
     *
     * @param slug   多语言 slug
     * @param locale 语言
     * @return 分类
     */
    @NotNull
    Optional<Category> findByLocalizedSlug(@NotNull String slug, @NotNull String locale);

    /**
     * 按 ID 批量查询分类
     *
     * @param ids ID 集合
     * @return id -> 分类
     */
    @NotNull
    Map<Long, Category> mapByIds(@NotNull Set<Long> ids);

    /**
     * 按 locale 读取所有分类的 i18n 覆盖
     *
     * @param locale 语言代码
     * @return key 为 categoryId 的覆盖 Map
     */
    @NotNull
    Map<Long, CategoryI18n> mapI18nByLocale(@NotNull String locale);

    /**
     * 按 locale 与 ID 集合读取 i18n
     *
     * @param categoryIds 分类ID集合
     * @param locale      语言
     * @return id -> i18n
     */
    @NotNull
    Map<Long, CategoryI18n> mapI18nByLocale(@NotNull Collection<Long> categoryIds, @NotNull String locale);

    /**
     * 根据 ID 获取分类
     *
     * @param id 分类 ID
     * @return 分类
     */
    @NotNull
    Optional<Category> findById(@NotNull Long id);

    /**
     * 查询分类详情 (含 i18n)
     *
     * @param id 分类 ID
     * @return 分类及其多语言
     */
    @NotNull
    Optional<CategoryWithI18n> findWithI18n(@NotNull Long id);

    /**
     * 分页查询分类
     *
     * @param page           页码
     * @param size           每页大小
     * @param filterByParent 是否按父级过滤
     * @param parentId       父级 ID ( filterByParent 为 true 时生效 )
     * @param keyword        关键词
     * @param isEnabled      是否启用
     * @return 分页结果
     */
    @NotNull
    PageResult<Category> page(int page, int size, boolean filterByParent, Long parentId, String keyword, Boolean isEnabled);

    /**
     * 新增分类
     *
     * @param category 分类实体
     * @return 生成的 ID
     */
    @NotNull
    Long insert(@NotNull Category category);

    /**
     * 更新分类
     *
     * @param category 分类实体
     */
    void update(@NotNull Category category);

    /**
     * 批量 upsert 分类的多语言
     *
     * @param categoryId 分类 ID
     * @param payloads   多语言列表
     */
    void upsertI18n(@NotNull Long categoryId, @NotNull List<CategoryI18n> payloads);

    /**
     * 获取分类的多语言列表
     *
     * @param categoryIds 分类 ID
     * @return categoryId -> i18n 列表
     */
    @NotNull
    Map<Long, List<CategoryI18n>> mapI18n(@NotNull Collection<Long> categoryIds);

    /**
     * 判断基础 slug 是否存在
     *
     * @param slug      slug
     * @param excludeId 排除的 ID
     * @return 是否存在
     */
    boolean existsBySlug(@NotNull String slug, Long excludeId);

    /**
     * 判断同一父级下名称是否重复
     *
     * @param parentId  父级 ID
     * @param name      名称
     * @param excludeId 排除的 ID
     * @return 是否存在
     */
    boolean existsByParentAndName(Long parentId, @NotNull String name, Long excludeId);

    /**
     * 判断 locale + slug 是否被占用
     *
     * @param locale            语言
     * @param slug              slug
     * @param excludeCategoryId 排除的分类 ID
     * @return 是否存在
     */
    boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeCategoryId);

    /**
     * 父级变化时更新子孙分类路径和层级
     *
     * @param categoryId 分类 ID
     * @param oldPrefix  旧前缀 ( oldPath + categoryId + '/' )
     * @param newPrefix  新前缀 ( newPath + categoryId + '/' )
     * @param levelDelta 层级差值
     */
    void updateDescendantsPath(Long categoryId, @NotNull String oldPrefix, @NotNull String newPrefix, int levelDelta);

    /**
     * 创建分类并返回详情
     *
     * @param category 分类
     * @param i18nList 多语言列表
     * @return 分类详情
     */
    @NotNull
    CategoryWithI18n createWithI18n(@NotNull Category category, @NotNull List<CategoryI18n> i18nList);

    /**
     * 更新分类, 可级联更新子节点路径与 i18n
     *
     * @param updated    更新后的分类
     * @param oldPrefix  旧路径前缀
     * @param newPrefix  新路径前缀
     * @param levelDelta 层级差
     * @param i18nList   i18n 列表
     * @return 分类详情
     */
    @NotNull
    CategoryWithI18n updateWithRelations(@NotNull Category updated, String oldPrefix, String newPrefix, int levelDelta, List<CategoryI18n> i18nList);

    /**
     * upsert i18n 并返回详情
     *
     * @param categoryId 分类 ID
     * @param i18nList   i18n 列表
     * @return 分类详情
     */
    @NotNull
    CategoryWithI18n upsertI18nAndFetch(@NotNull Long categoryId, @NotNull List<CategoryI18n> i18nList);

    /**
     * 更新分类状态
     *
     * @param category 分类
     * @param status   目标状态
     * @return 分类详情
     */
    @NotNull
    CategoryWithI18n updateStatus(@NotNull Category category, @NotNull CategoryStatus status);

    /**
     * 是否存在子分类
     *
     * @param categoryId 分类 ID
     * @return true 表示存在子分类
     */
    boolean hasChildren(@NotNull Long categoryId);

    /**
     * 是否有商品引用该分类
     *
     * @param categoryId 分类 ID
     * @return true 表示被商品引用
     */
    boolean hasProductReference(@NotNull Long categoryId);

    /**
     * 删除分类（包含多语言）
     *
     * @param categoryId 分类 ID
     */
    void delete(@NotNull Long categoryId);

    /**
     * 简单分页结果
     *
     * @param items 列表
     * @param total 总数
     * @param <T>   类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}

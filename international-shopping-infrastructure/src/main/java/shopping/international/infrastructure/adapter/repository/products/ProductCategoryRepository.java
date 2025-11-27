package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryWithI18n;
import shopping.international.infrastructure.dao.products.ProductCategoryI18nMapper;
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.po.ProductCategoryI18nPO;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;
import shopping.international.types.exceptions.AppException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品分类仓储实现 (MyBatis-Plus)
 */
@Repository
@RequiredArgsConstructor
public class ProductCategoryRepository implements IProductCategoryRepository {

    /**
     * 商品分类 Mapper
     */
    private final ProductCategoryMapper categoryMapper;
    /**
     * 商品分类 i18n Mapper
     */
    private final ProductCategoryI18nMapper categoryI18nMapper;

    /**
     * 查询启用状态的商品分类列表, 并按层级(level) 排序, 同一层级内按排序号(sortOrder) 排序, 最后按 ID 排序
     *
     * @return 一个非空的 <code>Category</code> 对象列表, 包含所有启用状态的商品分类
     */
    @Override
    public @NotNull List<Category> listEnabledCategories() {
        List<ProductCategoryPO> records = categoryMapper.selectList(new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getStatus, CategoryStatus.ENABLED.name())
                .orderByAsc(ProductCategoryPO::getLevel, ProductCategoryPO::getSortOrder, ProductCategoryPO::getId));
        return records.stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public @NotNull Optional<Category> findBySlug(@NotNull String slug) {
        ProductCategoryPO record = categoryMapper.selectOne(new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getSlug, slug)
                .last("limit 1"));
        return Optional.ofNullable(record).map(this::toEntity);
    }

    @Override
    public @NotNull Optional<Category> findByLocalizedSlug(@NotNull String slug, @NotNull String locale) {
        ProductCategoryI18nPO i18n = categoryI18nMapper.selectOne(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getLocale, locale)
                .eq(ProductCategoryI18nPO::getSlug, slug)
                .last("limit 1"));
        if (i18n == null)
            return Optional.empty();
        ProductCategoryPO category = categoryMapper.selectById(i18n.getCategoryId());
        return Optional.ofNullable(category).map(this::toEntity);
    }

    /**
     * 按 ID 批量查询分类
     *
     * @param ids ID 集合
     * @return id -> 分类
     */
    @Override
    public @NotNull Map<Long, Category> mapByIds(@NotNull Set<Long> ids) {
        if (ids.isEmpty())
            return Map.of();
        List<ProductCategoryPO> records = categoryMapper.selectByIds(ids);
        return records.stream()
                .collect(Collectors.toMap(ProductCategoryPO::getId, this::toEntity,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 根据指定的 locale 语言代码, 查询并返回所有分类的本地化信息映射
     *
     * @param locale 语言代码, 如 en-US, 指定要查询的分类本地化信息的语言版本
     * @return 一个以分类 ID (categoryId) 为键, 对应的 <code>CategoryI18n</code> 实体为值的 Map, 包含了给定 locale 的所有分类本地化信息
     */
    @Override
    public @NotNull Map<Long, CategoryI18n> mapI18nByLocale(@NotNull String locale) {
        List<ProductCategoryI18nPO> records = categoryI18nMapper.selectList(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getLocale, locale)
                .orderByAsc(ProductCategoryI18nPO::getCategoryId));
        return records.stream()
                .collect(Collectors.toMap(ProductCategoryI18nPO::getCategoryId, this::toI18nEntity,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 按 locale 与 ID 集合读取 i18n
     *
     * @param categoryIds 分类ID集合
     * @param locale      语言
     * @return id -> i18n
     */
    @Override
    public @NotNull Map<Long, CategoryI18n> mapI18nByLocale(@NotNull Collection<Long> categoryIds, @NotNull String locale) {
        if (categoryIds.isEmpty())
            return Map.of();
        List<ProductCategoryI18nPO> records = categoryI18nMapper.selectList(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getLocale, locale)
                .in(ProductCategoryI18nPO::getCategoryId, categoryIds)
                .orderByAsc(ProductCategoryI18nPO::getCategoryId));
        return records.stream()
                .collect(Collectors.toMap(ProductCategoryI18nPO::getCategoryId, this::toI18nEntity,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    @Override
    public @NotNull Optional<Category> findById(@NotNull Long id) {
        ProductCategoryPO po = categoryMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public @NotNull Optional<CategoryWithI18n> findWithI18n(@NotNull Long id) {
        Optional<Category> categoryOpt = findById(id);
        if (categoryOpt.isEmpty())
            return Optional.empty();
        Map<Long, List<CategoryI18n>> i18nMap = mapI18n(Set.of(id));
        return Optional.of(new CategoryWithI18n(categoryOpt.get(), i18nMap.getOrDefault(id, List.of())));
    }

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
    @Override
    public @NotNull PageResult<Category> page(int page, int size, boolean filterByParent, Long parentId, String keyword, Boolean isEnabled) {
        String status = statusString(isEnabled);
        long total = categoryMapper.countAdminPage(filterByParent, parentId, keyword, status);
        if (total == 0)
            return new PageResult<>(List.of(), 0);
        int offset = Math.max(page - 1, 0) * size;
        List<ProductCategoryPO> records = categoryMapper.selectAdminPage(filterByParent, parentId, keyword, status, size, offset);
        List<Category> items = records.stream().map(this::toEntity).toList();
        return new PageResult<>(items, total);
    }

    /**
     * 新增分类
     *
     * @param category 分类实体
     * @return 生成的 ID
     */
    @Override
    public @NotNull Long insert(@NotNull Category category) {
        ProductCategoryPO po = toPo(category);
        categoryMapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(@NotNull Category category) {
        categoryMapper.updateById(toPo(category));
    }

    /**
     * 批量 upsert 分类的多语言
     *
     * @param categoryId 分类 ID
     * @param payloads   多语言列表
     */
    @Override
    public void upsertI18n(@NotNull Long categoryId, @NotNull List<CategoryI18n> payloads) {
        if (payloads.isEmpty())
            return;
        // 根据 CategoryId 获取 CategoryId -> I18n 的映射
        Map<String, ProductCategoryI18nPO> existingMap = categoryI18nMapper.selectList(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                        .eq(ProductCategoryI18nPO::getCategoryId, categoryId))
                .stream()
                .collect(Collectors.toMap(ProductCategoryI18nPO::getLocale, Function.identity(),
                        (current, ignore) -> current, LinkedHashMap::new));
        for (CategoryI18n vo : payloads) {
            ProductCategoryI18nPO po = existingMap.get(vo.getLocale());
            if (po == null) {
                categoryI18nMapper.insert(ProductCategoryI18nPO.builder()
                        .categoryId(categoryId)
                        .locale(vo.getLocale())
                        .name(vo.getName())
                        .slug(vo.getSlug())
                        .brand(vo.getBrand())
                        .build());
                continue;
            }
            po.setName(vo.getName());
            po.setSlug(vo.getSlug());
            po.setBrand(vo.getBrand());
            categoryI18nMapper.updateById(po);
        }
    }

    @Override
    public @NotNull Map<Long, List<CategoryI18n>> mapI18n(@NotNull Collection<Long> categoryIds) {
        if (categoryIds.isEmpty())
            return Map.of();
        List<ProductCategoryI18nPO> records = categoryI18nMapper.selectList(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .in(ProductCategoryI18nPO::getCategoryId, categoryIds)
                .orderByAsc(ProductCategoryI18nPO::getCategoryId));
        Map<Long, List<CategoryI18n>> result = new LinkedHashMap<>();
        for (ProductCategoryI18nPO po : records) {
            result.computeIfAbsent(po.getCategoryId(), ignore -> new ArrayList<>())
                    .add(toI18nEntity(po));
        }
        return result;
    }

    /**
     * 判断基础 slug 是否存在
     *
     * @param slug      slug
     * @param excludeId 排除的 ID
     * @return 是否存在
     */
    @Override
    public boolean existsBySlug(@NotNull String slug, Long excludeId) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getSlug, slug);
        if (excludeId != null)
            wrapper.ne(ProductCategoryPO::getId, excludeId);
        Long count = categoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByParentAndName(Long parentId, @NotNull String name, Long excludeId) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getName, name);
        if (parentId == null)
            wrapper.isNull(ProductCategoryPO::getParentId);
        else
            wrapper.eq(ProductCategoryPO::getParentId, parentId);
        if (excludeId != null)
            wrapper.ne(ProductCategoryPO::getId, excludeId);
        Long count = categoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /**
     * 判断 locale + slug 是否被占用
     *
     * @param locale            语言
     * @param slug              slug
     * @param excludeCategoryId 排除的分类 ID
     * @return 是否存在
     */
    @Override
    public boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeCategoryId) {
        LambdaQueryWrapper<ProductCategoryI18nPO> wrapper = new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getLocale, locale)
                .eq(ProductCategoryI18nPO::getSlug, slug);
        if (excludeCategoryId != null)
            wrapper.ne(ProductCategoryI18nPO::getCategoryId, excludeCategoryId);
        Long count = categoryI18nMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public void updateDescendantsPath(Long categoryId, @NotNull String oldPrefix, @NotNull String newPrefix, int levelDelta) {
        categoryMapper.updateDescendantsPath(categoryId, oldPrefix, newPrefix, levelDelta);
    }

    /**
     * 创建分类并返回详情
     *
     * @param category 分类
     * @param i18nList 多语言列表
     * @return 分类详情
     */
    @Override
    @Transactional
    public @NotNull CategoryWithI18n createWithI18n(@NotNull Category category, @NotNull List<CategoryI18n> i18nList) {
        Long id = insert(category);
        if (!i18nList.isEmpty())
            upsertI18n(id, i18nList);
        return findWithI18n(id).orElseThrow(() -> new AppException("分类创建失败"));
    }

    @Override
    @Transactional
    public @NotNull CategoryWithI18n updateWithRelations(@NotNull Category updated, String oldPrefix, String newPrefix, int levelDelta, List<CategoryI18n> i18nList) {
        update(updated);
        if (oldPrefix != null && newPrefix != null && (!Objects.equals(oldPrefix, newPrefix) || levelDelta != 0))
            updateDescendantsPath(updated.getId(), oldPrefix, newPrefix, levelDelta);
        if (i18nList != null && !i18nList.isEmpty())
            upsertI18n(updated.getId(), i18nList);
        return findWithI18n(updated.getId()).orElseThrow(() -> new AppException("分类更新失败"));
    }

    @Override
    @Transactional
    public @NotNull CategoryWithI18n upsertI18nAndFetch(@NotNull Long categoryId, @NotNull List<CategoryI18n> i18nList) {
        if (!i18nList.isEmpty())
            upsertI18n(categoryId, i18nList);
        return findWithI18n(categoryId).orElseThrow(() -> new AppException("分类不存在"));
    }

    @Override
    @Transactional
    public @NotNull CategoryWithI18n updateStatus(@NotNull Category category, @NotNull CategoryStatus status) {
        Category updated = Category.reconstitute(category.getId(), category.getParentId(), category.getName(), category.getSlug(),
                category.getLevel(), category.getPath(), category.getSortOrder(), status, category.getCreatedAt(), category.getUpdatedAt());
        update(updated);
        return findWithI18n(updated.getId()).orElse(new CategoryWithI18n(updated, List.of()));
    }

    /**
     * 将 <code>Category</code> 对象转换为 <code>ProductCategoryPO</code> 持久化对象
     *
     * @param category 要转换的分类实体, 包含了如 ID, 父级 ID, 名称, 别名等信息
     * @return 由给定 <code>Category</code> 实体构建的 <code>ProductCategoryPO</code> 对象, 用于数据库持久化操作
     */
    private ProductCategoryPO toPo(Category category) {
        return ProductCategoryPO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .level(category.getLevel())
                .path(category.getPath())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus() == null ? null : category.getStatus().name())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * 将 <code>ProductCategoryPO</code> 对象转换为 <code>Category</code> 实体
     *
     * @param po 要转换的持久化对象
     * @return 由给定 <code>ProductCategoryPO</code> 对象重建的分类实体
     */
    private Category toEntity(ProductCategoryPO po) {
        return Category.reconstitute(
                po.getId(),
                po.getParentId(),
                po.getName(),
                po.getSlug(),
                po.getLevel() == null ? 1 : po.getLevel(),
                po.getPath(),
                po.getSortOrder() == null ? 0 : po.getSortOrder(),
                CategoryStatus.from(po.getStatus()),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 将 <code>ProductCategoryI18nPO</code> 对象转换为 <code>CategoryI18n</code> 实体
     *
     * @param po 要转换的持久化对象, 包含分类本地化信息
     * @return 由给定 <code>ProductCategoryI18nPO</code> 对象重建的商品分类本地化实体
     */
    private CategoryI18n toI18nEntity(ProductCategoryI18nPO po) {
        return CategoryI18n.of(po.getLocale(), po.getName(), po.getSlug(), po.getBrand());
    }

    /**
     * 根据给定的布尔值, 返回相应的分类状态字符串表示
     *
     * @param enabled 一个 <code>Boolean</code> 类型的参数, 表示是否启用的状态. 如果为 <code>null</code>, 则方法返回 <code>null</code>
     * @return 如果 <code>enabled</code> 参数为 <code>true</code>, 则返回 {@link CategoryStatus#ENABLED} 的名称; 如果为 <code>false</code> 或 <code>null</code>, 则返回 {@link CategoryStatus#DISABLED} 的名称
     *
     */
    private String statusString(Boolean enabled) {
        if (enabled == null)
            return null;
        return enabled ? CategoryStatus.ENABLED.name() : CategoryStatus.DISABLED.name();
    }
}

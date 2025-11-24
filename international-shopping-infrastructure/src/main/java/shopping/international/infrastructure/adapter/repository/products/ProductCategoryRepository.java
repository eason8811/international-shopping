package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.infrastructure.dao.products.ProductCategoryI18nMapper;
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.po.ProductCategoryI18nPO;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;

import java.util.*;
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

    @Override
    public @NotNull Map<Long, Category> mapByIds(@NotNull Set<Long> ids) {
        if (ids.isEmpty())
            return Map.of();
        List<ProductCategoryPO> records = categoryMapper.selectBatchIds(ids);
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
                CategoryStatus.from(po.getStatus())
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
}

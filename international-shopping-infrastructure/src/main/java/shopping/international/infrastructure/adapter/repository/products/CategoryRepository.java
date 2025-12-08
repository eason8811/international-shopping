package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.ICategoryRepository;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.infrastructure.dao.products.ProductCategoryI18nMapper;
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.ProductMapper;
import shopping.international.infrastructure.dao.products.po.ProductCategoryI18nPO;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;
import shopping.international.infrastructure.dao.products.po.ProductPO;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;

/**
 * 基于 MyBatis-Plus 的分类聚合仓储实现
 *
 * <p>负责组合读写 {@code product_category} 与 {@code product_category_i18n}, 并提供必要的唯一性及引用校验</p>
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepository implements ICategoryRepository {

    /**
     * 分类主表 Mapper
     */
    private final ProductCategoryMapper categoryMapper;
    /**
     * 分类多语言表 Mapper
     */
    private final ProductCategoryI18nMapper categoryI18nMapper;
    /**
     * 商品表 Mapper, 用于删除校验
     */
    private final ProductMapper productMapper;

    /**
     * 按 ID 查询分类 (含 i18n)
     *
     * @param categoryId 分类 ID
     * @return 聚合, 可为空
     */
    @Override
    public Optional<Category> findById(@NotNull Long categoryId) {
        ProductCategoryPO po = categoryMapper.selectWithI18nById(categoryId);
        if (po == null || po.getId() == null)
            return Optional.empty();
        return Optional.of(toAggregate(po));
    }

    /**
     * 按条件列出全量分类, 常用于构建树
     *
     * @param status 状态过滤, 为空则不过滤
     * @return 按 sortOrder、id 升序的分类列表, 包含 i18n
     */
    @Override
    public @NotNull List<Category> listAll(@Nullable CategoryStatus status) {
        List<ProductCategoryPO> pos = categoryMapper.selectWithI18n(null, false, null,
                status == null ? null : status.name(), null, null);
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream().map(this::toAggregate).toList();
    }


    /**
     * 分页列出分类
     *
     * @param parentId        父分类 ID, 可空
     * @param parentSpecified 是否对 parentId 进行过滤 (区分 "未传" 与 "明确为 null" )
     * @param keyword         关键词 (匹配 name/slug), 可空
     * @param status          启用状态过滤, 可空
     * @param offset          偏移量, 从 0 开始
     * @param limit           单页数量
     * @return 当前页的分类列表
     */
    @Override
    public @NotNull List<Category> list(@Nullable Long parentId, boolean parentSpecified, @Nullable String keyword,
                                        @Nullable CategoryStatus status, int offset, int limit) {
        List<ProductCategoryPO> pos = categoryMapper.selectWithI18n(
                parentId, parentSpecified, keyword,
                status == null ? null : status.name(),
                offset, limit);
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream().map(this::toAggregate).toList();
    }

    /**
     * 统计满足条件的分类数量
     *
     * @param parentId        父分类 ID, 可空
     * @param parentSpecified 是否对 parentId 进行过滤
     * @param keyword         关键词
     * @param status          状态过滤
     * @return 总数
     */
    @Override
    public long count(@Nullable Long parentId, boolean parentSpecified, @Nullable String keyword,
                      @Nullable CategoryStatus status) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = buildBaseFilter(parentId, parentSpecified, keyword, status);
        Long total = categoryMapper.selectCount(wrapper);
        return total == null ? 0 : total;
    }

    /**
     * 检查 slug 是否已存在
     *
     * @param slug              唯一 slug
     * @param excludeCategoryId 需要排除的分类 ID, 可空
     * @return 是否存在
     */
    @Override
    public boolean existsBySlug(@NotNull String slug, @Nullable Long excludeCategoryId) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getSlug, slug);
        if (excludeCategoryId != null)
            wrapper.ne(ProductCategoryPO::getId, excludeCategoryId);
        Long n = categoryMapper.selectCount(wrapper);
        return n != null && n > 0;
    }

    /**
     * 检查同一父级下名称是否重复
     *
     * @param parentId          父分类 ID, 可空表示根
     * @param name              分类名称
     * @param excludeCategoryId 需要排除的分类 ID, 可空
     * @return 是否存在重名
     */
    @Override
    public boolean existsByParentAndName(@Nullable Long parentId, @NotNull String name, @Nullable Long excludeCategoryId) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getName, name);
        if (parentId == null)
            wrapper.isNull(ProductCategoryPO::getParentId);
        else
            wrapper.eq(ProductCategoryPO::getParentId, parentId);
        if (excludeCategoryId != null)
            wrapper.ne(ProductCategoryPO::getId, excludeCategoryId);
        Long n = categoryMapper.selectCount(wrapper);
        return n != null && n > 0;
    }

    /**
     * 新增分类 (含 i18n)
     *
     * @param category 待保存的新聚合, ID 为空
     * @return 带持久化 ID 的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Category save(@NotNull Category category) {
        ProductCategoryPO po = ProductCategoryPO.builder()
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .level(category.getLevel())
                .path(category.getPath())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus().name())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
        try {
            categoryMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("分类唯一约束冲突", e);
        }
        category.assignId(po.getId());
        persistI18nSnapshot(category.getId(), category.getI18nList());
        return findById(category.getId()).orElseThrow(() -> new ConflictException("分类已保存但未能回读"));
    }

    /**
     * 更新分类基础信息及可选的 i18n
     *
     * @param category    已存在的聚合快照
     * @param replaceI18n 是否重写 i18n 表 (为 {@code false} 时不改 i18n)
     * @param moveContext 父级变更时传入, 用于批量调整子节点层级与路径, 可空
     * @return 更新后的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Category update(@NotNull Category category, boolean replaceI18n, @Nullable MoveContext moveContext) {
        LambdaUpdateWrapper<ProductCategoryPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductCategoryPO::getId, category.getId())
                .set(ProductCategoryPO::getParentId, category.getParentId())
                .set(ProductCategoryPO::getName, category.getName())
                .set(ProductCategoryPO::getSlug, category.getSlug())
                .set(ProductCategoryPO::getLevel, category.getLevel())
                .set(ProductCategoryPO::getPath, category.getPath())
                .set(ProductCategoryPO::getSortOrder, category.getSortOrder())
                .set(ProductCategoryPO::getStatus, category.getStatus().name());
        try {
            categoryMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("分类唯一约束冲突", e);
        }

        if (replaceI18n)
            persistI18nSnapshot(category.getId(), category.getI18nList());

        if (moveContext != null)
            moveDescendants(moveContext);

        return findById(category.getId()).orElseThrow(() -> new ConflictException("分类更新后回读失败"));
    }

    /**
     * 删除分类及其 i18n
     *
     * @param categoryId 分类 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(@NotNull Long categoryId) {
        categoryI18nMapper.delete(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getCategoryId, categoryId));
        categoryMapper.deleteById(categoryId);
    }

    /**
     * 判断是否存在子分类
     *
     * @param categoryId 分类 ID
     * @return 是否有子节点
     */
    @Override
    public boolean hasChildren(@NotNull Long categoryId) {
        Long n = categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategoryPO>()
                .eq(ProductCategoryPO::getParentId, categoryId)
                .last("limit 1"));
        return n != null && n > 0;
    }

    /**
     * 判断分类下是否存在商品
     *
     * @param categoryId 分类 ID
     * @return 是否存在商品引用
     */
    @Override
    public boolean hasProducts(@NotNull Long categoryId) {
        Long n = productMapper.selectCount(new LambdaQueryWrapper<ProductPO>()
                .eq(ProductPO::getCategoryId, categoryId)
                .last("limit 1"));
        return n != null && n > 0;
    }

    /**
     * 新增单条多语言记录
     *
     * @param categoryId 分类 ID
     * @param i18n       多语言值对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n) {
        ProductCategoryI18nPO po = ProductCategoryI18nPO.builder()
                .categoryId(categoryId)
                .locale(i18n.getLocale())
                .name(i18n.getName())
                .slug(i18n.getSlug())
                .brand(i18n.getBrand())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            categoryI18nMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("分类多语言唯一约束冲突", e);
        }
    }

    /**
     * 更新单条多语言记录
     *
     * @param categoryId 分类 ID
     * @param i18n       新值对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n) {
        LambdaUpdateWrapper<ProductCategoryI18nPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductCategoryI18nPO::getCategoryId, categoryId)
                .eq(ProductCategoryI18nPO::getLocale, i18n.getLocale())
                .set(ProductCategoryI18nPO::getName, i18n.getName())
                .set(ProductCategoryI18nPO::getSlug, i18n.getSlug())
                .set(ProductCategoryI18nPO::getBrand, i18n.getBrand())
                .set(ProductCategoryI18nPO::getUpdatedAt, LocalDateTime.now());
        categoryI18nMapper.update(null, wrapper);
    }

    /**
     * 列出指定分类的全部多语言
     *
     * @param categoryId 分类 ID
     * @return 多语言列表
     */
    @Override
    public @NotNull List<CategoryI18n> listI18nByCategoryId(@NotNull Long categoryId) {
        ProductCategoryPO po = categoryMapper.selectWithI18nById(categoryId);
        if (po == null || po.getI18nList() == null || po.getI18nList().isEmpty())
            return Collections.emptyList();
        return po.getI18nList().stream()
                .filter(Objects::nonNull)
                .map(this::toI18n)
                .toList();
    }

    /**
     * 构造基础查询条件
     *
     * @param parentId        父分类 ID
     * @param parentSpecified 是否过滤 parent
     * @param keyword         关键词
     * @param status          状态
     * @return 查询 wrapper
     */
    private LambdaQueryWrapper<ProductCategoryPO> buildBaseFilter(@Nullable Long parentId, boolean parentSpecified,
                                                                  @Nullable String keyword, @Nullable CategoryStatus status) {
        LambdaQueryWrapper<ProductCategoryPO> wrapper = new LambdaQueryWrapper<>();
        if (parentSpecified && parentId == null)
            wrapper.isNull(ProductCategoryPO::getParentId);
        if (parentSpecified && parentId != null)
            wrapper.eq(ProductCategoryPO::getParentId, parentId);
        if (keyword != null && !keyword.isBlank())
            wrapper.and(q -> q.like(ProductCategoryPO::getName, keyword)
                    .or().like(ProductCategoryPO::getSlug, keyword));
        if (status != null)
            wrapper.eq(ProductCategoryPO::getStatus, status.name());
        return wrapper;
    }

    /**
     * 将主表与 i18n 组合成领域聚合
     *
     * @param po 主表数据
     * @return 聚合
     */
    private Category toAggregate(@NotNull ProductCategoryPO po) {
        List<CategoryI18n> i18nList = po.getI18nList() == null ? Collections.emptyList()
                : po.getI18nList().stream()
                .filter(Objects::nonNull)
                .map(this::toI18n)
                .toList();
        CategoryStatus status = CategoryStatus.from(po.getStatus());
        return Category.reconstitute(
                po.getId(), po.getParentId(), po.getName(), po.getSlug(),
                po.getLevel() == null ? 1 : po.getLevel(),
                po.getPath(), po.getSortOrder() == null ? 0 : po.getSortOrder(),
                status, null, i18nList, po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    /**
     * 将 i18n PO 转为值对象
     *
     * @param po 多语言持久化对象
     * @return 值对象
     */
    private CategoryI18n toI18n(@NotNull ProductCategoryI18nPO po) {
        return CategoryI18n.of(normalizeLocale(po.getLocale()), po.getName(), po.getSlug(), po.getBrand());
    }

    /**
     * 持久化完整的 i18n 快照（先删后插）
     *
     * @param categoryId 分类 ID
     * @param i18nList   多语言列表
     */
    private void persistI18nSnapshot(@NotNull Long categoryId, @Nullable List<CategoryI18n> i18nList) {
        categoryI18nMapper.delete(new LambdaQueryWrapper<ProductCategoryI18nPO>()
                .eq(ProductCategoryI18nPO::getCategoryId, categoryId));
        if (i18nList == null || i18nList.isEmpty())
            return;
        LocalDateTime now = LocalDateTime.now();
        for (CategoryI18n i18n : i18nList) {
            ProductCategoryI18nPO po = ProductCategoryI18nPO.builder()
                    .categoryId(categoryId)
                    .locale(i18n.getLocale())
                    .name(i18n.getName())
                    .slug(i18n.getSlug())
                    .brand(i18n.getBrand())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            categoryI18nMapper.insert(po);
        }
    }

    /**
     * 移动子树的 path 与 level
     *
     * @param moveContext 移动上下文
     */
    private void moveDescendants(@NotNull MoveContext moveContext) {
        List<ProductCategoryPO> descendants = categoryMapper.selectList(new LambdaQueryWrapper<ProductCategoryPO>()
                .likeRight(ProductCategoryPO::getPath, moveContext.oldPrefix()));
        for (ProductCategoryPO child : descendants) {
            String newPath = replacePrefix(child.getPath(), moveContext.oldPrefix(), moveContext.newPrefix());
            Integer newLevel = (child.getLevel() == null ? 1 : child.getLevel()) + moveContext.levelDelta();
            LambdaUpdateWrapper<ProductCategoryPO> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ProductCategoryPO::getId, child.getId())
                    .set(ProductCategoryPO::getPath, newPath)
                    .set(ProductCategoryPO::getLevel, newLevel);
            categoryMapper.update(null, wrapper);
        }
    }

    /**
     * 替换路径前缀
     *
     * @param originalPath 原始 path
     * @param oldPrefix    旧前缀
     * @param newPrefix    新前缀
     * @return 替换后的 path
     */
    private String replacePrefix(@Nullable String originalPath, @NotNull String oldPrefix, @NotNull String newPrefix) {
        String source = originalPath == null ? "" : originalPath;
        if (!source.startsWith(oldPrefix))
            return source;
        String suffix = source.substring(oldPrefix.length());
        String candidate = newPrefix + suffix;
        return candidate.isBlank() ? null : candidate;
    }
}

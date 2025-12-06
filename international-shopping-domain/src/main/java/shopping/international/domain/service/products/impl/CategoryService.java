package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.product.ICategoryRepository;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.service.products.ICategoryService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品分类领域服务实现
 *
 * <p>职责: 校验业务约束 (唯一性, 层级合法性, 删除保护等), 编排聚合行为并调用仓储完成持久化</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    /**
     * 分类聚合仓储
     */
    private final ICategoryRepository categoryRepository;

    /**
     * 列出全部启用分类 (含 i18n), 用于构建用户侧分类树
     *
     * @return 分类列表
     */
    @Override
    public @NotNull List<Category> listEnabled() {
        return categoryRepository.listAll(CategoryStatus.ENABLED);
    }

    /**
     * 分页筛选分类 (管理侧)
     *
     * @param page            页码, 从 1 开始
     * @param size            页大小
     * @param parentSpecified 是否按父级过滤
     * @param parentId        父级 ID, 可空
     * @param keyword         关键词, 可空
     * @param isEnabled       启用过滤, 可空
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult list(int page, int size, boolean parentSpecified, @Nullable Long parentId,
                                    @Nullable String keyword, @Nullable Boolean isEnabled) {
        int offset = (page - 1) * size;
        CategoryStatus status = isEnabled == null ? null : (isEnabled ? CategoryStatus.ENABLED : CategoryStatus.DISABLED);
        String trimmedKeyword = keyword == null ? null : keyword.strip();

        List<Category> items = categoryRepository.list(parentId, parentSpecified, trimmedKeyword, status, offset, size);
        long total = categoryRepository.count(parentId, parentSpecified, trimmedKeyword, status);
        return new PageResult(items, total);
    }

    /**
     * 获取分类详情
     *
     * @param categoryId 分类 ID
     * @return 聚合
     */
    @Override
    public @NotNull Category get(@NotNull Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalParamException("分类不存在"));
    }

    /**
     * 创建分类
     *
     * @param name      名称
     * @param slug      唯一 slug
     * @param parentId  父分类 ID, 可空
     * @param sortOrder 排序
     * @param isEnabled 启用状态
     * @param i18nList  多语言列表, 可空
     * @return 新建聚合
     */
    @Override
    public @NotNull Category create(@NotNull String name, @NotNull String slug, @Nullable Long parentId,
                                    @NotNull Integer sortOrder, @NotNull Boolean isEnabled,
                                    @NotNull List<CategoryI18n> i18nList) {
        Category parent = parentId == null ? null : categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalParamException("父分类不存在"));
        int level = parent == null ? 1 : parent.getLevel() + 1;
        String path = buildPath(parent);
        Category category = Category.create(parentId, level, name, slug, sortOrder, null, i18nList);

        category.moveTo(parentId, level, path);
        if (!isEnabled)
            category.changeStatus(CategoryStatus.DISABLED);

        ensureUniqueness(category.getId(), category.getParentId(), category.getName(), category.getSlug());

        return categoryRepository.save(category);
    }

    /**
     * 更新分类基础信息及可选 i18n
     *
     * @param categoryId  分类 ID
     * @param name        新名称, 可空
     * @param slug        新 slug, 可空
     * @param parentId    新父级, 可空
     * @param sortOrder   新排序, 可空
     * @param isEnabled   新启用状态, 可空
     * @param i18nPatches 多语言增量列表, 可空
     * @return 更新后的聚合
     */
    @Override
    public @NotNull Category update(@NotNull Long categoryId, @Nullable String name, @Nullable String slug,
                                    @Nullable Long parentId, @Nullable Integer sortOrder, @Nullable Boolean isEnabled,
                                    @Nullable List<CategoryI18nPatch> i18nPatches) {
        Category current = get(categoryId);

        Category parent = parentId == null ? null : get(parentId);
        if (parentId != null && parentId.equals(categoryId))
            throw new IllegalParamException("分类不能移动到自身下");
        if (parent != null && isDescendant(parent, current))
            throw new IllegalParamException("分类不能移动到自己的子孙节点下");

        String oldPath = current.getPath();
        int oldLevel = current.getLevel();

        boolean parentChanged = parentId != null ? !parentId.equals(current.getParentId()) : current.getParentId() != null;
        if (parentChanged) {
            int newLevel = parent == null ? 1 : parent.getLevel() + 1;
            String newPath = buildPath(parent);
            current.moveTo(parentId, newLevel, newPath);
        }

        if (name != null || slug != null)
            current.updateBasic(name, slug, null);
        if (sortOrder != null)
            current.updateSortOrder(sortOrder);
        if (isEnabled != null)
            current.changeStatus(isEnabled ? CategoryStatus.ENABLED : CategoryStatus.DISABLED);

        boolean replaceI18n = i18nPatches != null;
        if (i18nPatches != null) {
            List<CategoryI18n> merged = mergeI18n(current.getI18nList(), i18nPatches);
            current.replaceI18n(merged);
        }

        ensureUniqueness(categoryId, current.getParentId(),
                name == null ? current.getName() : name,
                slug == null ? current.getSlug() : slug);

        ICategoryRepository.MoveContext moveContext = null;
        if (parentChanged) {
            String oldPrefix = buildDescendantPrefix(oldPath, current.getId());
            String newPrefix = buildDescendantPrefix(current.getPath(), current.getId());
            int levelDelta = current.getLevel() - oldLevel;
            moveContext = new ICategoryRepository.MoveContext(oldPrefix, newPrefix, levelDelta);
        }

        categoryRepository.update(current, replaceI18n, moveContext);
        return get(categoryId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(@NotNull Long categoryId) {
        Category category = get(categoryId);
        if (categoryRepository.hasChildren(categoryId))
            throw new ConflictException("存在子分类, 无法删除");
        if (categoryRepository.hasProducts(categoryId))
            throw new ConflictException("分类下存在商品引用, 无法删除");
        categoryRepository.delete(category.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CategoryI18n addI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n) {
        Category category = get(categoryId);
        category.addI18n(i18n);
        categoryRepository.saveI18n(categoryId, i18n);
        return i18n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CategoryI18n updateI18n(@NotNull Long categoryId, @NotNull CategoryI18nPatch patch) {
        Category category = get(categoryId);
        category.updateI18n(patch.locale(), patch.name(), patch.slug(), patch.brand());
        CategoryI18n updated = category.getI18nList().stream()
                .filter(item -> item.getLocale().equals(normalizeLocale(patch.locale())))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("指定语言不存在"));
        categoryRepository.updateI18n(categoryId, updated);
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Category toggleStatus(@NotNull Long categoryId, boolean enable) {
        Category category = get(categoryId);
        CategoryStatus target = enable ? CategoryStatus.ENABLED : CategoryStatus.DISABLED;
        if (category.getStatus() != target) {
            category.changeStatus(target);
            categoryRepository.update(category, false, null);
        }
        return get(categoryId);
    }

    /**
     * 合并增量 i18n, 保证 name/slug 完整
     *
     * @param existing 现有列表
     * @param patches  增量指令
     * @return 合并后的完整列表
     */
    private List<CategoryI18n> mergeI18n(@Nullable List<CategoryI18n> existing,
                                         @NotNull List<CategoryI18nPatch> patches) {
        List<CategoryI18n> source = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
        List<CategoryI18n> result = new ArrayList<>();
        for (CategoryI18nPatch patch : patches) {
            String locale = normalizeLocale(patch.locale());
            requireNotNull(locale, "locale 不能为空");
            Optional<CategoryI18n> current = source.stream()
                    .filter(item -> item.getLocale().equals(locale))
                    .findFirst();
            String name = patch.name() == null && current.isPresent() ? current.get().getName() : patch.name();
            String slug = patch.slug() == null && current.isPresent() ? current.get().getSlug() : patch.slug();
            String brand = patch.brand() == null && current.isPresent() ? current.get().getBrand() : patch.brand();
            requireNotBlank(name, "分类名称不能为空");
            requireNotBlank(slug, "分类 slug 不能为空");
            result.add(CategoryI18n.of(locale, name, slug, brand));
        }
        return result;
    }

    /**
     * 计算分类路径字符串
     *
     * @param parent 父分类
     * @return 路径, 如 /1/3/
     */
    private String buildPath(@Nullable Category parent) {
        if (parent == null || parent.getId() == null)
            return null;
        String parentPath = parent.getPath();
        String normalized = (parentPath == null || parentPath.isBlank()) ? "/" : parentPath;
        if (!normalized.endsWith("/"))
            normalized = normalized + "/";
        return normalized + parent.getId() + "/";
    }

    /**
     * 生成子树路径前缀 (旧/新)
     *
     * @param path       当前 path
     * @param categoryId 分类 ID
     * @return 子树前缀
     */
    private String buildDescendantPrefix(@Nullable String path, @NotNull Long categoryId) {
        String normalized = (path == null || path.isBlank()) ? "/" : path;
        if (!normalized.endsWith("/"))
            normalized = normalized + "/";
        return normalized + categoryId + "/";
    }

    /**
     * 判断 candidate 是否为 ancestor 的子孙
     *
     * @param candidate 待判定节点
     * @param ancestor  祖先节点
     * @return 是否为子孙
     */
    private boolean isDescendant(@NotNull Category candidate, @NotNull Category ancestor) {
        if (ancestor.getId() == null || candidate.getPath() == null)
            return false;
        String marker = "/" + ancestor.getId() + "/";
        return candidate.getPath().contains(marker);
    }

    /**
     * 校验 slug 与“同父同名”的唯一性
     *
     * @param categoryId 当前分类 ID, 可空
     * @param parentId   父 ID
     * @param name       名称
     * @param slug       slug
     */
    private void ensureUniqueness(@Nullable Long categoryId, @Nullable Long parentId,
                                  @NotNull String name, @NotNull String slug) {
        if (categoryRepository.existsBySlug(slug, categoryId))
            throw new ConflictException("分类 slug 已存在");
        if (categoryRepository.existsByParentAndName(parentId, name, categoryId))
            throw new ConflictException("同级下分类名称重复");
    }
}

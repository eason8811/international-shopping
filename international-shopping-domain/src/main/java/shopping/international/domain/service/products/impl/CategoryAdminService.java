package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;
import shopping.international.domain.model.vo.products.CategoryUpsertCommand;
import shopping.international.domain.model.vo.products.CategoryWithI18n;
import shopping.international.domain.service.products.ICategoryAdminService;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 分类管理服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryAdminService implements ICategoryAdminService {

    private static final int NAME_MAX = 64;
    private static final int SLUG_MAX = 64;
    private static final int BRAND_MAX = 120;
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");

    /**
     * 分类仓储
     */
    private final IProductCategoryRepository categoryRepository;

    /**
     * 分页查询分类
     */
    @Override
    public @NotNull PageResult<CategoryNode> page(int page, int size, boolean filterByParent, Long parentId, String keyword, Boolean isEnabled) {
        int pageNo = page <= 0 ? 1 : page;
        int pageSize = size <= 0 ? 20 : Math.min(size, 100);
        String normalizedKeyword = normalizeKeyword(keyword);
        IProductCategoryRepository.PageResult<Category> pageResult = categoryRepository.page(pageNo, pageSize, filterByParent, parentId, normalizedKeyword, isEnabled);
        if (pageResult.items().isEmpty())
            return new PageResult<>(List.of(), pageResult.total());

        Set<Long> ids = pageResult.items().stream()
                .map(Category::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, List<CategoryI18n>> i18nMap = ids.isEmpty() ? Map.of() : categoryRepository.mapI18n(ids);

        List<CategoryNode> nodes = pageResult.items().stream()
                .map(cat -> CategoryNode.from(cat, null, i18nMap.get(cat.getId())))
                .toList();
        return new PageResult<>(nodes, pageResult.total());
    }

    /**
     * 分类详情
     */
    @Override
    public @NotNull CategoryNode detail(@NotNull Long categoryId) {
        CategoryWithI18n detail = categoryRepository.findWithI18n(categoryId)
                .orElseThrow(() -> IllegalParamException.of("分类不存在"));
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 创建分类
     */
    @Override
    public @NotNull CategoryNode create(@NotNull CategoryUpsertCommand command) {
        String name = normalizeName(command.getName());
        String slug = normalizeSlug(command.getSlug());
        Long parentId = normalizeParentId(command.getParentId());
        int sortOrder = command.getSortOrder() == null ? 0 : command.getSortOrder();
        String brand = normalizeBrand(command.getBrand());
        Category parent = resolveParent(parentId);
        validateUniqueness(slug, name, parentId, null);

        List<CategoryI18n> i18nList = normalizeI18n(command.getI18nList(), brand, null);
        CategoryStatus status = resolveStatus(command.getIsEnabled(), CategoryStatus.ENABLED);
        String path = buildPath(parent);
        int level = parent == null ? 1 : parent.getLevel() + 1;

        Category category = Category.reconstitute(null, parentId, name, slug, level, path, sortOrder, status, null, null);
        CategoryWithI18n detail = categoryRepository.createWithI18n(category, i18nList);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 更新分类
     */
    @Override
    public @NotNull CategoryNode update(@NotNull Long categoryId, @NotNull CategoryUpsertCommand command) {
        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> IllegalParamException.of("分类不存在"));
        String name = normalizeName(command.getName());
        String slug = normalizeSlug(command.getSlug());
        Long parentId = normalizeParentId(command.getParentId());
        int sortOrder = command.getSortOrder() == null ? 0 : command.getSortOrder();
        String brand = normalizeBrand(command.getBrand());
        Category parent = resolveParent(parentId);
        ensureNoCycle(existing, parent);
        validateUniqueness(slug, name, parentId, categoryId);

        List<CategoryI18n> i18nList = command.getI18nList() == null
                ? null
                : normalizeI18n(command.getI18nList(), brand, categoryId);
        CategoryStatus status = resolveStatus(command.getIsEnabled(), existing.getStatus());
        String newPath = buildPath(parent);
        int newLevel = parent == null ? 1 : parent.getLevel() + 1;
        int levelDelta = newLevel - existing.getLevel();
        String oldPrefix = buildDescendantPrefix(existing.getPath(), existing.getId());
        String newPrefix = buildDescendantPrefix(newPath, existing.getId());

        Category updated = Category.reconstitute(existing.getId(), parentId, name, slug, newLevel, newPath,
                sortOrder, status, existing.getCreatedAt(), existing.getUpdatedAt());
        CategoryWithI18n detail = categoryRepository.updateWithRelations(updated, oldPrefix, newPrefix, levelDelta, i18nList);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * upsert 分类多语言
     */
    @Override
    public @NotNull CategoryNode upsertI18n(@NotNull Long categoryId, @NotNull List<CategoryI18n> payloads) {
        categoryRepository.findById(categoryId).orElseThrow(() -> IllegalParamException.of("分类不存在"));
        List<CategoryI18n> normalized = normalizeI18n(payloads, null, categoryId);
        CategoryWithI18n detail = categoryRepository.upsertI18nAndFetch(categoryId, normalized);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 启用或禁用分类
     */
    @Override
    public @NotNull CategoryNode toggleEnable(@NotNull Long categoryId, boolean enabled) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> IllegalParamException.of("分类不存在"));
        CategoryStatus target = enabled ? CategoryStatus.ENABLED : CategoryStatus.DISABLED;
        if (target == category.getStatus()) {
            CategoryWithI18n detail = categoryRepository.findWithI18n(categoryId)
                    .orElse(new CategoryWithI18n(category, List.of()));
            return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
        }
        CategoryWithI18n detail = categoryRepository.updateStatus(category, target);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 规范化关键字
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank())
            return null;
        String trimmed = keyword.strip();
        if (trimmed.length() > NAME_MAX)
            return trimmed.substring(0, NAME_MAX);
        return trimmed;
    }

    /**
     * 规范化名称
     */
    private String normalizeName(String name) {
        requireNotBlank(name, "分类名称不能为空");
        String normalized = name.strip();
        if (normalized.length() > NAME_MAX)
            throw new IllegalParamException("分类名称长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 规范化 slug
     */
    private String normalizeSlug(String slug) {
        requireNotBlank(slug, "分类 slug 不能为空");
        String normalized = slug.strip();
        if (normalized.length() > SLUG_MAX)
            throw new IllegalParamException("分类 slug 长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 规范化品牌
     */
    private String normalizeBrand(String brand) {
        if (brand == null)
            return null;
        String normalized = brand.strip();
        if (normalized.length() > BRAND_MAX)
            throw new IllegalParamException("品牌文案长度不能超过 120 个字符");
        return normalized;
    }

    /**
     * 规范化父级 ID
     */
    private Long normalizeParentId(Long parentId) {
        if (parentId == null)
            return null;
        if (parentId <= 0)
            return null;
        return parentId;
    }

    /**
     * 解析父分类
     */
    private Category resolveParent(Long parentId) {
        if (parentId == null)
            return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> IllegalParamException.of("父分类不存在"));
    }

    /**
     * 校验唯一性
     */
    private void validateUniqueness(String slug, String name, Long parentId, Long excludeId) {
        if (categoryRepository.existsBySlug(slug, excludeId))
            throw new IllegalParamException("分类 slug 已存在");
        if (categoryRepository.existsByParentAndName(parentId, name, excludeId))
            throw new IllegalParamException("同一父分类下名称已存在");
    }

    /**
     * 校验是否形成环
     */
    private void ensureNoCycle(Category current, Category parent) {
        if (parent == null)
            return;
        requireNotNull(current.getId(), "分类 ID 不能为空");
        if (Objects.equals(current.getId(), parent.getId()))
            throw new IllegalParamException("父分类不能是自身");
        String parentPath = normalizePath(parent.getPath());
        if (parentPath.contains("/" + current.getId() + "/"))
            throw new IllegalParamException("父分类不能是当前分类的子节点");
    }

    /**
     * 规范化 locale
     */
    private String normalizeLocale(String locale) {
        requireNotBlank(locale, "语言代码不能为空");
        String normalized = locale.strip();
        if (normalized.length() > 16)
            throw new IllegalParamException("语言代码长度不能超过 16 个字符");
        if (!LOCALE_PATTERN.matcher(normalized).matches())
            throw new IllegalParamException("语言代码格式不合法");
        return normalized;
    }

    /**
     * 规范化 i18n 列表
     */
    private List<CategoryI18n> normalizeI18n(@Nullable List<CategoryI18n> payloads, @Nullable String brandFallback, Long excludeCategoryId) {
        if (payloads == null)
            return List.of();
        Set<String> locales = new LinkedHashSet<>();
        List<CategoryI18n> result = new ArrayList<>();
        for (CategoryI18n payload : payloads) {
            if (payload == null)
                continue;
            String locale = normalizeLocale(payload.getLocale());
            if (!locales.add(locale))
                throw new IllegalParamException("重复的多语言 locale");
            String name = normalizeName(payload.getName());
            String slug = normalizeSlug(payload.getSlug());
            String brand = payload.getBrand();
            if (brand == null)
                brand = brandFallback;
            else if (brand.strip().length() > BRAND_MAX)
                throw new IllegalParamException("品牌文案长度不能超过 120 个字符");
            if (brand != null)
                brand = brand.strip();
            if (categoryRepository.existsLocalizedSlug(locale, slug, excludeCategoryId))
                throw new IllegalParamException("多语言 slug 已存在");
            result.add(CategoryI18n.of(locale, name, slug, brand));
        }
        return result;
    }

    /**
     * 构建路径
     */
    private String buildPath(Category parent) {
        if (parent == null)
            return "/";
        String parentPath = normalizePath(parent.getPath());
        return parentPath + parent.getId() + "/";
    }

    /**
     * 规范化路径
     */
    private String normalizePath(String raw) {
        if (raw == null || raw.isBlank())
            return "/";
        String trimmed = raw.strip();
        if (!trimmed.startsWith("/"))
            trimmed = "/" + trimmed;
        if (!trimmed.endsWith("/"))
            trimmed = trimmed + "/";
        return trimmed.replaceAll("/{2,}", "/");
    }

    /**
     * 子节点路径前缀
     */
    private String buildDescendantPrefix(String path, Long categoryId) {
        String normalized = normalizePath(path);
        return normalized + categoryId + "/";
    }

    /**
     * 解析启用状态
     */
    private CategoryStatus resolveStatus(@Nullable Boolean isEnabled, @NotNull CategoryStatus defaultStatus) {
        if (isEnabled == null)
            return defaultStatus;
        return Boolean.TRUE.equals(isEnabled) ? CategoryStatus.ENABLED : CategoryStatus.DISABLED;
    }
}

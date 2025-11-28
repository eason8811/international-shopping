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

    /**
     * 分类名称最大长度
     */
    private static final int NAME_MAX = 64;
    /**
     * 分类 slug 最大长度
     */
    private static final int SLUG_MAX = 64;
    /**
     * 品牌文案最大长度
     */
    private static final int BRAND_MAX = 120;
    /**
     * 语言代码正则
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");

    /**
     * 分类仓储
     */
    private final IProductCategoryRepository categoryRepository;

    /**
     * 根据给定条件分页查询分类节点
     *
     * @param page           页码, 如果小于等于 0 则默认为 1
     * @param size           每页显示的数量, 如果小于等于 0 或大于 100 则默认为 20
     * @param filterByParent 是否仅返回父级分类下的子分类
     * @param parentId       父分类 ID, 当 filterByParent 为 true 时有效
     * @param keyword        查询关键字, 用于模糊匹配分类名称等信息
     * @param isEnabled      是否仅返回启用状态的分类
     * @return 包含 {@code CategoryNode} 对象列表和总记录数的 {@code PageResult} 实例
     * <p>此方法首先根据传入参数调整分页参数, 并调用 {@code repository} 层进行数据获取, 如果查询结果为空, 则直接返回空列表和总记录数,
     * 否则, 会进一步处理每条分类记录, 将其转换为 {@code CategoryNode} 形式, 同时加载相关的国际化信息, 最终将这些节点与总记录数封装进 {@code PageResult} 返回</p>
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
        // 根据分类 ID 获取 分类ID -> 分类 I18N 映射
        Map<Long, List<CategoryI18n>> i18nMap = ids.isEmpty() ? Map.of() : categoryRepository.mapI18n(ids);
        // 给分类列表中的每个分类都添加 I18N 列表, 而不是进行 I18N 替换
        List<CategoryNode> categoryNodeList = pageResult.items().stream()
                .map(cat -> CategoryNode.from(cat, null, i18nMap.get(cat.getId())))
                .toList();
        return new PageResult<>(categoryNodeList, pageResult.total());
    }

    /**
     * 获取指定ID的分类节点详细信息
     *
     * @param categoryId 分类的唯一标识符, 不能为null
     * @return 返回一个包含分类详情的 {@link CategoryNode} 对象, 不会返回null
     * @throws IllegalParamException 如果提供的 categoryId 对应的分类不存在, 则抛出此异常
     */
    @Override
    public @NotNull CategoryNode detail(@NotNull Long categoryId) {
        CategoryWithI18n detail = categoryRepository.findWithI18n(categoryId)
                .orElseThrow(() -> IllegalParamException.of("分类不存在"));
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 创建一个新的分类节点
     *
     * @param command 包含创建分类所需信息的命令对象, 如名称、slug等
     * @return 新创建的分类节点, 不会为null
     * <p>
     * 该方法首先对传入的<code>CategoryUpsertCommand</code>对象进行处理, 包括标准化分类名称、slug、父分类ID等,
     * 然后根据这些信息构建一个新的<code>Category
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
     *
     * @param categoryId 分类 ID
     * @param command    更新命令
     * @return 更新后的分类
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
     * 批量 upsert 分类多语言
     *
     * @param categoryId 分类 ID
     * @param payloads   多语言列表
     * @return 更新后的分类
     */
    @Override
    public @NotNull CategoryNode upsertI18n(@NotNull Long categoryId, @NotNull List<CategoryI18n> payloads) {
        categoryRepository.findById(categoryId).orElseThrow(() -> IllegalParamException.of("分类不存在"));
        List<CategoryI18n> normalized = normalizeI18n(payloads, null, categoryId);
        CategoryWithI18n detail = categoryRepository.upsertI18nAndFetch(categoryId, normalized);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 启用或停用分类
     *
     * @param categoryId 分类 ID
     * @param enabled    目标状态
     * @return 更新后的分类
     */
    @Override
    public @NotNull CategoryNode toggleEnable(@NotNull Long categoryId, boolean enabled) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> IllegalParamException.of("分类不存在"));
        CategoryStatus targetStatus = enabled ? CategoryStatus.ENABLED : CategoryStatus.DISABLED;
        // 如果目标状态与当前状态相同, 则直接返回当前 CategoryNode
        if (targetStatus == category.getStatus()) {
            CategoryWithI18n detail = categoryRepository.findWithI18n(categoryId)
                    .orElse(new CategoryWithI18n(category, List.of()));
            return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
        }
        CategoryWithI18n detail = categoryRepository.updateStatus(category, targetStatus);
        return CategoryNode.from(detail.getCategory(), null, detail.getI18nList());
    }

    /**
     * 将给定的关键字进行标准化处理 包括去除前后空白字符 并限制其最大长度
     *
     * @param keyword 待处理的关键字 如果为 null 或者只包含空白字符 则返回 null
     * @return 标准化后的关键字 如果原始关键字过长 则会被截断到 <code>NAME_MAX</code> 长度
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
     * 将给定的名称进行规范化处理 包括去除首尾空白字符 并检查长度是否超过限制
     *
     * @param name 需要被规范化的原始名称
     * @return 规范化后的名称 如果名称符合要求
     * @throws IllegalParamException 如果名称为空 或者 名称长度超过 64 个字符
     */
    private String normalizeName(String name) {
        requireNotBlank(name, "分类名称不能为空");
        String normalized = name.strip();
        if (normalized.length() > NAME_MAX)
            throw new IllegalParamException("分类名称长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 将传入的 slug 标签进行规范化处理
     *
     * @param slug 需要被规范化的原始字符串, 该字符串不能为空且长度不能超过 64 个字符
     * @return 返回经过规范化后的字符串, 即去除首尾空白字符后的结果
     * @throws IllegalParamException 当输入的 slug 字符串为空或其长度超过了预设的最大值 (64 个字符) 时抛出
     */
    private String normalizeSlug(String slug) {
        requireNotBlank(slug, "分类 slug 不能为空");
        String normalized = slug.strip();
        if (normalized.length() > SLUG_MAX)
            throw new IllegalParamException("分类 slug 长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * <p>此方法用于规范化品牌名称, 主要包括去除首尾空白字符以及长度检查.</p>
     *
     * @param brand 待处理的品牌名称字符串 如果传入 null, 则直接返回 null
     * @return 处理后的品牌名称字符串 如果原字符串长度超过 120 个字符, 将抛出异常
     * @throws IllegalParamException 当品牌名称长度超过 120 个字符时抛出
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
     * 此方法用于规范化传入的 parentId 参数
     * 如果 parentId 为 null 或者小于等于 0, 则返回 null; 否则返回原始的 parentId 值
     *
     * @param parentId 需要被规范化的父级 ID
     * @return 规范化后的父级 ID, 如果输入不合法 (null 或 <= 0), 返回 null
     */
    private Long normalizeParentId(Long parentId) {
        if (parentId == null)
            return null;
        if (parentId <= 0)
            return null;
        return parentId;
    }

    /**
     * 根据给定的 <code>parentId</code> 查找并返回对应的父分类实体
     * 如果 <code>parentId</code> 为 null 或者找不到对应分类, 则返回 null 或抛出异常
     *
     * @param parentId 父分类的 ID 如果为 null, 方法将直接返回 null
     * @return 返回与 <code>parentId</code> 匹配的 <code>Category</code> 实体, 如果找不到匹配项, 抛出 <code>IllegalParamException</code>
     * @throws IllegalParamException 当根据提供的 <code>parentId</code> 无法找到对应的分类时抛出
     */
    private Category resolveParent(Long parentId) {
        if (parentId == null)
            return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> IllegalParamException.of("父分类不存在"));
    }

    /**
     * 验证给定的 slug 和 name 在指定的 parentId 下是否唯一
     *
     * @param slug      待验证的分类 slug
     * @param name      待验证的分类名称
     * @param parentId  分类所属的父分类 id, 如果是顶级分类则此值可以为 null
     * @param excludeId 排除检查的 id, 通常用于更新操作时排除自身
     * @throws IllegalParamException 当 slug 或 name 不唯一时抛出
     */
    private void validateUniqueness(String slug, String name, Long parentId, Long excludeId) {
        if (categoryRepository.existsBySlug(slug, excludeId))
            throw new IllegalParamException("分类 slug 已存在");
        if (categoryRepository.existsByParentAndName(parentId, name, excludeId))
            throw new IllegalParamException("同一父分类下名称已存在");
    }

    /**
     * 确保给定的分类与其指定的父分类之间不存在循环引用
     *
     * @param current 当前处理的分类, 不能为 null
     * @param parent  作为当前分类父节点的分类, 可以为 null
     * @throws IllegalParamException 如果发现存在循环引用 或者 分类 ID 为空, 则抛出此异常
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
     * 将给定的语言代码转换为标准格式
     *
     * @param locale 语言代码, 必须非空且长度不超过 16 个字符, 符合特定的正则表达式
     * @return 标准化后的语言代码
     * @throws IllegalParamException 如果语言代码为空, 长度过长或格式不正确时抛出
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
     * 对给定的 <code>List<CategoryI18n></code> 进行规范化处理, 确保每个 <code>CategoryI18n</code> 实例符合业务规则要求
     *
     * @param payloadList       需要被规范化的多语言类别信息列表, 可以为 null
     * @param brandFallback     当某个 <code>CategoryI18n</code> 的品牌信息为空时使用的备用品牌名, 可以为 null
     * @param excludeCategoryId 在检查 slug 是否已存在时需要排除的类别 ID, 用于更新操作场景下避免与自身冲突
     * @return 规范化后的多语言类别信息列表, 如果输入为 null 则返回空列表
     * @throws IllegalParamException 如果发现重复的 locale 或者品牌文案长度超过限制或 slug 已经存在抛出此异常
     */
    private List<CategoryI18n> normalizeI18n(@Nullable List<CategoryI18n> payloadList, @Nullable String brandFallback, Long excludeCategoryId) {
        if (payloadList == null)
            return List.of();
        Set<String> locales = new LinkedHashSet<>();
        List<CategoryI18n> result = new ArrayList<>();
        for (CategoryI18n payload : payloadList) {
            if (payload == null)
                continue;
            String locale = normalizeLocale(payload.getLocale());
            if (!locales.add(locale))
                throw new IllegalParamException("重复的多语言 locale: " + locale);
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
                throw new IllegalParamException(locale + " 多语言 slug 已存在");
            result.add(CategoryI18n.of(locale, name, slug, brand));
        }
        return result;
    }

    /**
     * 构建指定类别的路径字符串
     *
     * @param parent 类别的父节点 如果为 null, 则返回根路径 "/"
     * @return 返回构建后的路径字符串 包含父节点的路径和当前节点 id
     */
    private String buildPath(Category parent) {
        if (parent == null)
            return "/";
        String parentPath = normalizePath(parent.getPath());
        return parentPath + parent.getId() + "/";
    }

    /**
     * <p>将给定的路径字符串转换为统一格式, 该方法确保路径以斜杠开始, 以斜杠结束, 并且中间不会有多余的连续斜杠</p>
     *
     * @param raw 待标准化的原始路径字符串 如果为空或仅包含空白字符, 则返回根目录 "/"
     * @return 标准化后的路径字符串 确保路径以斜杠开始和结束, 中间没有重复的斜杠
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
     * 构建后代节点的前缀路径
     *
     * @param path       路径字符串 代表当前节点的位置
     * @param categoryId 类别标识符 用于区分不同类别的节点
     * @return 返回构建好的后代节点前缀路径 包含传入的路径和类别标识符 并以斜杠结尾
     */
    private String buildDescendantPrefix(String path, Long categoryId) {
        String normalized = normalizePath(path);
        return normalized + categoryId + "/";
    }

    /**
     * 根据给定的启用状态和默认状态解析出最终的状态值
     *
     * @param isEnabled     一个布尔值, 表示是否启用, 可以为 null
     * @param defaultStatus 当 isEnabled 为 null 时返回的默认状态, 不能为空
     * @return 返回基于 isEnabled 值决定的 CategoryStatus, 如果 isEnabled 为 null, 则直接返回 defaultStatus; 如果 isEnabled 为 true, 返回 ENABLED 状态; 否则返回 DISABLED 状态
     */
    private CategoryStatus resolveStatus(@Nullable Boolean isEnabled, @NotNull CategoryStatus defaultStatus) {
        if (isEnabled == null)
            return defaultStatus;
        return isEnabled ? CategoryStatus.ENABLED : CategoryStatus.DISABLED;
    }
}

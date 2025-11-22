package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;
import shopping.international.domain.service.products.ICategoryQueryService;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 商品分类查询默认实现
 */
@Service
@RequiredArgsConstructor
public class CategoryQueryService implements ICategoryQueryService {

    /**
     * 分类节点排序: sortOrder 升序, 再按 ID 升序
     */
    private static final Comparator<CategoryNode> NODE_ORDER = Comparator
            .comparingInt(CategoryNode::getSortOrder)
            .thenComparing(CategoryNode::getId);

    /**
     * locale 合法性校验: 仅允许字母数字及横线/下划线, 段长 2-8
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");

    /**
     * 商品分类仓储
     */
    private final IProductCategoryRepository categoryRepository;

    /**
     * 列出启用分类树
     *
     * @param locale 语言代码, 可空
     * @return 根节点列表
     */
    @Override
    public @NotNull List<CategoryNode> tree(@Nullable String locale) {
        String normalizedLocale = normalizeLocale(locale);

        List<Category> enabledCategoryList = categoryRepository.listEnabledCategories();
        Map<Long, CategoryI18n> i18nMap = normalizedLocale == null
                ? Collections.emptyMap()
                : categoryRepository.mapI18nByLocale(normalizedLocale);

        List<Category> sortedCategoryList = enabledCategoryList.stream()
                .sorted(Comparator
                        .comparingInt(Category::getLevel)
                        .thenComparingInt(Category::getSortOrder)
                        .thenComparing(Category::getId))
                .toList();

        // 建立 id -> node 索引
        Map<Long, CategoryNode> index = new LinkedHashMap<>();
        for (Category category : sortedCategoryList) {
            CategoryNode node = CategoryNode.from(category, i18nMap.get(category.getId()));
            index.put(category.getId(), node);
        }

        // 建立父子关系
        List<CategoryNode> roots = new ArrayList<>();
        for (Category category : sortedCategoryList) {
            CategoryNode current = index.get(category.getId());
            Long parentId = category.getParentId();
            // 找不到父节点, 视为根节点
            if (parentId == null) {
                roots.add(current);
                continue;
            }
            CategoryNode parent = index.get(parentId);
            if (parent == null) {
                roots.add(current);
                continue;
            }
            parent.addChild(current);
        }

        roots.sort(NODE_ORDER);
        for (CategoryNode root : roots)
            root.sortChildrenRecursively(NODE_ORDER);
        return roots;
    }

    /**
     * locale 规范化与校验
     *
     * @param locale 请求入参
     * @return 合法 locale, 或 null
     */
    private String normalizeLocale(@Nullable String locale) {
        if (locale == null)
            return null;
        String trimmed = locale.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > 16)
            throw new IllegalParamException("locale 最长 16 个字符");
        if (!LOCALE_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("locale 格式不合法");
        return trimmed;
    }
}

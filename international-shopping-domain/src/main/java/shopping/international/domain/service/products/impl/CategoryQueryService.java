package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryTreeNode;
import shopping.international.domain.service.products.ICategoryQueryService;

import java.util.*;

/**
 * 商品分类查询默认实现
 */
@Service
@RequiredArgsConstructor
public class CategoryQueryService implements ICategoryQueryService {

    /**
     * 分类节点排序: sortOrder 升序, 再按 ID 升序
     */
    private static final Comparator<CategoryTreeNode> NODE_ORDER = Comparator
            .comparingInt(CategoryTreeNode::getSortOrder)
            .thenComparing(CategoryTreeNode::getId);

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
    public @NotNull List<CategoryTreeNode> tree(@Nullable String locale) {
        String normalizedLocale = locale == null || locale.isBlank() ? null : locale;
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
        Map<Long, CategoryTreeNode> index = new LinkedHashMap<>();
        for (Category category : sortedCategoryList) {
            CategoryTreeNode node = CategoryTreeNode.from(category, i18nMap.get(category.getId()));
            index.put(category.getId(), node);
        }

        // 建立父子关系
        List<CategoryTreeNode> roots = new ArrayList<>();
        for (Category category : sortedCategoryList) {
            CategoryTreeNode current = index.get(category.getId());
            Long parentId = category.getParentId();
            // 找不到父节点, 视为根节点
            if (parentId == null) {
                roots.add(current);
                continue;
            }
            CategoryTreeNode parent = index.get(parentId);
            if (parent == null) {
                roots.add(current);
                continue;
            }
            parent.addChild(current);
        }

        roots.sort(NODE_ORDER);
        for (CategoryTreeNode root : roots)
            root.sortChildrenRecursively(NODE_ORDER);
        return roots;
    }
}

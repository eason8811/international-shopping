package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.PublicCategoryNodeRespond;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.service.products.ICategoryService;
import shopping.international.types.constant.SecurityConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户侧分类接口
 *
 * <p>提供分类树查询, 按照请求 locale 进行多语言覆盖</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/products/categories")
public class CategoryController {

    /**
     * 分类领域服务
     */
    private final ICategoryService categoryService;

    /**
     * 查询启用状态的分类树 (已做多语言覆盖)
     *
     * @param locale 请求的语言代码, 可空
     * @return 分类树
     */
    @GetMapping("/tree")
    public ResponseEntity<Result<List<PublicCategoryNodeRespond>>> tree(
            @RequestParam(value = "locale", required = false) @Nullable String locale) {
        requireNotBlank(locale, "locale 不能为空");
        String normalizedLocale = normalizeLocale(locale);
        List<Category> categories = categoryService.listEnabled();
        List<PublicCategoryNodeRespond> nodes = categories.stream()
                .map(category -> toPublicNode(category, normalizedLocale))
                .collect(Collectors.toList());
        List<PublicCategoryNodeRespond> tree = buildTree(nodes);
        return ResponseEntity.ok(Result.ok(tree));
    }

    /**
     * 将聚合转为用户侧节点, 并按 locale 覆盖名称/slug/品牌
     *
     * @param category 聚合
     * @param locale   语言代码
     * @return 响应节点
     */
    private PublicCategoryNodeRespond toPublicNode(Category category, @Nullable String locale) {
        CategoryI18n localized = findI18n(category, locale);
        String name = getI18nOrDefault(localized, CategoryI18n::getName, category.getName());
        String slug = getI18nOrDefault(localized, CategoryI18n::getSlug, category.getSlug());
        String brand = getI18nOrDefault(localized, CategoryI18n::getBrand, category.getBrand());
        return PublicCategoryNodeRespond.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(name)
                .slug(slug)
                .level(category.getLevel())
                .path(category.getPath())
                .sortOrder(category.getSortOrder())
                .brand(brand)
                .children(new ArrayList<>())
                .isEnabled(category.getStatus() == CategoryStatus.ENABLED)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .locale(locale)
                .build();
    }

    /**
     * 按 locale 查找匹配的多语言
     *
     * @param category 聚合
     * @param locale   目标 locale
     * @return 匹配项或 null
     */
    private CategoryI18n findI18n(Category category, @Nullable String locale) {
        if (locale == null)
            return null;
        return category.getI18nList().stream()
                .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据父子关系构造树形结构
     *
     * @param flatNodes 扁平节点列表
     * @return 树形节点列表
     */
    private List<PublicCategoryNodeRespond> buildTree(List<PublicCategoryNodeRespond> flatNodes) {
        Map<Long, PublicCategoryNodeRespond> map = new HashMap<>();
        for (PublicCategoryNodeRespond node : flatNodes) {
            map.put(node.getId(), node);
        }
        List<PublicCategoryNodeRespond> roots = new ArrayList<>();
        for (PublicCategoryNodeRespond node : flatNodes) {
            if (node.getParentId() == null) {
                roots.add(node);
                continue;
            }
            PublicCategoryNodeRespond parent = map.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            if (parent.getChildren() == null)
                parent.setChildren(new ArrayList<>());
            parent.getChildren().add(node);

        }
        return roots;
    }
}

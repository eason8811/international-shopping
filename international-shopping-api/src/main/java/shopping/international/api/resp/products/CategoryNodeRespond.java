package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;

import java.util.List;

/**
 * 商品分类树响应节点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryNodeRespond {
    /**
     * 分类 ID
     */
    private Long id;
    /**
     * 父分类节点 ID
     */
    private Long parentId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 分类别名
     */
    private String slug;
    /**
     * 分类层级
     */
    private Integer level;
    /**
     * 分类路径
     */
    private String path;
    /**
     * 分类排序
     */
    private Integer sortOrder;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 当前展示语言 (若使用了 i18n 覆盖)
     */
    private String locale;
    /**
     * i18n 覆盖部分
     */
    private CategoryI18nRespond i18n;
    /**
     * 子分类节点
     */
    private List<CategoryNodeRespond> children;

    /**
     * 从领域层节点转换为响应节点
     *
     * @param node 领域层分类节点
     * @return 响应节点
     */
    public static CategoryNodeRespond from(CategoryNode node) {
        CategoryI18nRespond i18nResp = null;
        CategoryI18n i18nVo = node.getI18n();
        if (i18nVo != null)
            i18nResp = new CategoryI18nRespond(i18nVo.getName(), i18nVo.getSlug(), i18nVo.getBrand());

        List<CategoryNodeRespond> childResponds = node.getChildren().stream()
                .map(CategoryNodeRespond::from)
                .toList();

        return new CategoryNodeRespond(
                node.getId(),
                node.getParentId(),
                node.getName(),
                node.getSlug(),
                node.getLevel(),
                node.getPath(),
                node.getSortOrder(),
                node.getBrand(),
                node.getLocale(),
                i18nResp,
                childResponds
        );
    }

    /**
     * i18n 覆盖部分
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryI18nRespond {
        /**
         * 分类名称
         */
        private String name;
        /**
         * 分类别名
         */
        private String slug;
        /**
         * 品牌文案
         */
        private String brand;
    }
}

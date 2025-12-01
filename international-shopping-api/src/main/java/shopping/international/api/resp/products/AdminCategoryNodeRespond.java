package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 商品分类树响应节点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryNodeRespond {
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
     * 全量 i18n 列表
     */
    private List<CategoryI18nPayloadRespond> i18nList;
    /**
     * 子分类节点
     */
    private List<AdminCategoryNodeRespond> children;
    /**
     * 是否启用
     */
    private Boolean isEnabled;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从领域层节点转换为响应节点
     *
     * @param node 领域层分类节点
     * @return 响应节点
     */
    public static AdminCategoryNodeRespond from(CategoryNode node) {
        List<CategoryI18nPayloadRespond> i18nListResponds = node.getI18nList().stream()
                .map(CategoryI18nPayloadRespond::from)
                .toList();
        String displayBrand = node.getBrand();
        // 若没有指定当前语言的品牌文案，则使用 i18n 列表中第一个非空的品牌文案
        if (displayBrand == null)
            displayBrand = i18nListResponds.stream()
                    .map(CategoryI18nPayloadRespond::getBrand)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

        List<AdminCategoryNodeRespond> childNodeList = node.getChildren().stream()
                .map(AdminCategoryNodeRespond::from)
                .toList();

        return new AdminCategoryNodeRespond(
                node.getId(),
                node.getParentId(),
                node.getName(),
                node.getSlug(),
                node.getLevel(),
                node.getPath(),
                node.getSortOrder(),
                displayBrand,
                i18nListResponds,
                childNodeList,
                node.getStatus() == null ? null : node.getStatus() == CategoryStatus.ENABLED,
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    /**
     * 带 locale 的 i18n 响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryI18nPayloadRespond {
        /**
         * 语言代码
         */
        private String locale;
        /**
         * 分类名称
         */
        private String name;
        /**
         * 分类 slug
         */
        private String slug;
        /**
         * 品牌文案
         */
        private String brand;

        /**
         * 从领域对象转换为响应对象
         *
         * @param vo 值对象
         * @return 响应
         */
        public static CategoryI18nPayloadRespond from(CategoryI18n vo) {
            return new CategoryI18nPayloadRespond(vo.getLocale(), vo.getName(), vo.getSlug(), vo.getBrand());
        }
    }
}

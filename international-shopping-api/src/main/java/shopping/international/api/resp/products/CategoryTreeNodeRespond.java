package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryTreeNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧分类树节点响应 CategoryTreeNodeRespond
 *
 * <p>返回已经按 locale 做过本地化替换的分类节点, 不包含多语言列表</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNodeRespond {
    /**
     * 分类 ID
     */
    private Long id;
    /**
     * 父级分类 ID
     */
    private Long parentId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 分类 slug
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
     * 排序权重
     */
    private Integer sortOrder;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 当前返回语言
     */
    private String locale;
    /**
     * 子节点列表
     */
    private List<CategoryTreeNodeRespond> children;
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
     * 将领域层分类树节点转换为响应对象
     *
     * @param node 领域层分类树节点
     * @return 分类树节点响应
     */
    public static CategoryTreeNodeRespond from(CategoryTreeNode node) {
        List<CategoryTreeNodeRespond> childNodes = node.getChildren()
                .stream()
                .map(CategoryTreeNodeRespond::from)
                .toList();
        return new CategoryTreeNodeRespond(
                node.getId(),
                node.getParentId(),
                node.getName(),
                node.getSlug(),
                node.getLevel(),
                node.getPath(),
                node.getSortOrder(),
                node.getBrand(),
                node.getLocale(),
                childNodes,
                node.getStatus() == null ? null : node.getStatus() == CategoryStatus.ENABLED,
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }
}

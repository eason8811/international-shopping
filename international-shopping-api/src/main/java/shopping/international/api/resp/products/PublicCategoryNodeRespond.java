package shopping.international.api.resp.products;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryTreeNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧分类树节点响应 PublicCategoryNodeRespond
 *
 * <p>返回已经按 locale 做过本地化替换的分类节点, 不包含多语言列表</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicCategoryNodeRespond extends AbstractCategoryNodeRespond {
    /**
     * 当前返回语言
     */
    private String locale;

    /**
     * 构造一个 PublicCategoryNodeRespond 对象, 该对象用于表示用户侧分类树节点的响应
     *
     * @param id        节点 ID
     * @param parentId  父节点 ID
     * @param name      节点名称
     * @param slug      节点别名
     * @param level     节点层级
     * @param path      节点路径
     * @param sortOrder 节点排序号
     * @param brand     品牌信息
     * @param children  子节点列表
     * @param isEnabled 是否启用
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param locale    当前返回的语言代码
     */
    public PublicCategoryNodeRespond(Long id, Long parentId, String name, String slug, Integer level, String path, Integer sortOrder, String brand, List<PublicCategoryNodeRespond> children, Boolean isEnabled, LocalDateTime createdAt, LocalDateTime updatedAt, String locale) {
        super(id, parentId, name, slug, level, path, sortOrder, brand, children, isEnabled, createdAt, updatedAt);
        this.locale = locale;
    }

    /**
     * 将领域层分类树节点转换为响应对象
     *
     * @param node 领域层分类树节点
     * @return 分类树节点响应
     */
    public static PublicCategoryNodeRespond from(CategoryTreeNode node) {
        List<PublicCategoryNodeRespond> childNodes = node.getChildren()
                .stream()
                .map(PublicCategoryNodeRespond::from)
                .toList();
        return new PublicCategoryNodeRespond(
                node.getId(),
                node.getParentId(),
                node.getName(),
                node.getSlug(),
                node.getLevel(),
                node.getPath(),
                node.getSortOrder(),
                node.getBrand(),
                childNodes,
                node.getStatus() == null ? null : node.getStatus() == CategoryStatus.ENABLED,
                node.getCreatedAt(),
                node.getUpdatedAt(),
                node.getLocale()
        );
    }
}

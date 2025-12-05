package shopping.international.api.resp.products;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抽象分类节点响应类
 */
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractCategoryNodeRespond {
    /**
     * 分类 ID
     */
    protected Long id;
    /**
     * 父分类节点 ID
     */
    protected Long parentId;
    /**
     * 分类名称
     */
    protected String name;
    /**
     * 分类别名
     */
    protected String slug;
    /**
     * 分类层级
     */
    protected Integer level;
    /**
     * 分类路径
     */
    protected String path;
    /**
     * 分类排序
     */
    protected Integer sortOrder;
    /**
     * 品牌文案
     */
    protected String brand;
    /**
     * 子分类节点
     */
    protected List<? extends AbstractCategoryNodeRespond> children;
    /**
     * 是否启用
     */
    protected Boolean isEnabled;
    /**
     * 创建时间
     */
    protected LocalDateTime createdAt;
    /**
     * 更新时间
     */
    protected LocalDateTime updatedAt;
}

package shopping.international.infrastructure.dao.products.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品分类持久化对象, 对应表 product_category
 * <p>记录商品分类树的结构信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_category")
public class ProductCategoryPO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 父分类ID, 根节点为空
     */
    @Column("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @Column("name")
    private String name;

    /**
     * 分类别名, 用于 SEO 或路由
     */
    @Column("slug")
    private String slug;

    /**
     * 层级, 根节点从 1 开始
     */
    @Column("level")
    private Integer level;

    /**
     * 祖先路径, 如 /1/3/5/
     */
    @Column("path")
    private String path;

    /**
     * 排序, 值越小越靠前
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 启用状态, ENABLED 或 DISABLED
     */
    @Column("status")
    private String status;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;
}

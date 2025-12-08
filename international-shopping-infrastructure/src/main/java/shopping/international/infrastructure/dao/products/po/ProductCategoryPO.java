package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类持久化对象, 对应表 product_category
 * <p>记录商品分类树的结构信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_category")
public class ProductCategoryPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父分类ID, 根节点为空
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 分类别名, 用于 SEO 或路由
     */
    @TableField("slug")
    private String slug;

    /**
     * 层级, 根节点从 1 开始
     */
    @TableField("level")
    private Integer level;

    /**
     * 祖先路径, 如 /1/3/5/
     */
    @TableField("path")
    private String path;

    /**
     * 排序, 值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 启用状态, ENABLED 或 DISABLED
     */
    @TableField("status")
    private String status;

    /**
     * 分类 I18N 本地化列表
     */
    @TableField(exist = false)
    private List<ProductCategoryI18nPO> i18nList;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

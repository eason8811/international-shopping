package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_category")
public class ProductCategoryPO {
    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 父分类 ID
     */
    @TableField("parent_id")
    private Long parentId;
    /**
     * 分类名称
     */
    @TableField("name")
    private String name;
    /**
     * 分类别名
     */
    @TableField("slug")
    private String slug;
    /**
     * 分类级别
     */
    @TableField("level")
    private Integer level;
    /**
     * 分类路径
     */
    @TableField("path")
    private String path;
    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
    /**
     * 状态
     */
    @TableField("status")
    private String status;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

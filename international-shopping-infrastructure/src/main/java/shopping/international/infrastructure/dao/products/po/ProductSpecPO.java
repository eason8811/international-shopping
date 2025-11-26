package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_spec
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec")
public class ProductSpecPO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SPU ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;
    /**
     * 类别编码(稳定): color / capacity
     */
    @TableField("spec_code")
    private String specCode;
    /**
     * 类别名称
     */
    @TableField("spec_name")
    private String specName;
    /**
     * 类别类型(用于UI渲染/业务规则)
     */
    @TableField("spec_type")
    private String specType;
    /**
     * 是否必选(每个SKU必须选择一个值)
     */
    @TableField("is_required")
    private Integer isRequired;
    /**
     * 排序(小在前)
     */
    @TableField("sort_order")
    private Integer sortOrder;
    /**
     * 启用状态
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

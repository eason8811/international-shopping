package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_spec_value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec_value")
public class ProductSpecValuePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("spec_id")
    private Long specId;
    @TableField("value_code")
    private String valueCode;
    @TableField("value_name")
    private String valueName;
    @TableField("attributes")
    private String attributes;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("status")
    private String status;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

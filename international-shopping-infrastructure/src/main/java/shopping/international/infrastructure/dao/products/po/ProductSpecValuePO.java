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
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SPU ID, 指向 product.id(冗余, 便于校验与查询)
     */
    @TableField("product_id")
    private Long productId;
    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @TableField("spec_id")
    private Long specId;
    /**
     * 值编码(稳定): black / gray / 512gb
     */
    @TableField("value_code")
    private String valueCode;
    /**
     * 值名称: 黑色 / 灰色 / 512GB
     */
    @TableField("value_name")
    private String valueName;
    /**
     * 附加属性: 如颜色hex、展示图等 (JSON)
     */
    @TableField("attributes")
    private String attributes;
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

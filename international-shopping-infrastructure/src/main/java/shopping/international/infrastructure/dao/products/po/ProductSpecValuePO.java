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
 * 规格值持久化对象, 对应表 product_spec_value
 * <p>记录规格类别下的可选值及其属性</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec_value")
public class ProductSpecValuePO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID, 指向 product.id (冗余便于校验与查询)
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @TableField("spec_id")
    private Long specId;

    /**
     * 规格值编码, 如 black, gray, 512gb
     */
    @TableField("value_code")
    private String valueCode;

    /**
     * 规格值名称
     */
    @TableField("value_name")
    private String valueName;

    /**
     * 附加属性 JSON, 如颜色 hex 或展示图等
     */
    @TableField("attributes")
    private String attributes;

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
     * 分类取值 I18N 本地化列表
     */
    @TableField(exist = false)
    private List<ProductSpecValueI18nPO> i18nList;

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

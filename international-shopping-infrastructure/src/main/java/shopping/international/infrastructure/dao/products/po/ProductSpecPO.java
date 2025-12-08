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
 * 商品规格类别持久化对象, 对应表 product_spec
 * <p>定义商品下的规格类别及其属性</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec")
public class ProductSpecPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 规格编码, 如 color、capacity
     */
    @TableField("spec_code")
    private String specCode;

    /**
     * 规格名称, 如 颜色、容量
     */
    @TableField("spec_name")
    private String specName;

    /**
     * 规格类型：COLOR、SIZE、CAPACITY、MATERIAL 或 OTHER
     */
    @TableField("spec_type")
    private String specType;

    /**
     * 是否必选, 决定每个 SKU 是否必须有该规格值
     */
    @TableField("is_required")
    private Boolean isRequired;

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
     * 规格 I18N 本地化信息列表
     */
    private List<ProductSpecI18nPO> i18nList;

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

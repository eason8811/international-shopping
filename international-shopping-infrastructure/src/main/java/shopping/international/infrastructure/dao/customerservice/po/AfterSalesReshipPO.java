package shopping.international.infrastructure.dao.customerservice.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象, aftersales_reship 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("aftersales_reship")
public class AfterSalesReshipPO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 补发单号
     */
    @TableField("reship_no")
    private String reshipNo;
    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 工单 ID
     */
    @TableField("ticket_id")
    private Long ticketId;
    /**
     * 原物流单 ID
     */
    @TableField("shipment_id")
    private Long shipmentId;
    /**
     * 补发原因编码
     */
    @TableField("reason_code")
    private String reasonCode;
    /**
     * 补发状态
     */
    @TableField("status")
    private String status;
    /**
     * 币种代码
     */
    @TableField("currency")
    private String currency;
    /**
     * 货品成本, Minor 形式
     */
    @TableField("items_cost")
    private Long itemsCost;
    /**
     * 运费成本, Minor 形式
     */
    @TableField("shipping_cost")
    private Long shippingCost;
    /**
     * 备注
     */
    @TableField("note")
    private String note;
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

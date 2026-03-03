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
 * 持久化对象, aftersales_reship_shipment 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("aftersales_reship_shipment")
public class AfterSalesReshipShipmentPO {

    /**
     * 补发单 ID
     */
    @TableId(value = "reship_id", type = IdType.INPUT)
    private Long reshipId;
    /**
     * 物流单 ID
     */
    @TableField("shipment_id")
    private Long shipmentId;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

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
 * 持久化对象, aftersales_reship_item 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "amount"})
@TableName("aftersales_reship_item")
public class AfterSalesReshipItemPO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 补发单 ID
     */
    @TableField("reship_id")
    private Long reshipId;
    /**
     * 原订单明细 ID
     */
    @TableField("order_item_id")
    private Long orderItemId;
    /**
     * SKU ID
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 补发数量
     */
    @TableField("quantity")
    private Integer quantity;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 补发明细金额, 由查询 SQL 计算填充, Minor 形式
     */
    @TableField(exist = false)
    private Long amount;
}

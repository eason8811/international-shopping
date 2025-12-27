package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: order_status_log
 *
 * <p>订单状态流转日志表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("order_status_log")
public class OrderStatusLogPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 事件来源
     */
    @TableField("event_source")
    private String eventSource;
    /**
     * 变更前状态
     */
    @TableField("from_status")
    private String fromStatus;
    /**
     * 变更后状态
     */
    @TableField("to_status")
    private String toStatus;
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
}


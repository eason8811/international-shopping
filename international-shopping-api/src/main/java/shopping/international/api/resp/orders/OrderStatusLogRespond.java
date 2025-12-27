package shopping.international.api.resp.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志响应 (OrderStatusLogRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusLogRespond {
    /**
     * 日志 ID
     */
    private Long id;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 事件来源
     */
    private OrderStatusEventSource eventSource;
    /**
     * 变更前状态 (可为空)
     */
    private OrderStatus fromStatus;
    /**
     * 变更后状态
     */
    private OrderStatus toStatus;
    /**
     * 备注 (可为空)
     */
    private String note;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}


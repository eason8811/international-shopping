package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 订单状态流转日志实体 (对应表 order_status_log)
 *
 * <p>用于审计与回放, 建议以追加写为主。</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OrderStatusLog implements Verifiable {
    /**
     * 主键ID (可为 {@code null}, 表示尚未持久化)
     */
    private Long id;
    /**
     * 订单ID, 指向 {@code orders.id}
     */
    private Long orderId;
    /**
     * 事件来源 (系统/用户/支付回调/调度/管理侧)
     */
    private OrderStatusEventSource eventSource;
    /**
     * 源状态 (首条日志可为空)
     */
    @Nullable
    private OrderStatus fromStatus;
    /**
     * 目标状态
     */
    private OrderStatus toStatus;
    /**
     * 备注 (可空, 最长 255)
     */
    @Nullable
    private String note;
    /**
     * 创建时间 (写入时间)
     */
    private LocalDateTime createdAt;

    /**
     * 创建一条新的状态流转日志 (用于追加写入)
     *
     * @param orderId      订单ID
     * @param eventSource  事件来源
     * @param fromStatus   源状态 (首条日志可为空)
     * @param toStatus     目标状态
     * @param note         备注, 可空
     * @return 新建的 {@link OrderStatusLog} 实体, {@code id} 为空表示未持久化
     */
    public static OrderStatusLog create(Long orderId, OrderStatusEventSource eventSource,
                                        @Nullable OrderStatus fromStatus, OrderStatus toStatus,
                                        @Nullable String note) {
        return new OrderStatusLog(null, orderId, eventSource, fromStatus, toStatus,
                note == null ? null : note.strip(), LocalDateTime.now());
    }

    /**
     * 校验当前日志实体字段是否满足基本不变式
     *
     * <ul>
     *     <li>{@code orderId/eventSource/toStatus/createdAt} 必填</li>
     *     <li>{@code note} 若不为空, 长度不得超过 255</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "订单 ID 不能为空");
        requireNotNull(eventSource, "事件来源不能为空");
        requireNotNull(toStatus, "目标状态不能为空");
        if (note != null)
            require(note.length() <= 255, "备注最长 255 个字符");
        requireNotNull(createdAt, "创建时间不能为空");
    }
}

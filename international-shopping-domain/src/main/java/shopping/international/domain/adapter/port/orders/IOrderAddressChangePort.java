package shopping.international.domain.adapter.port.orders;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.orders.OrderNo;

import java.time.Duration;

/**
 * 订单“仅一次改址”标记端口
 *
 * <p>职责:</p>
 * <ul>
 *     <li>基于外部存储 (如 Redis) 维护订单是否已改址的标记</li>
 *     <li>提供原子性的“抢占改址权”能力, 用于并发下保证仅一次改址语义</li>
 *     <li>在订单达到终态 (如 CANCELLED / CLOSED / REFUNDED) 后清理标记</li>
 * </ul>
 */
public interface IOrderAddressChangePort {

    /**
     * 尝试为指定订单标记“已改址”
     *
     * <p>并发语义:</p>
     * <ul>
     *     <li>首个调用方抢占成功返回 {@code true}</li>
     *     <li>后续并发调用方返回 {@code false}</li>
     * </ul>
     *
     * @param orderNo 订单号, 不能为空
     * @param ttl     标记的存活时间, 不能为空
     * @return 是否抢占成功
     */
    boolean tryMarkChanged(@NotNull OrderNo orderNo, @NotNull Duration ttl);

    /**
     * 判断指定订单是否已改址
     *
     * @param orderNo 订单号, 不能为空
     * @return 若已改址返回 {@code true}, 否则返回 {@code false}
     */
    boolean isChanged(@NotNull OrderNo orderNo);

    /**
     * 清理指定订单的改址标记
     *
     * @param orderNo 订单号, 不能为空
     */
    void clear(@NotNull OrderNo orderNo);
}


package shopping.international.infrastructure.adapter.port.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.orders.IOrderAddressChangePort;
import shopping.international.domain.model.vo.orders.OrderNo;

import java.time.Duration;

/**
 * 基于 Redis 的订单“仅一次改址”标记实现
 *
 * <p>键模式: {@code orders:addr_changed:{orderNo}}</p>
 */
@Component
@RequiredArgsConstructor
public class RedisOrderAddressChangePort implements IOrderAddressChangePort {

    /**
     * Redis Key 前缀
     */
    private static final String KEY_PREFIX = "orders:addr_changed:";

    /**
     * 标记值 (无业务含义, 仅用于占位)
     */
    private static final String VALUE_CHANGED = "1";

    /**
     * Redis 字符串模板
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试为指定订单标记“已改址”
     *
     * @param orderNo 订单号
     * @param ttl     标记的存活时间
     * @return 是否抢占成功
     */
    @Override
    public boolean tryMarkChanged(@NotNull OrderNo orderNo, @NotNull Duration ttl) {
        String key = buildKey(orderNo);
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, VALUE_CHANGED, ttl);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 判断指定订单是否已改址
     *
     * @param orderNo 订单号
     * @return 是否已改址
     */
    @Override
    public boolean isChanged(@NotNull OrderNo orderNo) {
        String key = buildKey(orderNo);
        String value = redisTemplate.opsForValue().get(key);
        return value != null;
    }

    /**
     * 清理指定订单的改址标记
     *
     * @param orderNo 订单号
     */
    @Override
    public void clear(@NotNull OrderNo orderNo) {
        String key = buildKey(orderNo);
        redisTemplate.delete(key);
    }

    /**
     * 构造 Redis Key
     *
     * @param orderNo 订单号
     * @return Key 字符串
     */
    private static String buildKey(@NotNull OrderNo orderNo) {
        return KEY_PREFIX + orderNo.getValue();
    }
}


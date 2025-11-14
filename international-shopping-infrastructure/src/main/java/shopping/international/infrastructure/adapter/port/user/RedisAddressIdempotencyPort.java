package shopping.international.infrastructure.adapter.port.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.user.IAddressIdempotencyPort;

import java.time.Duration;

/**
 * 基于 Redis 的用户地址创建幂等性适配器实现
 *
 * <p>键模式: {@code user:addr:idemp:{userId}:{idempotencyKey}}</p>
 *
 * <p>值约定:</p>
 * <ul>
 *     <li>{@code PENDING} - 表示已有请求获得创建权, 但尚未完成创建流程</li>
 *     <li>{@code OK:{addressId}} - 表示已成功创建地址并绑定到该幂等键</li>
 * </ul>
 *
 * <p>并发语义:</p>
 * <ul>
 *     <li>使用 Redis {@code SET key value NX EX ttl} 保证只有第一个请求能够抢占创建权</li>
 *     <li>后续请求会观察到 {@code PENDING} 或 {@code OK:{addressId}}, 从而实现幂等</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RedisAddressIdempotencyPort implements IAddressIdempotencyPort {

    /**
     * Redis Key 前缀: user:addr:idemp:
     */
    private static final String KEY_PREFIX = "user:addr:idemp:";

    /**
     * 表示“请求处理中”的占位字符串
     */
    private static final String VALUE_PENDING = "PENDING";

    /**
     * 表示“已成功绑定地址”的值前缀, 实际完整值为 {@code OK:{addressId}}
     */
    private static final String VALUE_PREFIX_OK = "OK:";

    /**
     * Redis 字符串模板, 用于执行字符串键值操作
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试为给定用户和幂等键注册一个"地址创建幂等 Token", 或返回已有结果
     *
     * <p>语义说明:</p>
     * <ul>
     *     <li>若 Redis 中不存在该 key, 则以"PENDING"占位写入并返回 {@link TokenStatus.Status#NEW NEW}</li>
     *     <li>若已存在且值为 {@code PENDING}, 则返回 {@link TokenStatus.Status#IN_PROGRESS IN_PROGRESS}</li>
     *     <li>若已存在且值为 {@code OK:{addressId}}, 则返回 {@link TokenStatus.Status#SUCCEEDED SUCCEEDED} 并携带 addressId</li>
     * </ul>
     *
     * @param userId         用户 ID, 用于与幂等键一起构成 Redis key 命名空间
     * @param idempotencyKey 幂等键, 通常来自 HTTP 头 {@code Idempotency-Key}
     * @param ttl            PENDING 状态在 Redis 中的存活时间, 到期后允许新的创建请求重新获得创建权
     * @return 当前幂等键对应的状态
     */
    @Override
    public @NotNull TokenStatus registerOrGet(@NotNull Long userId, @NotNull String idempotencyKey, @NotNull Duration ttl) {
        String key = buildKey(userId, idempotencyKey);

        // 1) 尝试占位: 仅首个请求能够成功 (SET NX)
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, VALUE_PENDING, ttl);
        if (Boolean.TRUE.equals(ok))
            // 当前调用获得创建权
            return new TokenStatus(TokenStatus.Status.NEW, null);


        // 2) 已有记录, 读取当前值
        String value = redisTemplate.opsForValue().get(key);

        // 2.1 极端情况下, 占位已过期或被清理, 允许重新占位一次
        if (value == null) {
            Boolean second = redisTemplate.opsForValue().setIfAbsent(key, VALUE_PENDING, ttl);
            if (Boolean.TRUE.equals(second))
                return new TokenStatus(TokenStatus.Status.NEW, null);

            value = redisTemplate.opsForValue().get(key);
        }

        // 2.2 若仍然为空, 保守视为“处理中”
        if (value == null)
            return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);

        // 2.3 处理中态
        if (VALUE_PENDING.equals(value))
            return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);

        // 2.4 成功态: OK:{addressId}
        if (value.startsWith(VALUE_PREFIX_OK)) {
            String idStr = value.substring(VALUE_PREFIX_OK.length());
            try {
                Long addressId = Long.parseLong(idStr);
                return new TokenStatus(TokenStatus.Status.SUCCEEDED, addressId);
            } catch (NumberFormatException ex) {
                // 值内容被污染, 保险起见降级为“处理中”
                return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
            }
        }

        // 2.5 其他未知值, 也视为“处理中”
        return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
    }

    /**
     * 在地址创建成功后, 将指定地址 ID 与幂等键进行绑定
     *
     * <p>该操作通常在数据库事务成功提交后调用, 用于将占位值 {@code PENDING} 覆盖为 {@code OK:{addressId}},
     * 以便后续重复请求能够直接返回已创建的地址结果</p>
     *
     * @param userId         用户 ID
     * @param idempotencyKey 幂等键, 必须与 {@link #registerOrGet(Long, String, Duration)} 中使用的值保持一致
     * @param addressId      新创建的地址主键 ID
     * @param ttl            成功态在 Redis 中的存活时间, 通常可以比 PENDING 态更长
     */
    @Override
    public void markSucceeded(@NotNull Long userId, @NotNull String idempotencyKey, @NotNull Long addressId, @NotNull Duration ttl) {
        String key = buildKey(userId, idempotencyKey);
        String value = VALUE_PREFIX_OK + addressId;
        // 直接覆盖占位值, 并重置 TTL 为成功态 TTL
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 构造 Redis Key
     *
     * @param userId         用户 ID
     * @param idempotencyKey 幂等键
     * @return Redis 键字符串
     */
    private static String buildKey(@NotNull Long userId, @NotNull String idempotencyKey) {
        return KEY_PREFIX + userId + ":" + idempotencyKey;
    }
}

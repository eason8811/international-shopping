package shopping.international.infrastructure.adapter.port.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;

import java.time.Duration;

/**
 * 工单写操作幂等端口实现, 基于 Redis 字符串 KV
 */
@Component
@RequiredArgsConstructor
public class TicketIdempotencyPort implements ITicketIdempotencyPort {

    /**
     * 创建工单幂等键前缀
     */
    private static final String CREATE_KEY_PREFIX = "cs:ticket:idemp:create:";
    /**
     * 关闭工单幂等键前缀
     */
    private static final String CLOSE_KEY_PREFIX = "cs:ticket:idemp:close:";
    /**
     * 请求处理中标记值
     */
    private static final String VALUE_PENDING = "PENDING";
    /**
     * 请求成功标记值前缀
     */
    private static final String VALUE_OK_PREFIX = "OK:";

    /**
     * Redis 字符串模板
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 注册或查询创建工单幂等令牌
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @Override
    public @NotNull TokenStatus registerCreateOrGet(@NotNull Long userId,
                                                    @NotNull String idempotencyKey,
                                                    @NotNull Duration ttl) {
        String key = CREATE_KEY_PREFIX + userId + ":" + idempotencyKey;
        return registerOrGet(key, ttl);
    }

    /**
     * 标记创建工单幂等令牌成功
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @param ticketNo        工单编号
     * @param ttl             成功状态 TTL
     */
    @Override
    public void markCreateSucceeded(@NotNull Long userId,
                                    @NotNull String idempotencyKey,
                                    @NotNull String ticketNo,
                                    @NotNull Duration ttl) {
        String key = CREATE_KEY_PREFIX + userId + ":" + idempotencyKey;
        redisTemplate.opsForValue().set(key, VALUE_OK_PREFIX + ticketNo, ttl);
    }

    /**
     * 注册或查询关闭工单幂等令牌
     *
     * @param userId          用户 ID
     * @param ticketNo        工单编号
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @Override
    public @NotNull TokenStatus registerCloseOrGet(@NotNull Long userId,
                                                   @NotNull String ticketNo,
                                                   @NotNull String idempotencyKey,
                                                   @NotNull Duration ttl) {
        String key = CLOSE_KEY_PREFIX + userId + ":" + ticketNo + ":" + idempotencyKey;
        return registerOrGet(key, ttl);
    }

    /**
     * 标记关闭工单幂等令牌成功
     *
     * @param userId          用户 ID
     * @param ticketNo        工单编号
     * @param idempotencyKey  幂等键
     * @param ttl             成功状态 TTL
     */
    @Override
    public void markCloseSucceeded(@NotNull Long userId,
                                   @NotNull String ticketNo,
                                   @NotNull String idempotencyKey,
                                   @NotNull Duration ttl) {
        String key = CLOSE_KEY_PREFIX + userId + ":" + ticketNo + ":" + idempotencyKey;
        redisTemplate.opsForValue().set(key, VALUE_OK_PREFIX + ticketNo, ttl);
    }

    /**
     * 注册幂等键或读取已有幂等状态
     *
     * @param key Redis Key
     * @param ttl 占位状态 TTL
     * @return 幂等状态
     */
    private @NotNull TokenStatus registerOrGet(@NotNull String key, @NotNull Duration ttl) {
        Boolean setResult = redisTemplate.opsForValue().setIfAbsent(key, VALUE_PENDING, ttl);
        if (Boolean.TRUE.equals(setResult))
            return new TokenStatus(TokenStatus.Status.NEW, null);

        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            Boolean retrySetResult = redisTemplate.opsForValue().setIfAbsent(key, VALUE_PENDING, ttl);
            if (Boolean.TRUE.equals(retrySetResult))
                return new TokenStatus(TokenStatus.Status.NEW, null);
            value = redisTemplate.opsForValue().get(key);
        }

        if (value == null || VALUE_PENDING.equals(value))
            return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);

        if (value.startsWith(VALUE_OK_PREFIX)) {
            String ticketNo = value.substring(VALUE_OK_PREFIX.length());
            if (ticketNo.isBlank())
                return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
            return new TokenStatus(TokenStatus.Status.SUCCEEDED, ticketNo);
        }
        return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
    }
}

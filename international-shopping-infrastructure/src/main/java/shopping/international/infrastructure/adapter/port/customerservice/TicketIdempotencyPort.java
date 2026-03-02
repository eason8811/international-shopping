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
     * 工单幂等键总前缀
     */
    private static final String KEY_PREFIX = "cs:ticket:idemp:";
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
        return registerActionOrGet("create", userId, "ticket", idempotencyKey, ttl);
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
        markActionSucceeded("create", userId, "ticket", idempotencyKey, ticketNo, ttl);
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
        return registerActionOrGet("close", userId, ticketNo, idempotencyKey, ttl);
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
        markActionSucceeded("close", userId, ticketNo, idempotencyKey, ticketNo, ttl);
    }

    /**
     * 注册或查询通用写操作幂等令牌
     *
     * @param scene           场景标识
     * @param userId          用户 ID
     * @param resource        资源标识
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @Override
    public @NotNull TokenStatus registerActionOrGet(@NotNull String scene,
                                                    @NotNull Long userId,
                                                    @NotNull String resource,
                                                    @NotNull String idempotencyKey,
                                                    @NotNull Duration ttl) {
        String key = buildKey(scene, userId, resource, idempotencyKey);
        return registerOrGet(key, ttl);
    }

    /**
     * 标记通用写操作幂等令牌成功
     *
     * @param scene           场景标识
     * @param userId          用户 ID
     * @param resource        资源标识
     * @param idempotencyKey  幂等键
     * @param resultRef       成功结果引用
     * @param ttl             成功状态 TTL
     */
    @Override
    public void markActionSucceeded(@NotNull String scene,
                                    @NotNull Long userId,
                                    @NotNull String resource,
                                    @NotNull String idempotencyKey,
                                    @NotNull String resultRef,
                                    @NotNull Duration ttl) {
        String key = buildKey(scene, userId, resource, idempotencyKey);
        redisTemplate.opsForValue().set(key, VALUE_OK_PREFIX + resultRef, ttl);
    }

    /**
     * 构造 Redis Key
     *
     * @param scene           场景标识
     * @param userId          用户 ID
     * @param resource        资源标识
     * @param idempotencyKey  幂等键
     * @return Redis Key
     */
    private @NotNull String buildKey(@NotNull String scene,
                                     @NotNull Long userId,
                                     @NotNull String resource,
                                     @NotNull String idempotencyKey) {
        return KEY_PREFIX + scene + ":" + userId + ":" + resource + ":" + idempotencyKey;
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
            String resultRef = value.substring(VALUE_OK_PREFIX.length());
            if (resultRef.isBlank())
                return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
            return new TokenStatus(TokenStatus.Status.SUCCEEDED, resultRef);
        }
        return new TokenStatus(TokenStatus.Status.IN_PROGRESS, null);
    }
}

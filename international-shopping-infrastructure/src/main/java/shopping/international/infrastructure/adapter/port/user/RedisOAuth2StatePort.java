package shopping.international.infrastructure.adapter.port.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.user.IOAuth2StatePort;
import shopping.international.domain.model.vo.user.OAuth2EphemeralState;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的一次性 OAuth2 授权状态存取实现
 *
 * <p>键模式: {@code oauth2:state:{state}}, 值为 {@link OAuth2EphemeralState} 的 JSON 序列化
 * 存储时设置 TTL, 读取时采用“一次性弹出”的语义 (读到即删除), 优先使用 Redis 6+ 的
 * {@code GETDEL}, 若不支持则降级为原子 Lua 脚本 (GET+DEL)</p>
 *
 * <p><b>线程安全/并发语义: </b> 使用底层连接的原子命令或单条 EVAL, 确保多并发下只有一个消费者能成功弹出</p>
 */
@Component
@RequiredArgsConstructor
public class RedisOAuth2StatePort implements IOAuth2StatePort {

    /**
     * Key 前缀: oauth2:state:
     */
    private static final String KEY_PREFIX = "oauth2:state:";

    /**
     * Redis KV 客户端 (String 友好 API)
     */
    private final StringRedisTemplate redis;

    /**
     * JSON 序列化/反序列化器
     */
    private final ObjectMapper mapper;

    /**
     * 保存一次性授权上下文 (state → nonce/code_verifier/redirect/provider), 并设置 TTL
     *
     * <p>使用底层 {@code SET key value EX ttl} 语义, 重复写同一 state 将覆盖之前的值 (UPSERT)</p>
     *
     * @param state 上下文
     * @param ttl   过期时间 (必须为正)
     * @throws IllegalStateException 当 JSON 序列化失败时抛出
     */
    @Override
    public void storeEphemeral(@NotNull OAuth2EphemeralState state, @NotNull Duration ttl) {
        try {
            final String key = KEY_PREFIX + state.state();
            final String json = mapper.writeValueAsString(state);
            // 显式标注为 RedisCallback 以避免与 SessionCallback 重载二义性
            redis.execute((RedisCallback<Void>) connection -> {
                byte[] k = key.getBytes(StandardCharsets.UTF_8);
                byte[] v = json.getBytes(StandardCharsets.UTF_8);
                // SET key value EX ttl (UPSERT)
                connection.stringCommands().set(k, v, Expiration.from(ttl), RedisStringCommands.SetOption.UPSERT);
                return null;
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 OAuth2EphemeralState 失败", e);
        }
    }

    /**
     * 根据 state 一次性弹出上下文 (读取即删除)
     *
     * <p>优先尝试 Redis 6+ 的 {@code GETDEL}, 若底层驱动/服务端不支持, 则回退到 Lua 脚本实现
     * {@code local v=GET key; if v then DEL key; end; return v}, 确保原子性</p>
     *
     * @param state state 字符串
     * @return 上下文, 不存在或过期返回空
     */
    @Override
    public @NotNull Optional<OAuth2EphemeralState> popByState(@NotNull String state) {
        final String key = KEY_PREFIX + state;

        OAuth2EphemeralState result = redis.execute((RedisCallback<OAuth2EphemeralState>) connection -> {
            byte[] k = key.getBytes(StandardCharsets.UTF_8);
            byte[] raw;

            try {
                // 优先尝试 Redis 6+ GETDEL
                raw = connection.stringCommands().getDel(k);
            } catch (Throwable unsupported) {
                // 回退到原子 Lua: GET 然后 DEL, 并返回旧值
                // KEYS[1] = key; 返回值为 VALUE
                final byte[] script = "local v=redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]); end; return v"
                        .getBytes(StandardCharsets.UTF_8);
                raw = connection.scriptingCommands()
                        .eval(script, ReturnType.VALUE, 1, k);
            }

            if (raw == null)
                return null;
            try {
                return mapper.readValue(raw, OAuth2EphemeralState.class);
            } catch (Exception e) {
                // 反序列化失败：作废该状态, 返回 null
                return null;
            }
        });

        return Optional.ofNullable(result);
    }
}

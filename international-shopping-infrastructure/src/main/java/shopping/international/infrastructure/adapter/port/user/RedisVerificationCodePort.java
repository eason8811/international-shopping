package shopping.international.infrastructure.adapter.port.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.user.IVerificationCodePort;
import shopping.international.domain.model.vo.user.EmailAddress;

import java.time.Duration;
import java.util.Collections;

/**
 * 基于 Redis 的验证码存取适配器
 *
 * <p>特点: </p>
 * <ul>
 *     <li>存储时设置 TTL</li>
 *     <li>校验 + 消费使用 Lua 脚本原子执行，避免并发条件竞争</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RedisVerificationCodePort implements IVerificationCodePort {

    /**
     * 用于与 Redis 进行交互的模板对象，专门处理字符串类型的数据。此字段在类中作为操作 Redis 的主要工具，支持设置键值对以及执行 Lua 脚本等高级功能
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 为邮箱保存激活验证码并设置过期时间 (幂等覆盖)
     *
     * @param email 邮箱
     * @param code  验证码
     * @param ttl   有效期
     */
    @Override
    public void storeEmailActivationCode(@NotNull EmailAddress email, @NotNull String code, @NotNull Duration ttl) {
        String key = keyOf(email);
        stringRedisTemplate.opsForValue().set(key, code, ttl);
    }

    /**
     * 校验并消费 (删除) 邮箱激活验证码
     *
     * <p>原子性保证: 使用 Lua 脚本比较值后删除</p>
     *
     * @param email 邮箱
     * @param code  用户提交的验证码
     * @return true 表示校验通过且已消费, false 表示验证码不存在或不匹配
     */
    @Override
    public boolean verifyAndConsumeEmailActivationCode(@NotNull EmailAddress email, @NotNull String code) {
        String key = keyOf(email);

        String lua = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    redis.call('del', KEYS[1])
                    return 1
                else
                    return 0
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        Long ret = stringRedisTemplate.execute(script, Collections.singletonList(key), code);
        return ret == 1L;
    }

    /**
     * 计算验证码 Redis Key
     *
     * @param email 邮箱
     * @return key，如 auth:email:activation:{email}
     */
    private static String keyOf(EmailAddress email) {
        return "auth:email:activation:" + email.getValue();
    }
}

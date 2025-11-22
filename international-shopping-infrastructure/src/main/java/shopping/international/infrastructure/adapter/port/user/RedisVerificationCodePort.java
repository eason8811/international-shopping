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
     * Redis 键前缀, 用于区分不同用途的验证码, 如激活邮件或密码重置
     */
    private static final String ACTIVATION_KEY_PREFIX = "auth:email:activation:";
    private static final String PASSWORD_RESET_KEY_PREFIX = "auth:email:password-reset:";

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
        storeCode(ACTIVATION_KEY_PREFIX, email, code, ttl);
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
        return verifyAndConsumeCode(ACTIVATION_KEY_PREFIX, email, code);
    }

    /**
     * 读取当前邮箱的验证码 (不消费)
     *
     * @param email 邮箱
     * @return 验证码, 如果不存在或已过期则返回 null
     */
    @Override
    public String getEmailActivationCode(@NotNull EmailAddress email) {
        return getCode(ACTIVATION_KEY_PREFIX, email);
    }

    /**
     * 存储找回密码验证码并设置有效期
     */
    @Override
    public void storePasswordResetCode(@NotNull EmailAddress email, @NotNull String code, @NotNull Duration ttl) {
        storeCode(PASSWORD_RESET_KEY_PREFIX, email, code, ttl);
    }

    /**
     * 校验并消费找回密码验证码
     */
    @Override
    public boolean verifyAndConsumePasswordResetCode(@NotNull EmailAddress email, @NotNull String code) {
        return verifyAndConsumeCode(PASSWORD_RESET_KEY_PREFIX, email, code);
    }

    /**
     * 读取找回密码验证码 (不消费)
     */
    @Override
    public String getPasswordResetCode(@NotNull EmailAddress email) {
        return getCode(PASSWORD_RESET_KEY_PREFIX, email);
    }

    /**
     * 计算验证码 Redis Key
     *
     * @param email 邮箱
     */
    private void storeCode(String prefix, EmailAddress email, String code, Duration ttl) {
        stringRedisTemplate.opsForValue().set(prefix + email.getValue(), code, ttl);
    }

    /**
     * 校验并消费 (删除) 指定类型的验证码
     *
     * <p>此方法通过 Redis 中存储的键值对来验证传入的验证码是否正确, 如果正确则立即从 Redis 中移除该验证码以防止重复使用。
     * 为了保证操作的原子性, 使用了 Lua 脚本来实现比较和删除操作。</p>
     *
     * @param prefix 验证码类型前缀, 用于区分不同用途的验证码, 如激活邮件或密码重置
     * @param email 用户邮箱, 作为查找验证码的依据
     * @param code 待校验的验证码
     * @return 如果验证码存在且与提供的 code 匹配, 则返回 true 并删除该验证码; 否则返回 false 表示验证码无效或不存在
     */
    private boolean verifyAndConsumeCode(String prefix, EmailAddress email, String code) {
        String key = prefix + email.getValue();
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

    private String getCode(String prefix, EmailAddress email) {
        return stringRedisTemplate.opsForValue().get(prefix + email.getValue());
    }
}

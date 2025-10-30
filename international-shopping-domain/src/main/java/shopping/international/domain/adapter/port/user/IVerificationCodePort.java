package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * 验证码存取端口 (Port), 由缓存 Redis 实现, 支持 TTL
 */
public interface IVerificationCodePort {

    /**
     * 为邮箱保存激活验证码并设置过期时间 (幂等覆盖) 
     *
     * @param email 邮箱
     * @param code  验证码
     * @param ttl   有效期
     */
    void storeEmailActivationCode(@NotNull String email, @NotNull String code, @NotNull Duration ttl);

    /**
     * 校验并消费 (删除) 邮箱激活验证码
     *
     * @param email 邮箱
     * @param code  用户提交的验证码
     * @return 校验是否通过 (通过则同时消费掉) 
     */
    boolean verifyAndConsumeEmailActivationCode(@NotNull String email, @NotNull String code);
}

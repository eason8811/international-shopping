package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.user.EmailAddress;

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
    void storeEmailActivationCode(@NotNull EmailAddress email, @NotNull String code, @NotNull Duration ttl);

    /**
     * 校验并消费 (删除) 邮箱激活验证码
     *
     * @param email 邮箱
     * @param code  用户提交的验证码
     * @return 校验是否通过 (通过则同时消费掉) 
     */
    boolean verifyAndConsumeEmailActivationCode(@NotNull EmailAddress email, @NotNull String code);

    /**
     * 获取当前邮箱对应的验证码, 不消费
     *
     * @param email 邮箱
     * @return 当前存储的验证码, 若不存在或已过期则返回 null
     */
    String getEmailActivationCode(@NotNull EmailAddress email);

    /**
     * 存储找回密码验证码并设置有效期 (幂等覆盖)
     *
     * @param email 邮箱
     * @param code  验证码
     * @param ttl   有效期
     */
    void storePasswordResetCode(@NotNull EmailAddress email, @NotNull String code, @NotNull Duration ttl);

    /**
     * 校验并消费找回密码验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 是否校验通过 (通过即消费)
     */
    boolean verifyAndConsumePasswordResetCode(@NotNull EmailAddress email, @NotNull String code);

    /**
     * 获取当前邮箱的找回密码验证码 (不消费)
     *
     * @param email 邮箱
     * @return 验证码, 若不存在或已过期则返回 null
     */
    String getPasswordResetCode(@NotNull EmailAddress email);
}

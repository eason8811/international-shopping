package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.enums.EmailDeliveryStatus;
import shopping.international.types.exceptions.EmailSendException;

import java.time.Duration;

/**
 * 邮件发送端口 (Port), 领域层通过该端口向外部邮件系统 (如 SendGrid) 发送验证码邮件
 */
public interface IEmailPort {

    /**
     * 发送激活验证码邮件
     *
     * @param email 收件人邮箱
     * @param code  激活验证码
     * @throws EmailSendException 邮件系统异常或发送失败时抛出
     */
    void sendActivationEmail(@NotNull String email, @NotNull String code) throws EmailSendException;

    /**
     * 绑定指定邮箱与激活邮件的消息 ID, 并可设置该绑定的有效期
     *
     * @param email     目标邮箱, 用于存储与其关联的激活邮件消息 ID
     * @param messageId 激活邮件的消息 ID, 将与提供的邮箱地址进行绑定
     * @param ttl       绑定的有效期, 可以为空. 如果为 null, 则表示没有过期时间; 否则, 表示从当前时刻起该绑定有效的持续时间
     */
    void bindActivationMessageId(@NotNull String email, @NotNull String messageId, Duration ttl);

    /**
     * 获取与指定邮箱关联的激活邮件的消息 ID
     *
     * @param email 目标邮箱, 用于查找与其绑定的激活邮件消息 ID
     * @return 返回与指定邮箱关联的激活邮件的消息 ID. 如果没有找到相关联的消息 ID, 则返回 null
     */
    String getActivationMessageId(@NotNull String email);

    /**
     * 根据消息 ID 获取邮件投递状态
     *
     * @param messageId 消息 ID, 用于从 Resend 服务查询对应的邮件状态
     * @return 邮件投递状态, 如果遇到错误或无法识别的状态, 返回 {@link EmailDeliveryStatus#UNKNOWN}
     */
    @NotNull
    EmailDeliveryStatus getStatusByMessageId(@NotNull String messageId);
}

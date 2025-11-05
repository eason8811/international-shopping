package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.EmailAddress;
import shopping.international.domain.model.vo.user.Nickname;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.model.vo.user.Username;
import shopping.international.types.enums.EmailDeliveryStatus;
import shopping.international.types.exceptions.*;

/**
 * 本地认证与令牌发放的领域服务接口
 *
 * <p>职责边界: </p>
 * <ul>
 *   <li>完成注册激活流程的领域规则 (唯一性校验, 状态切换等)</li>
 *   <li>完成本地登录 (账号/邮箱/手机 + 密码) 校验与登录审计 (如最近登录时间)</li>
 *   <li>签发与刷新 <b>访问令牌</b> 与 <b>刷新令牌</b> (纯字符串, 不包含任何 Web/Cookie 概念)</li>
 * </ul>
 *
 * <p>重要约束: </p>
 * <ul>
 *   <li>禁止使用任何 Servlet/Web 相关类型；控制器仅接收/下发 Cookie</li>
 *   <li>实现类可依赖密码编码器, JWT 组件, 仓储等基础设施, 但这些依赖不向上暴露</li>
 * </ul>
 */
public interface IAuthService {

    /**
     * 注册新用户并发送激活邮件 (账户初始状态为 DISABLED)
     *
     * @param username   用户名 (唯一登录名)
     * @param rawPassword 明文密码 (领域服务内负责安全哈希)
     * @param nickname   昵称
     * @param email      邮箱 (可空, 为空则不发送激活邮件)
     * @param phone      手机 (可空)
     * @throws IllegalParamException 当用户名/邮箱/手机存在唯一性冲突, 或参数非法时抛出
     * @throws EmailSendException       如果在发送邮件过程中发生错误 (例如, 邮件服务不可用)
     */
    void register(@NotNull Username username, @NotNull String rawPassword, @NotNull Nickname nickname,
                  @NotNull EmailAddress email, @Nullable PhoneNumber phone);

    /**
     * 校验邮箱验证码并激活账户 (状态从 DISABLED → ACTIVE), 返回激活后的用户聚合快照
     *
     * @param email  收到验证码的邮箱
     * @param code   验证码
     * @return 激活后的 {@link User} 快照
     * @throws VerificationCodeInvalidException 当验证码错误/过期, 或账户不存在/已激活时抛出
     */
    User verifyEmailAndActivate(EmailAddress email, String code);

    /**
     * 重新发送激活邮件给指定邮箱地址
     *
     * @param email 用户的注册邮箱地址
     * @throws IllegalArgumentException 如果提供的邮箱地址格式不正确
     * @throws EmailSendException 如果在发送邮件过程中发生错误 (例如, 邮件服务不可用)
     */
    void resendActivationEmail(EmailAddress email);

    /**
     * 本地登录: 支持 {@code 用户名 / 邮箱 / 手机号其一} + 明文密码, 校验成功返回用户聚合快照
     *
     * @param account    用户名 / 邮箱 / 手机号
     * @param rawPassword 明文密码
     * @return 登录成功的用户聚合快照
     * @throws AccountException 当凭证无效, 账户未激活或被禁用时抛出
     */
    User login(String account, String rawPassword);

    /**
     * 为指定用户签发新的访问令牌 (Access Token)
     *
     * @param userId 用户ID
     * @return 访问令牌字符串 (如 JWT)
     */
    String issueAccessToken(Long userId);

    /**
     * 为指定用户签发新的刷新令牌 (Refresh Token)
     *
     * @param userId 用户ID
     * @return 刷新令牌字符串
     */
    String issueRefreshToken(Long userId);

    /**
     * 使用刷新令牌换取新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新访问令牌
     * @throws RefreshTokenInvalidException 当刷新令牌无效, 过期或被吊销时抛出
     */
    String refreshAccessToken(String refreshToken);

    /**
     * 获取指定邮箱的激活消息ID
     *
     * @param email 用户的电子邮件地址 用于查询对应的激活消息ID
     * @return 激活消息的唯一标识符 如果没有找到 则返回空字符串
     */
    String getActivationMessageId(@NotNull EmailAddress email);

    /**
     * 通过消息ID获取邮件投递状态
     *
     * @param messageId 消息ID, 用于唯一标识一封邮件
     * @return EmailDeliveryStatus 返回与给定消息ID相关的邮件投递状态
     */
    EmailDeliveryStatus getStatusByMessageId(@NotNull String messageId);
}

package shopping.international.domain.service.user.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.domain.adapter.port.user.IVerificationCodePort;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AccountStatus;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.*;
import shopping.international.domain.service.user.IAuthService;
import shopping.international.types.enums.EmailDeliveryStatus;
import shopping.international.types.exceptions.*;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 本地认证与令牌发放领域服务实现 (内聚密码哈希与 JWT 签发能力)
 *
 * <p><b>分层约束: </b>不引入任何 Servlet/Web 类型, 密码哈希 (BCrypt) 与 JWT (Nimbus HS256)
 * 在本类内实现, 仅通过 Repository 与 Port 与外部世界交互</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    // ========= 依赖 (Repository / Port) =========

    /**
     * 用户聚合仓储 (组合 user_account / user_auth 等表的装配与持久化)
     */
    private final IUserRepository userRepository;
    /**
     * 邮件发送端口 (例如 SendGrid)
     */
    private final IEmailPort emailPort;
    /**
     * 验证码端口 (例如 Redis 存储与原子校验/消费)
     */
    private final IVerificationCodePort verificationCodePort;

    // ========= 内聚能力 (配置与工具) =========

    /**
     * JWT 发行规范 (由上层注入, 来源于配置)
     */
    private final JwtIssueSpec jwtSpec;
    /**
     * BCrypt 密码编码器 (强度可按需调整)
     */
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    /**
     * 激活验证码有效期 (建议 10 分钟)
     */
    private final Duration activationCodeTtl = Duration.ofMinutes(10);

    // ========= 注册 / 激活 =========

    /**
     * 注册新用户并发送激活邮件 (账户初始状态为 DISABLED)
     *
     * @param username    用户名 (唯一登录名)
     * @param rawPassword 明文密码 (领域服务内负责安全哈希)
     * @param nickname    昵称
     * @param email       邮箱 (可空, 为空则不发送激活邮件)
     * @param phone       手机 (可空)
     * @throws IllegalParamException 当用户名/邮箱/手机存在唯一性冲突, 或参数非法时抛出
     * @throws EmailSendException    如果在发送邮件过程中发生错误 (例如, 邮件服务不可用)
     */
    @Override
    public void register(@NotNull Username username, @NotNull String rawPassword, @NotNull Nickname nickname,
                         @NotNull EmailAddress email, @Nullable PhoneNumber phone) {

        // 1. 幂等唯一性前置校验 (DB 层仍需唯一索引兜底)
        try {
            require(!userRepository.existsByUsername(username), "用户名已存在");
            require(!userRepository.existsByEmail(email), "邮箱已存在");
            if (phone != null)
                require(!userRepository.existsByPhone(phone), "手机号已存在");
        } catch (IllegalParamException e) {
            throw new ConflictException(e.getMessage(), e);
        }

        // 2. 领域聚合构造 (User.register 内部会附带 LOCAL 绑定)
        String passwordHash = bcrypt.encode(rawPassword);
        User toSave = User.register(
                username,
                nickname,
                email,
                phone,
                passwordHash
        );

        // 3. 原子持久化 (账户 + 本地绑定)
        userRepository.saveNewUserWithBindings(toSave);

        // 4. 下发邮箱验证码 (覆盖式存储, 最后一次生效)
        String code = generateNumericCode(6);
        verificationCodePort.storeEmailActivationCode(email, code, activationCodeTtl);
        emailPort.sendActivationEmail(email, code);
    }

    /**
     * 校验邮箱验证码并激活账户 (状态从 DISABLED → ACTIVE), 返回激活后的用户聚合快照
     *
     * @param email 收到验证码的邮箱
     * @param code  验证码
     * @return 激活后的 {@link User} 快照
     * @throws VerificationCodeInvalidException 当验证码错误/过期, 或账户不存在/已激活时抛出
     */
    @Override
    public User verifyEmailAndActivate(@NotNull EmailAddress email, @NotNull String code) {
        boolean pass = verificationCodePort.verifyAndConsumeEmailActivationCode(email, code);
        if (!pass)
            throw new VerificationCodeInvalidException("验证码错误或已过期");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VerificationCodeInvalidException("账户不存在或邮箱未绑定"));
        if (user.getStatus() == AccountStatus.ACTIVE)
            return user; // 幂等

        user.activate();
        userRepository.updateStatus(user.getId(), AccountStatus.ACTIVE);
        return user;
    }

    /**
     * 重新发送激活邮件给指定邮箱地址
     *
     * @param email 用户的注册邮箱地址
     * @throws IllegalArgumentException 如果提供的邮箱地址格式不正确
     * @throws EmailSendException       如果在发送邮件过程中发生错误 (例如, 邮件服务不可用)
     */
    @Override
    public void resendActivationEmail(@NotNull EmailAddress email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalParamException("账户不存在"));
        if (user.getStatus() == AccountStatus.ACTIVE)
            throw new IllegalParamException("账户已激活, 无需重发");

        String code = generateNumericCode(6);
        verificationCodePort.storeEmailActivationCode(email, code, activationCodeTtl);
        emailPort.sendActivationEmail(email, code);
    }

    // ========= 登录 / 审计 =========

    /**
     * 本地登录: 支持 {@code 用户名 / 邮箱 / 手机号其一} + 明文密码, 校验成功返回用户聚合快照
     *
     * @param account     用户名 / 邮箱 / 手机号
     * @param rawPassword 明文密码
     * @return 登录成功的用户聚合快照
     * @throws AccountException 当凭证无效, 账户未激活或被禁用时抛出
     */
    @Override
    public User login(@NotNull String account, @NotNull String rawPassword) {
        User user = userRepository.findByLoginAccount(account)
                .orElseThrow(() -> new AccountException("账户不存在"));

        // 必须存在 LOCAL 绑定, 且密码匹配
        String localPasswordHash = user.getBindingsSnapshot().stream()
                .filter(binding -> binding.getProvider() == AuthProvider.LOCAL)
                .map(AuthBinding::getPasswordHash)
                .findFirst()
                .orElse(null);
        if (localPasswordHash == null || !bcrypt.matches(rawPassword, localPasswordHash))
            throw new AccountException("用户名或密码错误");

        if (user.getStatus() != AccountStatus.ACTIVE)
            throw new AccountException("账户未激活或已禁用");

        // 登录审计
        LocalDateTime now = LocalDateTime.now();
        user.recordLogin(AuthProvider.LOCAL, now);
        userRepository.recordLogin(user.getId(), AuthProvider.LOCAL, now);
        return user;
    }

    // ========= JWT: 签发 / 刷新 =========

    /**
     * 为指定用户签发新的访问令牌 (Access Token)
     *
     * @param userId 用户ID
     * @return 访问令牌字符串 (如 JWT)
     */
    @Override
    public String issueAccessToken(@NotNull Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AccountException("账户不存在"));
        String scopeListString = user.getBindingsSnapshot().stream()
                .map(AuthBinding::getScope)
                .filter(Objects::nonNull)                 // 防空
                .flatMap(string -> Arrays.stream(string.split(",")))
                .map(String::trim)
                .filter(string -> !string.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtSpec.accessTokenValiditySeconds());
        return signJwt(userId, user.getUsername(), user.getEmail(), scopeListString, now, exp, false);
    }

    /**
     * 为指定用户签发新的刷新令牌 (Refresh Token)
     *
     * @param userId 用户ID
     * @return 刷新令牌字符串
     */
    @Override
    public String issueRefreshToken(@NotNull Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AccountException("账户不存在"));
        String scopeListString = user.getBindingsSnapshot().stream()
                .map(AuthBinding::getScope)
                .filter(Objects::nonNull)                 // 防空
                .flatMap(string -> Arrays.stream(string.split(",")))
                .map(String::trim)
                .filter(string -> !string.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtSpec.refreshTokenValiditySeconds());
        return signJwt(userId, user.getUsername(), user.getEmail(), scopeListString, now, exp, true);
    }

    /**
     * 使用刷新令牌换取新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新访问令牌
     * @throws RefreshTokenInvalidException 当刷新令牌无效, 过期或被吊销时抛出
     */
    @Override
    public String refreshAccessToken(@NotNull String refreshToken) {
        // 1. 解析与基本校验 (签名/过期/iss/aud/nbf)
        SignedJWT jwt = parse(refreshToken);
        if (jwt == null)
            throw new RefreshTokenInvalidException("刷新令牌格式错误");

        try {
            if (!jwt.verify(new MACVerifier(hmacKey())))
                throw new RefreshTokenInvalidException("刷新令牌签名无效");
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now();

            Date exp = claims.getExpirationTime();
            if (exp == null || now.isAfter(exp.toInstant().plusSeconds(jwtSpec.clockSkewSeconds())))
                throw new RefreshTokenInvalidException("刷新令牌已过期");

            Date nbf = claims.getNotBeforeTime();
            if (nbf != null && now.isBefore(nbf.toInstant().minusSeconds(jwtSpec.clockSkewSeconds())))
                throw new RefreshTokenInvalidException("刷新令牌尚未生效");

            if (jwtSpec.issuer() != null && !Objects.equals(jwtSpec.issuer(), claims.getIssuer()))
                throw new RefreshTokenInvalidException("发行者不匹配");

            if (jwtSpec.audience() != null && claims.getAudience() != null && !claims.getAudience().isEmpty()
                    && !claims.getAudience().contains(jwtSpec.audience()))
                throw new RefreshTokenInvalidException("受众不匹配");

            String typ = Objects.toString(claims.getClaim("typ"), null);
            if (!"refresh".equals(typ))
                throw new RefreshTokenInvalidException("非法的令牌类型");

            // 2. 读取 uid, 签发新的 Access Token
            Object uid = claims.getClaim("uid"); // 我们在签发时始终放入 uid
            Long userId = (uid instanceof Number number) ? number.longValue() : Long.parseLong(String.valueOf(uid));
            return issueAccessToken(userId);
        } catch (ParseException e) {
            throw new RefreshTokenInvalidException("刷新令牌解析失败: " + e.getMessage());
        } catch (Exception e) {
            throw new RefreshTokenInvalidException("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定邮箱的激活消息ID
     *
     * @param email 用户的电子邮件地址 用于查询对应的激活消息ID
     * @return 激活消息的唯一标识符 如果没有找到 则返回空字符串
     */
    public String getActivationMessageId(@NotNull EmailAddress email) {
        return emailPort.getActivationMessageId(email);
    }

    /**
     * 通过消息ID获取邮件投递状态
     *
     * @param messageId 消息ID, 用于唯一标识一封邮件
     * @return EmailDeliveryStatus 返回与给定消息ID相关的邮件投递状态
     */
    public EmailDeliveryStatus getStatusByMessageId(@NotNull String messageId) {
        return emailPort.getStatusByMessageId(messageId);
    }

    // ========= 私有: JWT 与密码工具 =========

    /**
     * 生成并签名一个 JWT (可作为 access 或 refresh)
     * <ul>
     *   <li>sub: uid</li>
     *   <li>username: 用户名</li>
     *   <li>email: 邮箱</li>
     *   <li>typ: {@code access|refresh}</li>
     *   <li>iat/nbf/exp/iss/aud</li>
     *   <li>roles: 使用逗号分隔的字符串</li>
     * </ul>
     *
     * @param userId          用户的唯一标识符
     * @param username        用户名, 用于JWT中的用户名声明
     * @param email           用户邮箱, 用于JWT中的邮箱声明
     * @param scopeListString 权限列表字符串, 代表用户的角色或权限
     * @param iat             发行时间, 表示JWT被签发的时间
     * @param exp             过期时间, 表示JWT的有效期限
     * @param isRefresh       标识该JWT是否为刷新令牌
     * @return 签名后的JWT字符串, 可直接使用
     * @throws IllegalStateException 如果在创建或签名JWT过程中发生异常, 则抛出此异常
     */
    private String signJwt(Long userId, Username username, EmailAddress email, String scopeListString, Instant iat, Instant exp, boolean isRefresh) {
        try {

            JWTClaimsSet.Builder claimSetBuilder = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("uid", userId)
                    .claim("username", username.getValue())
                    .claim("email", email.getValue())
                    .claim("typ", isRefresh ? "refresh" : "access")
                    .claim("roles", scopeListString)
                    .issueTime(Date.from(iat))
                    .notBeforeTime(Date.from(iat))
                    .expirationTime(Date.from(exp));

            if (jwtSpec.issuer() != null)
                claimSetBuilder.issuer(jwtSpec.issuer());
            if (jwtSpec.audience() != null && !jwtSpec.audience().isBlank())
                claimSetBuilder.audience(Collections.singletonList(jwtSpec.audience()));

            SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimSetBuilder.build());
            signed.sign(new MACSigner(hmacKey()));
            return signed.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 Base64 编码的密钥字符串解码为字节数组, 用于 HMAC 算法
     *
     * @return 返回解码后的密钥字节数组, 该数组将被用作 HMAC 操作中的密钥
     */
    private byte[] hmacKey() {
        requireNotBlank(jwtSpec.secretBase64(), "JWT 密钥未配置");
        String b64 = jwtSpec.secretBase64();
        return Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析给定的字符串为 <code>SignedJWT</code> 对象
     *
     * @param raw 字符串形式的 JWT, 包含签名信息
     * @return 如果解析成功, 则返回一个 <code>SignedJWT</code> 对象, 否则返回 null, 注意此方法可能会抛出异常, 但会被捕获并转换成 null 返回
     */
    @Nullable
    private static SignedJWT parse(String raw) {
        try {
            return SignedJWT.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成一个指定长度的纯数字验证码
     *
     * @param len 需要生成的数字字符串的长度, 必须为正整数
     * @return 由随机数字组成的字符串, 字符串的长度等于传入的参数 <code>len</code>
     */
    private static String generateNumericCode(int len) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(random.nextInt(10));
        return sb.toString();
    }
}

package shopping.international.domain.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.user.IOAuth2RemotePort;
import shopping.international.domain.adapter.port.user.IOAuth2StatePort;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.*;
import shopping.international.domain.service.user.IBindingService;
import shopping.international.types.exceptions.OAuth2HandleException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 第三方账号绑定/解绑 应用服务实现
 *
 * <p>职责：
 * <ul>
 *   <li>查询当前用户的绑定列表</li>
 *   <li>生成绑定授权 URL (含 state / PKCE / nonce)</li>
 *   <li>处理 OAuth2 回调并在仓储层执行绑定 (upsert)</li>
 *   <li>解绑指定 provider (含最少绑定数与 LOCAL 保护)</li>
 * </ul>
 * </p>
 *
 * <p><b>事务边界：</b> 所有数据写操作均下沉至 {@link IUserRepository} 完成, 必要处由仓储方法声明
 * {@code @Transactional}, 本服务仅做编排与校验。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BindingService implements IBindingService {

    /**
     * 用户聚合仓储 (具体类)
     */
    private final IUserRepository userRepository;
    /**
     * 提供方信息配置列表
     */
    private final List<OAuth2ProviderSpec> oAuth2ProviderSpecList;
    /**
     * OAuth2 / OIDC 远程交互端口 (Token, ID Token 验签, UserInfo)
     */
    private final IOAuth2RemotePort remotePort;
    /**
     * 一次性 state 存取端口 (Redis GETDEL 语义)
     */
    private final IOAuth2StatePort statePort;
    /**
     * 授权发起到回调允许的最大时延 (10 分钟)
     */
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    /**
     * 查询当前用户的绑定列表
     *
     * @param userId 当前登录用户ID
     * @return 用户的认证绑定列表, 包含所有与该用户关联的第三方认证信息, 每个 {@link AuthBinding} 对象代表一个认证绑定
     */
    @Override
    public @NotNull List<AuthBinding> listBindings(@NotNull Long userId) {
        return userRepository.listBindingsByUserId(userId);
    }

    // ======================== 授权地址构造 ========================

    /**
     * 构建用于第三方账号绑定的授权 URL
     *
     * @param userId   用户ID, 用于标识当前用户
     * @param provider 第三方认证提供方, 指定要绑定的第三方服务
     * @param redirect 绑定成功后的重定向 URL, 可选参数, 如果为空则使用默认值
     * @return 授权 URL 字符串, 用于引导用户前往第三方平台进行授权操作
     */
    @Override
    public @NotNull String buildBindAuthorizationUrl(@NotNull Long userId, @NotNull AuthProvider provider, @Nullable String redirect) {
        OAuth2ProviderSpec providerSpec = selectConfigByProvider(provider);

        // 1. 生成一次性参数
        String state = randomUrlSafe(32);
        String nonce = randomUrlSafe(32);
        String codeVerifier = randomPkceVerifier();               // 43~128 URL-safe
        String codeChallenge = s256(codeVerifier);                 // base64url(no padding) of SHA-256

        // 2. 缓存一次性上下文 (回调时 pop 掉)
        statePort.storeEphemeral(new OAuth2EphemeralState(userId, provider, state, nonce, codeVerifier, redirect), STATE_TTL);

        // 3. 构造授权 URL (严格使用配置的 redirectUri, 前端落地页 redirect 仅存入缓存, 不传给第三方)
        StringBuilder url = new StringBuilder(providerSpec.authorizationEndpoint());
        url.append(url.indexOf("?") >= 0 ? "&" : "?")
                .append("response_type=code")
                .append("&client_id=").append(urlEncode(providerSpec.clientId()))
                .append("&redirect_uri=").append(urlEncode(providerSpec.redirectUri()))
                .append("&scope=").append(urlEncode(providerSpec.scope()))
                .append("&state=").append(urlEncode(state))
                .append("&nonce=").append(urlEncode(nonce))
                .append("&code_challenge=").append(urlEncode(codeChallenge))
                .append("&code_challenge_method=S256");

        return url.toString();
    }

    /**
     * 处理第三方账号绑定回调, 根据提供的参数校验并完成与当前用户的绑定
     *
     * @param provider         第三方认证提供方, 指定要绑定的第三方服务
     * @param code             授权码, 由第三方平台在授权成功后返回
     * @param state            状态标识, 用于防止跨站请求伪造攻击, 通常为生成授权URL时附带的一次性随机字符串
     * @param error            错误信息, 如果有错误发生则由第三方平台返回
     * @param errorDescription 错误描述, 提供更详细的错误信息
     * @return OAuth2CallbackResult 对象, 包含处理结果及可能的令牌信息和重定向URL, 详情见 {@link OAuth2CallbackResult}
     */
    @Override
    public @NotNull OAuth2CallbackResult handleBindCallback(@NotNull AuthProvider provider, @Nullable String code,
                                                            @Nullable String state, @Nullable String error,
                                                            @Nullable String errorDescription) {
        requireNotBlank(state, "state 缺失");
        OAuth2EphemeralState ephemeralState = statePort.popByState(state)
                .orElseThrow(() -> new OAuth2HandleException("非法或过期的 state"));

        // 强一致: 回调 provider 必须与发起时一致
        require(provider == ephemeralState.provider(), "provider 不匹配");

        // 失败分支 (用户取消等) : 仅清理一次性状态并返回失败结果 (携带前端落地页)
        if (error != null && !error.isBlank()) {
            String redirect = resolveRedirect(ephemeralState, selectConfigByProvider(provider));
            return OAuth2CallbackResult.failure(redirect);
        }

        requireNotBlank(code, "授权码缺失");

        OAuth2ProviderSpec providerSpec = selectConfigByProvider(provider);

        // 1. 换取 Token (带上 redirect_uri 与 code_verifier)
        OAuth2TokenResponse tokenResponse = remotePort.exchangeAuthorizationCode(
                providerSpec, code, providerSpec.redirectUri(), ephemeralState.codeVerifier());

        // 2. 验证 id_token (如果提供) 与 nonce 一致性 (OIDC)
        String sub;
        String issuer;
        boolean emailVerified;
        String email;
        String name;
        String avatar;

        if (tokenResponse.idToken() != null && !tokenResponse.idToken().isBlank()) {
            OidcIdTokenClaims claims = remotePort.verifyAndParseIdToken(providerSpec, tokenResponse.idToken());
            // 关键: nonce 必须匹配
            require(Objects.equals(ephemeralState.nonce(), claims.nonce()), "nonce 不匹配");
            // 取关键信息
            sub = requireNonBlank(claims.sub(), "id_token.sub 缺失");
            issuer = requireNonBlank(claims.iss(), "id_token.iss 缺失");
            email = emptyToNull(claims.email());
            emailVerified = claims.emailVerified() != null && claims.emailVerified();
            name = emptyToNull(claims.name());
            avatar = emptyToNull(claims.picture());
        } else {
            // 非 OIDC: 需要通过 userinfo 获取 sub
            OidcUserInfo info = remotePort.fetchUserInfo(providerSpec, tokenResponse.accessToken());
            sub = requireNonBlank(info.sub(), "userinfo.sub 缺失");
            issuer = requireNonBlank(providerSpec.issuer(), "非 OIDC 流程需在配置中提供 issuer");
            email = emptyToNull(info.email());
            emailVerified = info.emailVerified() != null && info.emailVerified();
            name = emptyToNull(info.name());
            avatar = emptyToNull(info.avatar());
        }

        // 3. 如需要更丰富资料且 scope 覆盖, 补充一次 userinfo 并校验 sub 一致
        try {
            requireNotBlank(providerSpec.userinfoEndpoint(), "userinfoEndpoint 缺失");
            OidcUserInfo info = remotePort.fetchUserInfo(providerSpec, tokenResponse.accessToken());
            requireNotNull(info.sub(), "userinfo.sub 缺失");
            require(Objects.equals(sub, info.sub()), "userinfo.sub 与 id_token.sub 不一致");
            // 尝试用 userinfo 覆盖更完整字段
            if (email == null)
                email = emptyToNull(info.email());
            if (!emailVerified)
                emailVerified = info.emailVerified() != null && info.emailVerified();
            if (name == null)
                name = emptyToNull(info.name());
            if (avatar == null)
                avatar = emptyToNull(info.avatar());
        } catch (Exception e) {
            // 允许失败, 不阻断主流程
            log.warn("获取更丰富的用户资料和 scope 覆盖失败, 但是忽略异常, 异常信息: {}", e.getMessage());
        }

        // 4) 并发/唯一性保护：同一个 (issuer, sub) 不得绑定至不同 user
        boolean boundToOthers = userRepository.existsBindingByIssuerAndUidExcludingUser(issuer, sub, ephemeralState.userId());
        String redirect = resolveRedirect(ephemeralState, providerSpec);
        if (boundToOthers)
            return OAuth2CallbackResult.failure(redirect);

        // 4.1 装载当前用户聚合
        User user = userRepository.findById(ephemeralState.userId())
                .orElseThrow(() -> new OAuth2HandleException("用户不存在"));

        // 4.2 访问令牌过期时间 (使用同一个 now, 避免多次调用 LocalDateTime.now())
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = (tokenResponse.expiresInSeconds() != null)
                ? now.plusSeconds(tokenResponse.expiresInSeconds())
                : null;

        // 4.3 构建绑定实体
        AuthBinding binding = AuthBinding.oauth(
                provider,
                issuer,
                sub,
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                expiresAt,
                tokenResponse.scope()
        );

        // 4.4 先由聚合检查 provider 唯一性, issuer+providerUid 唯一性, 至少一种登录方式等不变式
        user.addBinding(binding);
        user.recordLogin(provider, now);

        // 5) 再由仓储将增量写入持久化层
        userRepository.upsertAuthBinding(ephemeralState.userId(), binding);
        userRepository.recordLogin(ephemeralState.userId(), provider, user.getLastLoginAt());

        // 6) 成功回跳
        return OAuth2CallbackResult.success(null, null, redirect);
    }

    /**
     * 解除指定用户与第三方认证提供方之间的绑定关系
     *
     * @param userId   用户ID, 指定要解绑的用户
     * @param provider 第三方认证提供方, 指定要解除绑定的服务
     */
    @Override
    public void unbind(@NotNull Long userId, @NotNull AuthProvider provider) {
        // 1) 保护：不可解绑 LOCAL
        if (provider == AuthProvider.LOCAL)
            throw new IllegalArgumentException("本地登录不可解绑");

        // 2) 装载聚合, 应用 "至少保留一种登录方式" 等不变式
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 若解绑后没有任何登录方式, 将在这里抛异常
        user.removeBinding(provider);

        // 3) 仓储删除持久化记录
        userRepository.deleteBinding(userId, provider);
    }

    // =================== 私有工具 ===================

    /**
     * 解析最终前端落地页: 优先缓存 redirect, 其次 provider 默认成功页, 最后 "/"
     *
     * @param ephemeralState 缓存的一次性上下文
     * @param providerSpec   提供方配置
     * @return 最终前端落地页
     */
    private String resolveRedirect(OAuth2EphemeralState ephemeralState, OAuth2ProviderSpec providerSpec) {
        if (ephemeralState.redirect() != null && !ephemeralState.redirect().isBlank())
            return ephemeralState.redirect();
        if (providerSpec.defaultSuccessRedirect() != null && !providerSpec.defaultSuccessRedirect().isBlank())
            return providerSpec.defaultSuccessRedirect();
        return "/";
    }

    /**
     * 将给定的 code_verifier 通过 SHA-256 哈希算法进行处理, 并将结果转换为 base64url 编码格式的字符串
     *
     * @param codeVerifier 需要被哈希处理的 code_verifier 字符串
     * @return 经过 SHA-256 哈希处理并转换为 base64url 格式的字符串
     * @throws IllegalStateException 如果在计算过程中遇到异常, 则抛出此异常, 携带具体的错误信息
     */
    private static String s256(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return base64UrlNoPad(digest);
        } catch (Exception e) {
            throw new IllegalStateException("计算 code_challenge 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成一个用于 PKCE (Proof Key for Code Exchange) 流程的随机验证器
     *
     * @return 一个长度为 64 字符的 URL 安全的随机字符串, 作为 PKCE 验证器
     */
    private static String randomPkceVerifier() {
        return randomUrlSafe(64);
    }

    /**
     * 生成指定长度的URL安全的随机字符串
     *
     * @param byteLen 字节数组长度, 用于控制生成的随机字符串长度
     * @return 返回一个使用 Base64 URL 安全编码且无填充的随机字符串
     */
    private static String randomUrlSafe(int byteLen) {
        byte[] buf = new byte[byteLen];
        new Random().nextBytes(buf);
        return base64UrlNoPad(buf);
    }

    /**
     * 将给定的字节数组转换为无填充的 base64url 编码字符串
     *
     * @param bytes 待编码的字节数组
     * @return 无填充的 base64url 编码字符串
     */
    private static String base64UrlNoPad(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 将给定的字符串进行 URL 编码 使用 UTF-8 字符集
     *
     * @param string 需要被编码的原始字符串
     * @return 返回经过 URL 编码后的字符串
     */
    private static String urlEncode(String string) {
        return java.net.URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    /**
     * 确保给定的字符串是非空且不只包含空白字符 如果字符串为空或仅包含空白字符, 则抛出异常
     *
     * @param value 需要检查的字符串
     * @param msg   当 <code>value</code> 为空或空白时抛出异常的信息
     * @return 返回非空且不只包含空白字符的 <code>value</code>
     * @throws IllegalArgumentException 如果 <code>value</code> 为空或空白
     */
    @NotNull
    private static String requireNonBlank(String value, String msg) {
        requireNotBlank(value, msg);
        return value;
    }

    /**
     * 将空字符串或全空白字符串转换为 null
     *
     * @param value 待转换的字符串 如果 value 为空白 或者 只包含空白字符, 则返回 null
     * @return 转换后的结果 若输入为 null 或者 空白字符串, 返回 null; 否则, 返回原字符串
     */
    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * 根据给定的认证提供者选择对应的 OAuth2 配置信息
     *
     * @param provider 认证提供者的枚举类型, 用于匹配 {@code oAuth2ProviderSpecList} 中的配置项
     * @return 返回与指定提供者匹配的第一个 {@link OAuth2ProviderSpec} 对象 如果没有找到匹配的配置, 则抛出异常
     * @throws IllegalArgumentException 如果没有找到与指定提供者相匹配的任何配置, 将抛出此异常
     */
    private OAuth2ProviderSpec selectConfigByProvider(AuthProvider provider) {
        return oAuth2ProviderSpecList.stream()
                .filter(spec -> spec.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 provider 配置: " + provider));
    }
}

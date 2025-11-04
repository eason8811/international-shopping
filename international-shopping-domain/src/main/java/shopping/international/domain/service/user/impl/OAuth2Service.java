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
import shopping.international.domain.model.enums.user.Gender;
import shopping.international.domain.model.vo.user.*;
import shopping.international.domain.service.user.IAuthService;
import shopping.international.domain.service.user.IOAuth2Service;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.OAuth2HandleException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Objects.requireNonNullElse;
import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * OAuth2/OIDC 第三方登录领域服务实现
 *
 * <ul>
 *   <li>临时状态存取 (state/nonce/code_verifier)</li>
 *   <li>Token 置换, ID Token 验签, UserInfo 拉取 (经由 Port 调用外部提供方)</li>
 *   <li>本地用户聚合创建/读取/登录审计</li>
 *   <li>本地会话令牌签发 (委托 {@link IAuthService})</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service implements IOAuth2Service {

    /**
     * 用户聚合仓储
     */
    private final IUserRepository userRepository;
    /**
     * 提供方信息配置列表
     */
    private final List<OAuth2ProviderSpec> oAuth2ProviderSpecList;
    /**
     * 远端交互端口: 换 token, 验 id_token, 取 userinfo
     */
    private final IOAuth2RemotePort remotePort;
    /**
     * 临时 state/nonce/code_verifier 存取端口 (Redis)
     */
    private final IOAuth2StatePort statePort;
    /**
     * 本地会话签发 (与本地 AuthService 对齐)
     */
    private final IAuthService authService;

    /**
     * 授权发起到回调允许的最大时延 (10 分钟)
     */
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    // ======================== 授权地址构造 ========================

    /**
     * 构造第三方授权地址, 并以一次性 {@code state} 关联缓存 {@code nonce}/{@code code_verifier}/{@code redirect} 等上下文
     *
     * @param provider 第三方提供方 (如 GOOGLE/APPLE/...)
     * @param redirect 登录完成后的站内跳转地址 (可空)
     * @return 第三方授权页的完整 URL (包含 {@code client_id}, {@code redirect_uri}, {@code scope}, {@code state}, {@code nonce}, {@code code_challenge} 等)
     * @throws IllegalParamException 当提供方未配置或内部错误时抛出
     */
    @Override
    public String buildAuthorizationUrl(@NotNull AuthProvider provider, @Nullable String redirect) {
        OAuth2ProviderSpec providerSpec = selectConfigByProvider(provider);

        // 1. 生成一次性参数
        String state = randomUrlSafe(32);
        String nonce = randomUrlSafe(32);
        String codeVerifier = randomPkceVerifier();               // 43~128 URL-safe
        String codeChallenge = s256(codeVerifier);                 // base64url(no padding) of SHA-256

        // 2. 缓存一次性上下文 (回调时 pop 掉)
        statePort.storeEphemeral(new OAuth2EphemeralState(provider, state, nonce, codeVerifier, redirect), STATE_TTL);

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

        // provider 特定的额外参数 (如 prompt/login_hint 等) 由 infra 层在 MetaPort 中统一返回或此处追加
        return url.toString();
    }

    // ======================== 回调处理 ========================

    /**
     * 处理第三方回调 (与 OAuth2/OIDC 授权码 + PKCE 时序一致)
     *
     * <p><b>标准流程: </b></p>
     * <ol>
     *   <li><b>错误短路: </b>若 {@code error} 非空, 则仍需校验 {@code state} (一次性, 与发起时一致),
     *       成立则清理缓存并返回失败结果 (用于前端展示), 不一致则拒绝 (防重放/伪造)</li>
     *
     *   <li><b>校验 state: </b>必须与发起授权时生成的一次性 {@code state} 完全一致且未被使用, 据此
     *       <b>弹出 (并删除)</b>当时缓存的上下文: {@code nonce}, {@code code_verifier}, {@code redirect}</li>
     *
     *   <li><b>置换令牌: </b>向提供方的 token 端点发起 <code>application/x-www-form-urlencoded</code> 请求,
     *       携带参数:
     *       <ul>
     *         <li>{@code grant_type=authorization_code}</li>
     *         <li>{@code code}: 回调中的授权码</li>
     *         <li>{@code redirect_uri}: 必须与授权请求阶段使用的完全一致</li>
     *         <li>{@code client_id}: 客户端标识</li>
     *         <li>{@code client_secret}: <i>机密客户端</i>必填, 公共客户端通常不需要</li>
     *         <li>{@code code_verifier}: 与授权阶段配对的 PKCE 明文字段</li>
     *       </ul>
     *       期望返回 {@code access_token}, {@code expires_in}, {@code id_token}[OIDC], {@code refresh_token}[可选] 等</li>
     *
     *   <li><b>校验 ID Token (OIDC): </b>若返回了 {@code id_token}, 需使用提供方 JWK 验签, 并严格校验声明:
     *       <ul>
     *         <li>{@code iss}: 在允许发行者列表内</li>
     *         <li>{@code aud}: 包含本服务的 {@code client_id}</li>
     *         <li>{@code exp}/{@code iat}/{@code nbf}: 在允许时钟偏移内有效</li>
     *         <li>{@code nonce}: 必须与 <b>缓存中取回的 nonce 完全一致</b> (防止重放)</li>
     *       </ul>
     *       任一校验失败 → 失败结果并清理一次性状态</li>
     *
     *   <li><b>获取用户信息 (可选): </b>若需要更详细资料且 scope 覆盖 (如 {@code openid email profile}),
     *       使用 {@code access_token} 调用 <code>/userinfo</code>, 并要求 {@code sub} 与 {@code id_token.sub} 一致</li>
     *
     *   <li><b>本地账户处理: </b>依据 ({@code iss}, {@code sub})定位或创建本地用户及其绑定关系 (不存在则注册并绑定)</li>
     *
     *   <li><b>签发本地会话: </b>为该用户签发本地 {@code access_token} (以及必要时的 {@code refresh_token}),
     *       并返回 {@code redirectUrl} (优先使用缓存中的 {@code redirect}, 否则给出默认落地页)</li>
     * </ol>
     *
     * <p><b>安全要点: </b></p>
     * <ul>
     *   <li>{@code state}, {@code nonce}, {@code code_verifier} 必须与授权阶段一并生成并与会话/缓存关联, 回调时<b>一次性弹出并删除</b></li>
     *   <li>{@code redirect_uri} 在 token 置换阶段必须与授权阶段完全一致, 否则会被判定为 {@code invalid_grant}</li>
     *   <li>若提供方非 OIDC (不返回 {@code id_token}), 则跳过 nonce 比对, 仅依赖 {@code state}+token 机密性与 (可选) {@code userinfo}</li>
     * </ul>
     *
     * @param provider         第三方提供方 (如 GOOGLE/APPLE/...)
     * @param code             授权码 (成功场景)
     * @param state            一次性状态码 (由授权阶段生成)
     * @param error            失败场景错误码 (用户取消等, 可为空)
     * @param errorDescription 失败场景错误描述 (可为空)
     * @return 处理结果 (是否成功, 本地 access/refresh 令牌字符串, 跳转地址)
     * @throws OAuth2HandleException 当 state 非法/失配, token 置换失败, id_token 验签失败等场景抛出
     */
    @Override
    public OAuth2CallbackResult handleCallback(@NotNull AuthProvider provider, @Nullable String code, @Nullable String state,
                                               @Nullable String error, @Nullable String errorDescription) {

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


        // 4. 本地账户: 存在则登录, 不存在则注册并绑定
        Optional<User> existedUser = userRepository.findByProviderUid(issuer, sub);
        User user;
        if (existedUser.isPresent()) {
            user = existedUser.get();
        } else {
            // 生成唯一用户名 (如 google_xxxxx), 昵称与邮箱尽量填入
            String baseUsername = (provider.name().toLowerCase() + "_" + sub).replaceAll("[^a-zA-Z0-9_\\-]", "");
            String uniqueUsername = generateUniqueUsername(baseUsername);

            Username username = Username.of(uniqueUsername);
            Nickname nickname = Nickname.of(requireNonNullElse(name, uniqueUsername));
            EmailAddress emailAddress = EmailAddress.of(email);
            PhoneNumber phone = PhoneNumber.nullableOf(null);

            // 访问令牌过期时间
            LocalDateTime expiresAt = (tokenResponse.expiresInSeconds() != null)
                    ? LocalDateTime.now().plusSeconds(tokenResponse.expiresInSeconds())
                    : null;

            AuthBinding binding = AuthBinding.oauth(
                    provider,
                    issuer,
                    sub,
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    expiresAt,
                    tokenResponse.scope()
            );

            user = User.registerByOAuth(username, nickname, emailAddress, phone, binding);
            user.updateProfile(UserProfile.of(
                    name,
                    avatar,
                    Gender.UNKNOWN,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
            user.activate(); // 第三方登录默认直接 ACTIVE
            user = userRepository.saveNewUserWithBindings(user);
        }

        // 5. 登录审计 (第三方通道)
        LocalDateTime now = LocalDateTime.now();
        user.recordLogin(provider, now);
        userRepository.recordLogin(user.getId(), provider, now);

        // 6. 签发本地会话
        String access = authService.issueAccessToken(user.getId());
        String refresh = authService.issueRefreshToken(user.getId());

        String redirect = resolveRedirect(ephemeralState, providerSpec);
        return OAuth2CallbackResult.success(access, refresh, redirect);
    }

    // ======================== 私有工具 ========================

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
     * 生成一个基于给定基础用户名的唯一用户名 如果基础用户名已存在 则通过添加后缀数字来确保其唯一性
     *
     * @param baseUsername 基础用户名 用于生成最终唯一用户名的基础字符串
     * @return 返回生成的唯一用户名
     * @throws AccountException 当尝试超过 1000 次仍无法生成唯一用户名时抛出异常
     */
    private String generateUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int seq = 1;
        while (userRepository.existsByUsername(candidate)) {
            seq++;
            candidate = baseUsername + "_" + seq;
            if (seq > 1000)
                throw new AccountException("无法生成唯一用户名");
        }
        return candidate;
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

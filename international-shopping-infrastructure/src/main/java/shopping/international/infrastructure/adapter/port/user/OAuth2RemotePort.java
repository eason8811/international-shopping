package shopping.international.infrastructure.adapter.port.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import shopping.international.domain.adapter.port.user.IOAuth2RemotePort;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2ProviderSpec;
import shopping.international.domain.model.vo.user.OAuth2TokenResponse;
import shopping.international.domain.model.vo.user.OidcIdTokenClaims;
import shopping.international.domain.model.vo.user.OidcUserInfo;
import shopping.international.infrastructure.gateway.user.IOAuth2TokenApi;
import shopping.international.infrastructure.gateway.user.IOidcUserInfoApi;
import shopping.international.infrastructure.gateway.user.dto.TokenRequest;
import shopping.international.infrastructure.gateway.user.dto.TokenRespond;
import shopping.international.infrastructure.gateway.user.dto.UserInfoRespond;
import shopping.international.types.exceptions.IllegalParamException;

import java.net.URL;
import java.time.Instant;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * IOAuth2RemotePort 的 Retrofit + Nimbus 实现
 *
 * <p>职责: </p>
 * <ul>
 *   <li>调用第三方 Token 端点（x-www-form-urlencoded）</li>
 *   <li>使用 Remote JWK Set 验证并解析 ID Token</li>
 *   <li>调用 UserInfo 端点获取用户信息</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OAuth2RemotePort implements IOAuth2RemotePort {
    /**
     * JSON 处理器
     */
    private final ObjectMapper mapper;
    /**
     * @see IOAuth2TokenApi
     */
    private final IOAuth2TokenApi oauth2TokenApi;
    /**
     * @see IOidcUserInfoApi
     */
    private final IOidcUserInfoApi oidcUserInfoApi;

    /**
     * 授权码置换 Token (支持 PKCE)
     *
     * @param providerSpec 提供方配置
     * @param code         授权码
     * @param redirectUri  授权阶段使用的 redirect_uri (必须完全一致)
     * @param codeVerifier 授权阶段生成的 code_verifier
     * @return Token 响应体
     */
    @Override
    public @NotNull OAuth2TokenResponse exchangeAuthorizationCode(@NotNull OAuth2ProviderSpec providerSpec, @NotNull String code,
                                                                  @NotNull String redirectUri, @NotNull String codeVerifier) {
        try {
            TokenRequest request = TokenRequest.builder()
                    .provider(providerSpec.provider())
                    .code(code)
                    .redirectUri(redirectUri)
                    .clientId(providerSpec.clientId())
                    .clientSecret(providerSpec.clientSecret())
                    .codeVerifier(codeVerifier)
                    .build();

            Response<ResponseBody> resp = oauth2TokenApi.exchangeCode(providerSpec.tokenEndpoint(), request.toFieldMap()).execute();
            try (ResponseBody body = resp.body(); ResponseBody errorBody = resp.errorBody()) {
                if (!resp.isSuccessful() || body == null)
                    throw new IllegalParamException("Token 置换失败, HTTP " + resp.code() + " 错误体: " + errorBody);

                TokenRespond respond = mapper.readValue(body.bytes(), TokenRespond.class);

                return new OAuth2TokenResponse(
                        respond.getAccessToken(),
                        respond.getExpiresIn(),
                        respond.getIdToken(),
                        respond.getRefreshToken(),
                        normalizeScope(respond.getScope()),
                        respond.getTokenType()
                );
            }
        } catch (Exception e) {
            throw new IllegalParamException("Token 置换异常: " + e.getMessage());
        }
    }

    /**
     * 验签并解析 ID Token (OIDC)
     *
     * @param spec    提供方配置 (包含 issuer, aud/clientId, jwkSetUri 等)
     * @param idToken 原始 id_token 字符串
     * @return 解析后的声明集
     */
    @Override
    public @NotNull OidcIdTokenClaims verifyAndParseIdToken(@NotNull OAuth2ProviderSpec spec, @NotNull String idToken) {
        requireNotBlank(spec.issuer(), "缺少 issuer 配置以校验 id_token");
        requireNotBlank(spec.jwkSetUri(), "缺少 jwkSetUri 配置以校验 id_token");

        try {

            // 1. Nimbus JWT 处理器 + 远程 JWK
            HashSet<JWSAlgorithm> allowed = new HashSet<>(Arrays.asList(
                    JWSAlgorithm.RS256, JWSAlgorithm.PS256, JWSAlgorithm.ES256
            ));
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            // 配置资源检索器
            DefaultResourceRetriever retriever = new DefaultResourceRetriever(3000, 3000, 512 * 1024);
            // 构建 JWKSource
            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                    .create(new URL(spec.jwkSetUri()), retriever)
                    .cache(15 * 60 * 1000, 5 * 1000)
                    .retrying(true)
                    .build();
            // 允许集合：内部会基于 header.alg 选用匹配的 Key
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(allowed, jwkSource);
            processor.setJWSKeySelector(keySelector);

            // 2 处理并得到 claims
            JWTClaimsSet claims = processor.process(idToken, null);

            // 3 手工做严格校验: iss/aud/time window
            String iss = String.valueOf(claims.getIssuer());
            if (!Objects.equals(iss, spec.issuer()))
                throw new IllegalParamException("id_token.iss 不匹配");

            List<String> aud = claims.getAudience();
            if (aud == null || aud.isEmpty() || !aud.contains(spec.clientId()))
                throw new IllegalParamException("id_token.aud 不包含本 client_id");

            Instant now = Instant.now();
            long skew = Math.max(0, spec.clockSkewSeconds());
            Date exp = claims.getExpirationTime();
            if (exp == null || now.isAfter(exp.toInstant().plusSeconds(skew)))
                throw new IllegalParamException("id_token 已过期");

            Date nbf = claims.getNotBeforeTime();
            if (nbf != null && now.isBefore(nbf.toInstant().minusSeconds(skew)))
                throw new IllegalParamException("id_token 尚未生效");

            // 4) 映射到领域 VO
            return new OidcIdTokenClaims(
                    iss,
                    aud,
                    claims.getSubject(),
                    toInstant(claims.getExpirationTime()),
                    toInstant(claims.getIssueTime()),
                    toInstant(claims.getNotBeforeTime()),
                    stringOrNull(claims.getClaim("nonce")),
                    stringOrNull(claims.getClaim("email")),
                    boolOrNull(claims.getClaim("email_verified")),
                    stringOrNull(claims.getClaim("name")),
                    stringOrNull(claims.getClaim("picture"))
            );
        } catch (IllegalParamException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalParamException("id_token 验签/解析失败: " + e.getMessage());
        }
    }

    /**
     * 使用 access_token 获取用户信息 (如果配置了 userinfo 端点)
     *
     * @param spec        提供方配置
     * @param accessToken 访问令牌
     * @return 用户信息 (可能为空)
     */
    @Override
    public @NotNull OidcUserInfo fetchUserInfo(@NotNull OAuth2ProviderSpec spec, @NotNull String accessToken) {
        requireNotBlank(spec.userinfoEndpoint(), "未配置 userinfoEndpoint, 无法获取用户信息");
        try {
            Response<ResponseBody> resp = oidcUserInfoApi.userInfo(spec.userinfoEndpoint(), "Bearer " + accessToken).execute();

            try (ResponseBody body = resp.body(); ResponseBody errorBody = resp.errorBody()) {
                if (!resp.isSuccessful() || body == null)
                    throw new IllegalParamException("获取 UserInfo 失败, HTTP " + resp.code() + " 错误体: " + errorBody);

                String json = body.string();
                AuthProvider provider = spec.provider();

                if (provider == AuthProvider.TIKTOK) {
                    // TikTok: data.user.open_id / display_name / avatar_url
                    JsonNode root = mapper.readTree(json);
                    JsonNode user = root.path("data").path("user");
                    String sub = getJsonTextOrNull(user, "open_id");
                    String name = getJsonTextOrNull(user, "display_name");
                    String avatar = getJsonTextOrNull(user, "avatar_url");
                    return new OidcUserInfo(sub, null, null, name, avatar);
                }
                if (provider == AuthProvider.X) {
                    // X: data.id / name / username
                    JsonNode root = mapper.readTree(json);
                    JsonNode data = root.path("data");
                    String sub = getJsonTextOrNull(data, "id");
                    String nameProperty = getJsonTextOrNull(data, "name");
                    String usernameProperty = getJsonTextOrNull(data, "username");
                    String name = nameProperty == null ? usernameProperty : nameProperty;
                    String avatar = getJsonTextOrNull(data, "profile_image_url");
                    String email = getJsonTextOrNull(data, "confirmed_email");
                    return new OidcUserInfo(sub, email, true, name, avatar);
                }
                // 默认走标准 OIDC userinfo
                UserInfoRespond dto = mapper.readValue(json, UserInfoRespond.class);
                return new OidcUserInfo(
                        dto.getSub(),
                        dto.getEmail(),
                        dto.getEmailVerified(),
                        dto.getName(),
                        dto.getPicture()
                );
            }
        } catch (Exception e) {
            throw new IllegalParamException("UserInfo 获取异常: " + e.getMessage());
        }
    }

    // ===== 工具 =====

    /**
     * 将给定的 scope 字符串进行规范化处理
     * 该方法会将空格分隔的多个 scope 转换为逗号分隔的形式, 并去除重复项和空白字符
     *
     * @param scope 待处理的原始 scope 字符串, 可以包含一个或多个由空格分隔的 scope 值
     * @return 处理后的 scope 字符串, 如果输入为空或只包含空白字符, 则返回 null
     */
    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank())
            return null;
        // 将空格分隔的 scope 统一转为逗号分隔, 便于与领域聚合合并
        return String.join(",", Arrays.stream(scope.split("[\\s,]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList());
    }

    /**
     * 将给定的 <code>Date</code> 对象转换为 <code>Instant</code> 对象 如果输入为 null, 则返回 null
     *
     * @param date 输入的 <code>Date</code>
     * @return 转换后的 <code>Instant</code>
     */
    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /**
     * 将给定的对象转换为字符串, 如果对象为 <code>null</code>, 则返回 <code>null</code>
     *
     * @param object 输入的对象
     * @return 转换后的字符串
     */
    private static String stringOrNull(Object object) {
        return object == null ? null : String.valueOf(object);
    }

    /**
     * 将给定的对象转换为 <code>Boolean</code> 类型, 如果对象是 <code>null</code>, 则返回 <code>null</code>
     *
     * @param object 输入的对象, 可以是任何类型, 但只有当它是 <code>Boolean</code> 的实例时, 才会直接返回该布尔值, 否则, 会尝试将其转换为字符串再解析为布尔值
     * @return 如果输入对象是 <code>Boolean</code> 实例, 则直接返回该布尔值；如果对象可以被转换成表示真假的字符串, 返回对应的布尔值；如果对象为 <code>null</code> 或无法转换, 返回 <code>null</code>
     */
    private static Boolean boolOrNull(Object object) {
        if (object instanceof Boolean bool)
            return bool;
        return object == null ? null : Boolean.valueOf(String.valueOf(object));
    }

    /**
     * 从给定的 <code>JsonNode</code> 中根据字段名获取文本值 如果节点或指定字段不存在 或者是 null, 则返回 null
     *
     * @param node  给定的 JSON 节点
     * @param field 字段名
     * @return 如果找到且不是 null 的文本值, 则返回该值；否则返回 null
     */
    private static String getJsonTextOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode())
            return null;
        JsonNode childNode = node.get(field);
        return (childNode == null || childNode.isNull()) ? null : childNode.asText();
    }
}

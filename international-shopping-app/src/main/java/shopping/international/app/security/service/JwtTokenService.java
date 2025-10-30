package shopping.international.app.security.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import shopping.international.app.config.JwtProperties;
import shopping.international.types.exceptions.IllegalParamException;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 基于 {@code Nimbus} 的 {@link JwtTokenService} 实现 (HS256 示例)
 *
 * <p>职责: </p>
 * <ul>
 *   <li>校验签名 (HS256)</li>
 *   <li>校验过期时间 (exp), 生效时间 (nbf), 发行者 (iss), 受众 (aud)</li>
 *   <li>提取 subject/username/uid/roles/scope 等声明</li>
 *   <li>构造 {@link Authentication} 并返回</li>
 * </ul>
 *
 * <p>令牌字段约定 (可按你发放令牌时的策略调整): </p>
 * <ul>
 *   <li><b>sub</b>: 用户名 (或用户标识)</li>
 *   <li><b>uid</b>: 用户 ID (可选)</li>
 *   <li><b>roles</b>: 字符串数组 (如 ["USER","ADMIN"]) 或</li>
 *   <li><b>scope</b>: 空格分隔的权限集合 (如 "ROLE_USER profile:read")</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenService implements IJwtTokenService{

    /**
     * JWT 配置项
     */
    private final JwtProperties props;

    /**
     * 生成用于 HMAC 签名的密钥
     *
     * <p>该方法首先从配置属性中获取 Base64 编码的密钥字符串, 并确保其不为空白,
     * 然后将其解码为字节数组, 作为 HMAC 算法所需的密钥</p>
     *
     * @return 用于 HMAC 签名的密钥字节数组
     * @throws IllegalParamException 如果配置中的密钥为空或空白
     */
    private byte[] hmacKey() {
        requireNotBlank(props.getSecretBase64(), "security.jwt.secret-base64 未配置");
        return Base64.getDecoder().decode(props.getSecretBase64().getBytes(UTF_8));
    }

    /**
     * 解析并验证传入的 JWT 字符串, 并基于解析结果构建一个 {@link Authentication} 对象
     *
     * <p>此方法执行以下步骤:
     * <ol>
     *     <li>算法与签名校验 (HS256)</li>
     *     <li>标准声明校验: expirationTime/notBeforeTime/iss/aud</li>
     *     <li>从 JWT 中提取主体与权限信息</li>
     * </ol>
     * 如果在任何一步中发现错误, 方法将返回 {@code null}, 否则, 返回一个填充了主体和权限的 {@link UsernamePasswordAuthenticationToken} 实例
     *
     * @param rawJwt 原始的 JSON Web Token 字符串
     * @return 如果 JWT 有效且通过所有校验, 则返回一个包含用户认证信息的 {@link Authentication} 对象, 否则返回 null
     */
    public Authentication parseAndAuthenticate(String rawJwt) {
        try {
            SignedJWT jwt = SignedJWT.parse(rawJwt);

            // 1. 算法与签名校验 (HS256)
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                log.error("错误的 JWS 算法: {}", jwt.getHeader().getAlgorithm());
                return null;
            }
            if (!jwt.verify(new MACVerifier(hmacKey()))) {
                log.error("JWT 签名校验失败");
                return null;
            }

            // 2. 标准声明校验: expirationTime/notBeforeTime/iss/aud
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();

            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || now.isAfter(expirationTime.toInstant().plusSeconds(props.getClockSkewSeconds()))) {
                log.error("JWT 已于 '{}' 过期", expirationTime);
                return null;
            }

            Date notBeforeTime = claimsSet.getNotBeforeTime();
            if (notBeforeTime != null && now.isBefore(notBeforeTime.toInstant().minusSeconds(props.getClockSkewSeconds()))) {
                log.error("JWT 在 '{}' 之前尚未生效", notBeforeTime);
                return null;
            }

            if (props.getIssuer() != null && !Objects.equals(props.getIssuer(), claimsSet.getIssuer())) {
                log.error("发行者 (Issuer) '{}' 不匹配", claimsSet.getIssuer());
                return null;
            }

            if (props.getAudience() != null && claimsSet.getAudience() != null && !claimsSet.getAudience().isEmpty()
                    && !claimsSet.getAudience().contains(props.getAudience())) {
                log.error("受众 (Audience) '{}' 不匹配 ", claimsSet.getAudience());
                return null;
            }

            // 3. 提取主体与权限
            String principal = firstNonBlank(
                    claimsSet.getSubject(),
                    stringClaim(claimsSet, "username"),
                    stringClaim(claimsSet, "email")
            );
            if (principal == null) {
                log.error("JWT 中未找到主体声明 (sub/username/email)");
                return null;
            }

            // 允许 roles 是数组, 或 scope 是逗号分隔
            Collection<SimpleGrantedAuthority> authoritySet = extractAuthorities(claimsSet);

            // 把 uid 放进 detailMap, 便于控制器层拿到
            Map<String, Object> detailMap = new HashMap<>();
            Object uid = claimsSet.getClaim("uid");
            if (uid != null)
                detailMap.put("uid", uid);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authoritySet);
            auth.setDetails(detailMap);
            return auth;
        } catch (ParseException e) {
            log.error("错误的 JWT 格式: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("JWT 处理异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 claims 中提取权限:
     * <ul>
     *   <li>优先读取 {@code roles} (数组, 集合或逗号分隔字符串)</li>
     *   <li>若无 {@code roles}, 尝试 {@code scope} (逗号分隔)</li>
     * </ul>
     *
     * @param claims 令牌声明集
     * @return Spring Security 所需的权限集合
     */
    private Collection<SimpleGrantedAuthority> extractAuthorities(JWTClaimsSet claims) {
        // roles: ["USER","ADMIN"] / "USER,ADMIN"
        Object roles = claims.getClaim("roles");
        List<String> roleList = new ArrayList<>();
        if (roles instanceof Collection<?> collection)
            roleList.addAll(collection.stream().map(String::valueOf).toList());
        else if (roles instanceof String string && !string.isBlank())
            roleList.addAll(
                    Arrays.stream(string.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList()
            );

        if (roleList.isEmpty()) {
            String scope = stringClaim(claims, "scope");
            if (scope == null || scope.isBlank())
                return Collections.emptySet();
            roleList.addAll(Arrays.stream(scope.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList()
            );
        }

        // 规范化为 SimpleGrantedAuthority
        return roleList.stream()
                .filter(string -> !string.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    /**
     * 从给定的 JWT 声明集中获取指定名称的声明, 并将其转换为字符串形式返回
     *
     * <p>如果指定名称的声明不存在或其值无法转换为字符串, 则返回 null</p>
     *
     * @param claims 包含 JWT 声明的对象
     * @param name   待查询的声明名称
     * @return 指定名称的声明对应的字符串值, 如果该声明不存在或不能被转换为字符串, 返回 null
     */
    @Nullable
    private static String stringClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 从给定的字符串数组中找到第一个非空白字符串
     *
     * @param arr 可变参数列表, 包含待检查的字符串
     * @return 第一个非空白字符串, 如果没有符合条件的字符串, 则返回 null
     */
    @Nullable
    private static String firstNonBlank(String... arr) {
        return Arrays.stream(arr)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .findFirst()
                .orElse(null);
    }
}

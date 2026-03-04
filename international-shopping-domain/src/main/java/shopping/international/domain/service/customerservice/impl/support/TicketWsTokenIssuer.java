package shopping.international.domain.service.customerservice.impl.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import shopping.international.domain.model.vo.customerservice.TicketWsSessionIssueView;
import shopping.international.domain.model.vo.user.JwtIssueSpec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单 WebSocket 会话令牌签发器, 统一 USER 和 AGENT 的 JWT 令牌构造逻辑
 */
@Component
@RequiredArgsConstructor
public class TicketWsTokenIssuer {

    /**
     * JWT 签发配置
     */
    private final JwtIssueSpec jwtIssueSpec;

    /**
     * 令牌用途声明
     */
    private static final String TOKEN_TYPE = "ws";
    /**
     * 令牌作用域声明
     */
    private static final String TOKEN_SCOPE = "customerservice:ws";

    /**
     * 签发 WebSocket 会话令牌
     *
     * @param userId        用户 ID
     * @param actorStrategy 操作者策略
     * @param ttlSeconds    令牌 TTL 秒数
     * @return 会话令牌
     */
    public @NotNull String issueWsToken(@NotNull Long userId,
                                        @NotNull TicketActorStrategy actorStrategy,
                                        int ttlSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        try {
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("uid", userId)
                    .claim("typ", TOKEN_TYPE)
                    .claim("scope", TOKEN_SCOPE)
                    .claim("jti", UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt));

            String wsRoleClaim = actorStrategy.wsRoleClaim();
            if (wsRoleClaim != null && !wsRoleClaim.isBlank())
                claimsBuilder.claim("role", wsRoleClaim);

            if (jwtIssueSpec.issuer() != null && !jwtIssueSpec.issuer().isBlank())
                claimsBuilder.issuer(jwtIssueSpec.issuer());
            if (jwtIssueSpec.audience() != null && !jwtIssueSpec.audience().isBlank())
                claimsBuilder.audience(Collections.singletonList(jwtIssueSpec.audience()));

            SignedJWT wsJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            wsJwt.sign(new MACSigner(hmacKey()));
            return wsJwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("WebSocket 会话令牌签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构造 WebSocket 会话签发视图
     *
     * @param wsToken                  WebSocket 令牌
     * @param wsUrl                    WebSocket 地址
     * @param tokenTtlSeconds          令牌 TTL 秒数
     * @param heartbeatIntervalSeconds 心跳间隔秒数
     * @param resumeTtlSeconds         续传窗口秒数
     * @return 会话签发视图
     */
    public @NotNull TicketWsSessionIssueView buildSessionIssueView(@NotNull String wsToken,
                                                                    @NotNull String wsUrl,
                                                                    int tokenTtlSeconds,
                                                                    int heartbeatIntervalSeconds,
                                                                    int resumeTtlSeconds) {
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(tokenTtlSeconds);
        return new TicketWsSessionIssueView(
                wsToken,
                wsUrl,
                issuedAt,
                expiresAt,
                heartbeatIntervalSeconds,
                resumeTtlSeconds
        );
    }

    /**
     * 构建 HMAC 签名密钥
     *
     * @return 签名密钥字节数组
     */
    private byte[] hmacKey() {
        requireNotBlank(jwtIssueSpec.secretBase64(), "JWT 密钥未配置");
        try {
            return Base64.getDecoder().decode(jwtIssueSpec.secretBase64().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT 密钥 Base64 格式非法", e);
        }
    }
}

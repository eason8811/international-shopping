package shopping.international.domain.service.customerservice.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.adapter.repository.customerservice.IUserTicketRepository;
import shopping.international.domain.model.enums.customerservice.WsErrorCode;
import shopping.international.domain.model.enums.customerservice.WsResumeFallbackStrategy;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;
import shopping.international.domain.model.vo.customerservice.TicketWsConnectionContext;
import shopping.international.domain.model.vo.user.JwtIssueSpec;
import shopping.international.domain.service.customerservice.ITicketWsService;
import shopping.international.domain.service.customerservice.TicketWsConnectException;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单 WebSocket 领域服务实现
 */
@Service
@RequiredArgsConstructor
public class TicketWsService implements ITicketWsService {

    /**
     * 用户侧工单仓储
     */
    private final IUserTicketRepository userTicketRepository;
    /**
     * 管理侧工单仓储
     */
    private final IAdminTicketRepository adminTicketRepository;
    /**
     * JWT 签发配置
     */
    private final JwtIssueSpec jwtIssueSpec;

    /**
     * WebSocket 令牌用途标识
     */
    private static final String TOKEN_TYPE_WS = "ws";
    /**
     * WebSocket 令牌作用域
     */
    private static final String TOKEN_SCOPE = "customerservice:ws";
    /**
     * WebSocket 坐席角色标识
     */
    private static final String TOKEN_ROLE_AGENT = "agent";
    /**
     * 默认心跳间隔秒数
     */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    /**
     * 默认续传窗口秒数
     */
    private static final int RESUME_TTL_SECONDS = 600;
    /**
     * 事件 ID 中消息锚点解析规则
     */
    private static final Pattern MESSAGE_EVENT_ID_PATTERN = Pattern.compile("^msg:(\\d+)$");

    /**
     * 校验 WebSocket 连接请求并返回连接上下文
     *
     * @param wsToken     WebSocket 短期令牌
     * @param ticketNo    订阅工单编号, 可为空
     * @param ticketId    订阅工单 ID, 可为空
     * @param lastEventId 续传锚点事件 ID, 可为空
     * @return 连接上下文
     */
    @Override
    public @NotNull TicketWsConnectionContext authorizeConnection(@NotNull String wsToken,
                                                                  @Nullable String ticketNo,
                                                                  @Nullable Long ticketId,
                                                                  @Nullable String lastEventId) {
        requireNotBlank(wsToken, "ws_token 不能为空");
        if (ticketNo != null && ticketId != null)
            throw invalidFrame("ticket_no 与 ticket_id 不可同时传入");

        ParsedWsToken token = parseAndVerifyWsToken(wsToken);

        Long subscribedTicketId = resolveSubscribedTicketId(token, ticketNo, ticketId);
        Long resumeAfterMessageId = parseResumeAnchor(token, subscribedTicketId, lastEventId);

        return new TicketWsConnectionContext(
                token.userId(),
                token.agent(),
                subscribedTicketId,
                ticketNo,
                resumeAfterMessageId,
                HEARTBEAT_INTERVAL_SECONDS,
                RESUME_TTL_SECONDS
        );
    }

    /**
     * 拉取单工单范围内的增量消息
     *
     * @param actorUserId    连接操作者用户 ID
     * @param agent          是否管理侧坐席连接
     * @param ticketId       工单 ID
     * @param afterMessageId 增量锚点消息 ID, 可为空
     * @param size           拉取条数
     * @return 增量消息列表
     */
    @Override
    public @NotNull List<TicketMessageView> listIncrementalMessages(@NotNull Long actorUserId,
                                                                    boolean agent,
                                                                    @NotNull Long ticketId,
                                                                    @Nullable Long afterMessageId,
                                                                    int size) {
        require(actorUserId >= 1, "actorUserId 必须大于等于 1");
        require(ticketId >= 1, "ticketId 必须大于等于 1");
        require(size >= 1 && size <= 200, "size 必须在 1 到 200 之间");
        if (afterMessageId != null)
            require(afterMessageId >= 1, "afterMessageId 必须大于等于 1");

        if (agent)
            return adminTicketRepository.listTicketMessages(ticketId, null, afterMessageId, true, size);
        return userTicketRepository.listTicketMessages(ticketId, null, afterMessageId, true, size);
    }

    /**
     * 解析并校验 WebSocket 令牌
     *
     * @param wsToken WebSocket 令牌
     * @return 解析后的令牌载荷
     */
    private @NotNull ParsedWsToken parseAndVerifyWsToken(@NotNull String wsToken) {
        SignedJWT signedJwt;
        try {
            signedJwt = SignedJWT.parse(wsToken);
        } catch (ParseException e) {
            throw unauthorized("ws_token 格式非法");
        }

        try {
            boolean verified = signedJwt.verify(new MACVerifier(hmacKey()));
            if (!verified)
                throw unauthorized("ws_token 验签失败");
        } catch (JOSEException e) {
            throw unauthorized("ws_token 验签失败");
        }

        JWTClaimsSet claims;
        try {
            claims = signedJwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw unauthorized("ws_token 载荷解析失败");
        }

        validateClaims(claims);
        Long userId = extractUserId(claims);
        boolean agent = isAgentRole(claims);
        return new ParsedWsToken(userId, agent);
    }

    /**
     * 校验令牌载荷中的通用约束
     *
     * @param claims 令牌载荷
     */
    private void validateClaims(@NotNull JWTClaimsSet claims) {
        String tokenType = asString(claims.getClaim("typ"));
        if (!TOKEN_TYPE_WS.equals(tokenType))
            throw unauthorized("ws_token 类型非法");

        String scope = asString(claims.getClaim("scope"));
        if (!TOKEN_SCOPE.equals(scope))
            throw unauthorized("ws_token 作用域非法");

        Instant now = Instant.now();
        long clockSkewSeconds = Math.max(0L, jwtIssueSpec.clockSkewSeconds());

        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && now.plusSeconds(clockSkewSeconds).isBefore(notBefore.toInstant()))
            throw unauthorized("ws_token 尚未生效");

        Date expiresAt = claims.getExpirationTime();
        if (expiresAt == null)
            throw unauthorized("ws_token 缺少过期时间");
        if (now.minusSeconds(clockSkewSeconds).isAfter(expiresAt.toInstant()))
            throw unauthorized("ws_token 已过期");

        String configuredIssuer = jwtIssueSpec.issuer();
        if (configuredIssuer != null && !configuredIssuer.isBlank()) {
            String issuer = claims.getIssuer();
            if (!configuredIssuer.equals(issuer))
                throw unauthorized("ws_token 发行方非法");
        }

        String configuredAudience = jwtIssueSpec.audience();
        if (configuredAudience != null && !configuredAudience.isBlank()) {
            List<String> audienceList = claims.getAudience();
            if (audienceList == null || audienceList.stream().noneMatch(configuredAudience::equals))
                throw unauthorized("ws_token 受众非法");
        }
    }

    /**
     * 从令牌载荷提取用户 ID
     *
     * @param claims 令牌载荷
     * @return 用户 ID
     */
    private @NotNull Long extractUserId(@NotNull JWTClaimsSet claims) {
        Object uid = claims.getClaim("uid");
        Long userId = null;
        if (uid instanceof Number number)
            userId = number.longValue();
        else if (uid instanceof String uidString)
            try {
                userId = Long.parseLong(uidString.strip());
            } catch (NumberFormatException ignored) {
            }

        if (userId == null) {
            String subject = claims.getSubject();
            if (subject != null && !subject.isBlank())
                try {
                    userId = Long.parseLong(subject.strip());
                } catch (NumberFormatException ignored) {
                }
        }

        if (userId == null || userId < 1)
            throw unauthorized("ws_token 缺少合法 uid");
        return userId;
    }

    /**
     * 判断令牌是否为坐席角色
     *
     * @param claims 令牌载荷
     * @return 是否为坐席
     */
    private boolean isAgentRole(@NotNull JWTClaimsSet claims) {
        String role = asString(claims.getClaim("role"));
        return role != null && TOKEN_ROLE_AGENT.equals(role.toLowerCase(Locale.ROOT));
    }

    /**
     * 解析订阅工单并校验连接权限
     *
     * @param token    解析后的令牌
     * @param ticketNo 订阅工单编号, 可为空
     * @param ticketId 订阅工单 ID, 可为空
     * @return 订阅工单 ID, 可为空
     */
    private @Nullable Long resolveSubscribedTicketId(@NotNull ParsedWsToken token,
                                                     @Nullable String ticketNo,
                                                     @Nullable Long ticketId) {
        if (ticketNo != null) {
            List<Long> ticketIds = token.agent()
                    ? adminTicketRepository.listAgentTicketIdsByNos(token.userId(), List.of(ticketNo))
                    : userTicketRepository.listOwnedTicketIdsByNos(token.userId(), List.of(ticketNo));
            if (ticketIds.isEmpty())
                throw forbidden("无权订阅该 ticket_no");
            return ticketIds.get(0);
        }

        if (ticketId != null) {
            List<Long> ticketIds = token.agent()
                    ? adminTicketRepository.listAgentTicketIdsByIds(token.userId(), List.of(ticketId))
                    : userTicketRepository.listOwnedTicketIdsByIds(token.userId(), List.of(ticketId));
            if (ticketIds.isEmpty())
                throw forbidden("无权订阅该 ticket_id");
            return ticketId;
        }

        return null;
    }

    /**
     * 解析断线续传锚点
     *
     * @param token              解析后的令牌
     * @param subscribedTicketId 订阅工单 ID, 可为空
     * @param lastEventId        续传锚点事件 ID, 可为空
     * @return 续传锚点消息 ID, 可为空
     */
    private @Nullable Long parseResumeAnchor(@NotNull ParsedWsToken token,
                                             @Nullable Long subscribedTicketId,
                                             @Nullable String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank())
            return null;

        Matcher matcher = MESSAGE_EVENT_ID_PATTERN.matcher(lastEventId);
        if (!matcher.matches())
            throw new TicketWsConnectException(
                    WsErrorCode.RESUME_NOT_FOUND,
                    "last_event_id 格式非法或不可用",
                    WsResumeFallbackStrategy.RECONNECT_WITHOUT_LAST_EVENT_ID,
                    null,
                    null
            );

        long messageId;
        try {
            messageId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new TicketWsConnectException(
                    WsErrorCode.RESUME_NOT_FOUND,
                    "last_event_id 格式非法或不可用",
                    WsResumeFallbackStrategy.RECONNECT_WITHOUT_LAST_EVENT_ID,
                    null,
                    null
            );
        }

        if (subscribedTicketId == null)
            throw new TicketWsConnectException(
                    WsErrorCode.RESUME_NOT_AVAILABLE,
                    "未指定单工单订阅范围, 无法执行 last_event_id 续传",
                    WsResumeFallbackStrategy.RECONNECT_WITHOUT_LAST_EVENT_ID,
                    null,
                    null
            );

        boolean exists = token.agent()
                ? adminTicketRepository.existsMessageInTicket(subscribedTicketId, messageId)
                : userTicketRepository.existsMessageInTicket(subscribedTicketId, messageId);
        if (!exists)
            throw new TicketWsConnectException(
                    WsErrorCode.RESUME_NOT_FOUND,
                    "last_event_id 对应事件不存在或已不可用",
                    WsResumeFallbackStrategy.HTTP_CATCHUP_THEN_RECONNECT,
                    messageId,
                    null
            );

        return messageId;
    }

    /**
     * 构建 WebSocket 令牌验签密钥
     *
     * @return HMAC 密钥字节数组
     */
    private byte[] hmacKey() {
        requireNotBlank(jwtIssueSpec.secretBase64(), "JWT 密钥未配置");
        try {
            return Base64.getDecoder().decode(jwtIssueSpec.secretBase64().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw unauthorized("JWT 密钥 Base64 格式非法");
        }
    }

    /**
     * 将对象转换为字符串
     *
     * @param value 原始对象
     * @return 字符串值, 可为空
     */
    private @Nullable String asString(@Nullable Object value) {
        if (value == null)
            return null;
        if (value instanceof String string)
            return string.strip();
        return String.valueOf(value).strip();
    }

    /**
     * 构造未认证异常
     *
     * @param message 错误消息
     * @return 建连异常
     */
    private @NotNull TicketWsConnectException unauthorized(@NotNull String message) {
        return new TicketWsConnectException(WsErrorCode.UNAUTHORIZED, message, null, null, null);
    }

    /**
     * 构造无权限异常
     *
     * @param message 错误消息
     * @return 建连异常
     */
    private @NotNull TicketWsConnectException forbidden(@NotNull String message) {
        return new TicketWsConnectException(WsErrorCode.FORBIDDEN, message, null, null, null);
    }

    /**
     * 构造协议格式异常
     *
     * @param message 错误消息
     * @return 建连异常
     */
    private @NotNull TicketWsConnectException invalidFrame(@NotNull String message) {
        return new TicketWsConnectException(WsErrorCode.INVALID_FRAME, message, null, null, null);
    }

    /**
     * 解析后的 WebSocket 令牌载荷
     *
     * @param userId 令牌归属用户 ID
     * @param agent  是否坐席连接
     */
    private record ParsedWsToken(@NotNull Long userId, boolean agent) {
    }
}

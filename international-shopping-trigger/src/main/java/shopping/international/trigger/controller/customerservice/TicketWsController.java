package shopping.international.trigger.controller.customerservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import shopping.international.api.resp.customerservice.CsWsConnectAckRespond;
import shopping.international.api.resp.customerservice.CsWsEventEnvelopeRespond;
import shopping.international.api.resp.customerservice.CsWsResumeFallbackRespond;
import shopping.international.api.resp.customerservice.CsWsServerErrorFrameRespond;
import shopping.international.api.resp.customerservice.CsWsServerEventFrameRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.domain.model.enums.customerservice.WsErrorCode;
import shopping.international.domain.model.enums.customerservice.WsEventType;
import shopping.international.domain.model.enums.customerservice.WsFrameType;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;
import shopping.international.domain.model.vo.customerservice.TicketWsConnectionContext;
import shopping.international.domain.service.customerservice.ITicketWsService;
import shopping.international.domain.service.customerservice.TicketWsConnectException;
import shopping.international.types.exceptions.IllegalParamException;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 工单 WebSocket 控制器, 负责连接接入, 帧协议处理, 增量事件推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketWsController extends TextWebSocketHandler {

    /**
     * WebSocket 连接上下文属性键
     */
    private static final String ATTR_RUNTIME_KEY = "cs_ws_runtime";
    /**
     * 连接确认事件 ID 前缀
     */
    private static final String CONNECT_EVENT_ID_PREFIX = "connect:";
    /**
     * 消息事件 ID 前缀
     */
    private static final String MESSAGE_EVENT_ID_PREFIX = "msg:";
    /**
     * 增量轮询间隔, 单位毫秒
     */
    private static final long POLL_INTERVAL_MILLIS = 2000L;
    /**
     * 单次增量轮询最大消息条数
     */
    private static final int POLL_BATCH_SIZE = 100;

    /**
     * 工单 WebSocket 领域服务
     */
    private final ITicketWsService ticketWsService;
    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 连接运行态缓存
     */
    private final Map<String, SessionRuntime> runtimeMap = new ConcurrentHashMap<>();
    /**
     * 增量轮询线程池
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2,
            runnable -> {
                Thread thread = new Thread(runnable, "customerservice-ws-poll");
                thread.setDaemon(true);
                return thread;
            }
    );

    /**
     * 连接查询参数
     *
     * @param wsToken     连接令牌
     * @param lastEventId 续传锚点
     * @param ticketNo    工单编号
     * @param ticketId    工单 ID
     */
    private record ConnectQuery(@NotNull String wsToken,
                                @Nullable String lastEventId,
                                @Nullable String ticketNo,
                                @Nullable Long ticketId) {
    }

    /**
     * 释放轮询线程池资源
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * 连接建立后执行握手参数校验, 鉴权, 回传连接确认帧, 启动增量轮询
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        ConnectQuery query;
        try {
            query = parseConnectQuery(session.getUri());
        } catch (IllegalParamException ex) {
            sendErrorAndClose(session, WsErrorCode.INVALID_FRAME, ex.getMessage(), null, CloseStatus.POLICY_VIOLATION);
            return;
        }

        TicketWsConnectionContext context;
        try {
            context = ticketWsService.authorizeConnection(
                    query.wsToken(),
                    query.ticketNo(),
                    query.ticketId(),
                    query.lastEventId()
            );
        } catch (TicketWsConnectException ex) {
            CsWsResumeFallbackRespond fallback = toFallback(ex);
            sendErrorAndClose(session, ex.getCode(), ex.getMessage(), fallback, CloseStatus.POLICY_VIOLATION);
            return;
        }

        SessionRuntime runtime = new SessionRuntime(
                buildConnectionId(),
                session,
                context,
                context.resumeAfterMessageId() == null ? 0L : context.resumeAfterMessageId()
        );
        runtimeMap.put(session.getId(), runtime);
        session.getAttributes().put(ATTR_RUNTIME_KEY, runtime);

        sendConnectAck(runtime);
        startPolling(runtime);
    }

    /**
     * 收到文本帧后执行协议解析, 仅支持 ping 与 ack 客户端帧
     *
     * @param session WebSocket 会话
     * @param message 客户端文本帧
     */
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session,
                                     @NotNull TextMessage message) {
        SessionRuntime runtime = runtimeMap.get(session.getId());
        if (runtime == null)
            return;

        JsonNode payloadNode;
        try {
            payloadNode = objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            sendErrorFrame(session, WsErrorCode.INVALID_FRAME, "客户端帧不是合法 JSON", null);
            return;
        }

        JsonNode typeNode = payloadNode.get("type");
        if (typeNode == null || typeNode.isNull()) {
            sendErrorFrame(session, WsErrorCode.INVALID_FRAME, "客户端帧缺少 type 字段", null);
            return;
        }

        String frameType;
        try {
            frameType = normalizeNotNullField(typeNode.asText(), "type 不能为空", value -> value.length() <= 16, "type 长度不能超过 16 个字符").toLowerCase();
        } catch (IllegalParamException ex) {
            sendErrorFrame(session, WsErrorCode.INVALID_FRAME, ex.getMessage(), null);
            return;
        }

        switch (frameType) {
            case "ping" -> {
                // 当前协议中 ping 仅用于保活, 服务端无需回帧
            }
            case "ack" -> handleClientAckFrame(payloadNode, runtime);
            default -> sendErrorFrame(session, WsErrorCode.INVALID_FRAME, "不支持的客户端帧类型", null);
        }
    }

    /**
     * 连接关闭后清理运行态与轮询任务
     *
     * @param session     WebSocket 会话
     * @param closeStatus 关闭状态
     * @throws Exception 处理异常
     */
    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session,
                                      @NotNull CloseStatus closeStatus) throws Exception {
        cleanupRuntime(session.getId());
        super.afterConnectionClosed(session, closeStatus);
    }

    /**
     * 传输层异常时清理运行态并主动关闭连接
     *
     * @param session   WebSocket 会话
     * @param exception 传输异常
     */
    @Override
    public void handleTransportError(@NotNull WebSocketSession session,
                                     @NotNull Throwable exception) {
        cleanupRuntime(session.getId());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    /**
     * 处理客户端 ack 帧
     *
     * @param payloadNode 帧 JSON
     * @param runtime     会话运行态
     */
    private void handleClientAckFrame(@NotNull JsonNode payloadNode,
                                      @NotNull SessionRuntime runtime) {
        JsonNode eventIdNode = payloadNode.get("event_id");
        if (eventIdNode == null || eventIdNode.isNull()) {
            sendErrorFrame(runtime.session(), WsErrorCode.INVALID_FRAME, "ack 帧缺少 event_id 字段", null);
            return;
        }

        String eventId;
        try {
            eventId = normalizeNotNullField(eventIdNode.asText(), "event_id 不能为空", value -> value.length() <= 64, "event_id 长度不能超过 64 个字符");
        } catch (IllegalParamException ex) {
            sendErrorFrame(runtime.session(), WsErrorCode.INVALID_FRAME, ex.getMessage(), null);
            return;
        }

        runtime.lastAckEventId().set(eventId);
    }

    /**
     * 启动单连接增量轮询任务
     *
     * @param runtime 会话运行态
     */
    private void startPolling(@NotNull SessionRuntime runtime) {
        if (runtime.context().subscribedTicketId() == null)
            return;

        ScheduledFuture<?> pollFuture = scheduler.scheduleWithFixedDelay(
                () -> pollAndPush(runtime),
                POLL_INTERVAL_MILLIS,
                POLL_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
        runtime.pollFuture().set(pollFuture);
    }

    /**
     * 执行单次增量轮询并推送消息事件
     *
     * @param runtime 会话运行态
     */
    private void pollAndPush(@NotNull SessionRuntime runtime) {
        if (runtime.closed().get())
            return;

        WebSocketSession session = runtime.session();
        if (!session.isOpen()) {
            cleanupRuntime(session.getId());
            return;
        }

        Long ticketId = runtime.context().subscribedTicketId();
        if (ticketId == null)
            return;

        try {
            Long afterMessageId = runtime.lastPushedMessageId().get() <= 0 ? null : runtime.lastPushedMessageId().get();
            List<TicketMessageView> messageList = ticketWsService.listIncrementalMessages(
                    runtime.context().actorUserId(),
                    runtime.context().agent(),
                    ticketId,
                    afterMessageId,
                    POLL_BATCH_SIZE
            );

            for (TicketMessageView messageView : messageList) {
                if (messageView.id() == null || messageView.id() <= 0)
                    continue;

                runtime.lastPushedMessageId().set(Math.max(runtime.lastPushedMessageId().get(), messageView.id()));
                TicketMessageRespond messageRespond = AdminTicketRespondAssembler.toMessageRespond(messageView);
                WsEventType eventType = detectMessageEventType(messageView);
                String eventId = MESSAGE_EVENT_ID_PREFIX + messageView.id();
                sendEventFrame(runtime, eventId, eventType, messageView.ticketId(), runtime.context().subscribedTicketNo(), messageRespond);
            }
        } catch (TicketWsConnectException ex) {
            sendErrorAndClose(session, ex.getCode(), ex.getMessage(), toFallback(ex), CloseStatus.POLICY_VIOLATION);
        } catch (Exception ex) {
            log.warn("customerservice ws poll failed, sessionId={}", session.getId(), ex);
            sendErrorAndClose(session, WsErrorCode.RESUME_NOT_AVAILABLE, "服务端处理异常, 请稍后重试", null, CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 推送连接确认事件帧
     *
     * @param runtime 会话运行态
     */
    private void sendConnectAck(@NotNull SessionRuntime runtime) {
        CsWsConnectAckRespond ackData = CsWsConnectAckRespond.builder()
                .connectionId(runtime.connectionId())
                .serverTime(LocalDateTime.now())
                .heartbeatIntervalSeconds(runtime.context().heartbeatIntervalSeconds())
                .resumeTtlSeconds(runtime.context().resumeTtlSeconds())
                .build();

        String eventId = CONNECT_EVENT_ID_PREFIX + runtime.connectionId();
        sendEventFrame(
                runtime,
                eventId,
                WsEventType.WS_CONNECTED,
                runtime.context().subscribedTicketId(),
                runtime.context().subscribedTicketNo(),
                ackData
        );
    }

    /**
     * 发送事件帧
     *
     * @param runtime   会话运行态
     * @param eventId   事件 ID
     * @param eventType 事件类型
     * @param ticketId  工单 ID, 可为空
     * @param ticketNo  工单编号, 可为空
     * @param data      事件数据
     */
    private void sendEventFrame(@NotNull SessionRuntime runtime,
                                @NotNull String eventId,
                                @NotNull WsEventType eventType,
                                @Nullable Long ticketId,
                                @Nullable String ticketNo,
                                @NotNull Object data) {
        CsWsEventEnvelopeRespond<Object> envelope = CsWsEventEnvelopeRespond.builder()
                .eventId(eventId)
                .seq(runtime.seq().incrementAndGet())
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .ticketId(ticketId)
                .ticketNo(ticketNo)
                .traceId(null)
                .data(data)
                .build();

        CsWsServerEventFrameRespond<Object> frame = CsWsServerEventFrameRespond.builder()
                .type(WsFrameType.EVENT)
                .event(envelope)
                .build();

        sendFrame(runtime.session(), frame);
    }

    /**
     * 发送错误帧
     *
     * @param session  WebSocket 会话
     * @param code     错误码
     * @param message  错误消息
     * @param fallback 降级建议, 可为空
     */
    private void sendErrorFrame(@NotNull WebSocketSession session,
                                @NotNull WsErrorCode code,
                                @NotNull String message,
                                @Nullable CsWsResumeFallbackRespond fallback) {
        CsWsServerErrorFrameRespond frame = CsWsServerErrorFrameRespond.builder()
                .type(WsFrameType.ERROR)
                .code(code)
                .message(message)
                .occurredAt(LocalDateTime.now())
                .lastEventId(null)
                .fallback(fallback)
                .build();
        sendFrame(session, frame);
    }

    /**
     * 发送错误帧并关闭连接
     *
     * @param session     WebSocket 会话
     * @param code        错误码
     * @param message     错误消息
     * @param fallback    降级建议, 可为空
     * @param closeStatus 关闭状态
     */
    private void sendErrorAndClose(@NotNull WebSocketSession session,
                                   @NotNull WsErrorCode code,
                                   @NotNull String message,
                                   @Nullable CsWsResumeFallbackRespond fallback,
                                   @NotNull CloseStatus closeStatus) {
        sendErrorFrame(session, code, message, fallback);
        closeQuietly(session, closeStatus);
        cleanupRuntime(session.getId());
    }

    /**
     * 发送服务端帧对象
     *
     * @param session WebSocket 会话
     * @param frame   帧对象
     */
    private void sendFrame(@NotNull WebSocketSession session, @NotNull Object frame) {
        if (!session.isOpen())
            return;
        try {
            String payload = objectMapper.writeValueAsString(frame);
            synchronized (session) {
                if (session.isOpen())
                    session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException ex) {
            cleanupRuntime(session.getId());
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 清理会话运行态
     *
     * @param sessionId 会话 ID
     */
    private void cleanupRuntime(@NotNull String sessionId) {
        SessionRuntime runtime = runtimeMap.remove(sessionId);
        if (runtime == null)
            return;

        runtime.closed().set(true);
        ScheduledFuture<?> pollFuture = runtime.pollFuture().getAndSet(null);
        if (pollFuture != null)
            pollFuture.cancel(true);
    }

    /**
     * 安静关闭会话
     *
     * @param session     WebSocket 会话
     * @param closeStatus 关闭状态
     */
    private void closeQuietly(@NotNull WebSocketSession session,
                              @NotNull CloseStatus closeStatus) {
        if (!session.isOpen())
            return;
        try {
            session.close(closeStatus);
        } catch (IOException ignored) {
            // ignore
        }
    }

    /**
     * 解析连接查询参数
     *
     * @param uri 连接 URI
     * @return 规范化查询参数
     */
    private @NotNull ConnectQuery parseConnectQuery(@Nullable URI uri) {
        require(uri != null, "WebSocket 连接 URI 不能为空");

        MultiValueMap<String, String> queryMap = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String wsToken = normalizeNotNullField(
                queryMap.getFirst("ws_token"),
                "ws_token 不能为空",
                value -> value.length() <= 2048,
                "ws_token 长度不能超过 2048 个字符"
        );

        String lastEventId = normalizeNullableField(
                queryMap.getFirst("last_event_id"),
                "last_event_id 不能为空",
                value -> value.length() <= 64,
                "last_event_id 长度不能超过 64 个字符"
        );

        String ticketNo = normalizeNullableField(
                queryMap.getFirst("ticket_no"),
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度必须在 10 到 32 个字符之间"
        );

        Long ticketId = parseOptionalPositiveLong(queryMap.getFirst("ticket_id"), "ticket_id");

        return new ConnectQuery(wsToken, lastEventId, ticketNo, ticketId);
    }

    /**
     * 解析可为空的正整数查询参数
     *
     * @param rawValue 原始参数
     * @param field    字段名
     * @return 解析结果, 可为空
     */
    private @Nullable Long parseOptionalPositiveLong(@Nullable String rawValue,
                                                     @NotNull String field) {
        if (rawValue == null)
            return null;

        String normalized = normalizeNotNullField(rawValue, field + " 不能为空", value -> value.length() <= 32, field + " 长度不能超过 32 个字符");
        long parsed;
        try {
            parsed = Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            throw IllegalParamException.of(field + " 必须为整数");
        }
        require(parsed >= 1, field + " 必须大于等于 1");
        return parsed;
    }

    /**
     * 将连接异常转换为续传降级建议
     *
     * @param exception 连接异常
     * @return 降级建议, 可为空
     */
    private @Nullable CsWsResumeFallbackRespond toFallback(@NotNull TicketWsConnectException exception) {
        if (exception.getFallbackStrategy() == null)
            return null;
        return CsWsResumeFallbackRespond.builder()
                .strategy(exception.getFallbackStrategy())
                .suggestAfterId(exception.getSuggestAfterId())
                .retryAfterSeconds(exception.getRetryAfterSeconds())
                .build();
    }

    /**
     * 判断消息事件类型
     *
     * @param messageView 消息视图
     * @return 事件类型
     */
    private @NotNull WsEventType detectMessageEventType(@NotNull TicketMessageView messageView) {
        if (messageView.recalledAt() != null)
            return WsEventType.MESSAGE_RECALLED;
        if (messageView.editedAt() != null)
            return WsEventType.MESSAGE_UPDATED;
        return WsEventType.MESSAGE_CREATED;
    }

    /**
     * 生成连接 ID
     *
     * @return 连接 ID
     */
    private @NotNull String buildConnectionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 会话运行态
     *
     * @param connectionId        连接 ID
     * @param session             WebSocket 会话
     * @param context             连接上下文
     * @param lastPushedMessageId 最近已推送消息 ID
     * @param seq                 连接内事件序号
     * @param lastAckEventId      客户端最近确认事件 ID
     * @param pollFuture          增量轮询任务句柄
     * @param closed              是否已关闭
     */
    private record SessionRuntime(@NotNull String connectionId,
                                  @NotNull WebSocketSession session,
                                  @NotNull TicketWsConnectionContext context,
                                  @NotNull AtomicLong lastPushedMessageId,
                                  @NotNull AtomicLong seq,
                                  @NotNull AtomicReference<String> lastAckEventId,
                                  @NotNull AtomicReference<ScheduledFuture<?>> pollFuture,
                                  @NotNull AtomicBoolean closed) {

        /**
         * 创建会话运行态
         *
         * @param connectionId         连接 ID
         * @param session              WebSocket 会话
         * @param context              连接上下文
         * @param initialLastMessageId 初始化消息锚点
         */
        private SessionRuntime(@NotNull String connectionId,
                               @NotNull WebSocketSession session,
                               @NotNull TicketWsConnectionContext context,
                               long initialLastMessageId) {
            this(
                    connectionId,
                    session,
                    context,
                    new AtomicLong(initialLastMessageId),
                    new AtomicLong(0L),
                    new AtomicReference<>(),
                    new AtomicReference<>(),
                    new AtomicBoolean(false)
            );
        }

        /**
         * 比较两个运行态对象是否相等
         *
         * @param other 目标对象
         * @return 是否相等
         */
        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof SessionRuntime that))
                return false;
            return Objects.equals(connectionId, that.connectionId)
                    && Objects.equals(session.getId(), that.session.getId());
        }

        /**
         * 计算运行态哈希值
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(connectionId, session.getId());
        }
    }
}

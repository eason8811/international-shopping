package shopping.international.infrastructure.adapter.port.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.infrastructure.gateway.payment.IPayPalApi;
import shopping.international.infrastructure.gateway.payment.dto.*;
import shopping.international.types.config.PayPalProperties;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.URI;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * PayPal 端口实现 (Retrofit + Redis 防重放)
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(PayPalProperties.class)
public class PayPalPort implements IPayPalPort {

    /**
     * Redis 防重放 key 前缀
     */
    private static final String WEBHOOK_DEDUPE_PREFIX = "paypal:webhook:dedupe:";
    /**
     * Create order Path
     */
    public static final URI CREATE_ORDER_TEMPLATE = URI.of("/v2/checkout/orders");
    /**
     * Show order details Path
     */
    private static final URI SHOW_ORDER_DETAILS_PATH_TEMPLATE = URI.of("/v2/checkout/orders/{id}");
    /**
     * Capture payment for order Path
     */
    public static final URI CAPTURE_PAYMENT_ORDER_TEMPLATE = URI.of("/v2/checkout/orders/{id}/capture");
    /**
     * Refund captured payment Path
     */
    public static final URI REFUND_CAPTURED_PAYMENT_TEMPLATE = URI.of("/v2/payments/captures/{capture_id}/refund");
    /**
     * Verify webhook signature Path
     */
    public static final URI VERIFY_WEBHOOK_SIGNATURE_TEMPLATE = URI.of("/v1/notifications/verify-webhook-signature");
    /**
     * 获取 Access Token URI
     */
    public static final URI GET_ACCESS_TOKEN_TEMPLATE = URI.of("/v1/oauth2/token");
    /**
     * PayPal API (Retrofit)
     */
    private final IPayPalApi api;
    /**
     * JSON 序列化/反序列化器
     */
    private final ObjectMapper objectMapper;
    /**
     * Redis (用于防重放)
     */
    private final StringRedisTemplate redis;
    /**
     * PayPal 配置
     */
    private final PayPalProperties properties;
    /**
     * 货币配置服务 (用于 minor/major 换算)
     */
    private final ICurrencyConfigService currencyConfigService;
    /**
     * AccessToken 缓存 (简单内存缓存, 避免频繁换取)
     */
    private volatile TokenCache tokenCache;

    /**
     * 创建 PayPal Order (用于生成收银台跳转链接)
     *
     * @param cmd 创建订单命令
     * @return 创建结果
     */
    @Override
    public @NotNull CreateOrderResult createOrder(@NotNull CreateOrderCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");
        String bearer = "Bearer " + requireAccessToken();

        String amountMajor = currencyConfigService.get(cmd.currency()).toMajor(cmd.amountMinor()).toPlainString();
        PayPalCreateOrderRequest request = new PayPalCreateOrderRequest(
                "CAPTURE",
                List.of(new PayPalCreateOrderRequest.PurchaseUnit(new PayPalAmount(cmd.currency(), amountMajor))),
                new PayPalCreateOrderRequest.ApplicationContext(cmd.returnUrl(), cmd.cancelUrl(), "PAY_NOW")
        );

        String url = properties.getBaseUrl() + CREATE_ORDER_TEMPLATE;
        PayPalCreateOrderRespond resp = executeOrThrow(
                api.createOrder(
                        url,
                        bearer,
                        cmd.idempotencyKey(),
                        "return=representation",
                        request
                ),
                "创建 PayPal Order 失败"
        );

        String approveUrl = findApproveUrl(resp.getLinks())
                .orElseThrow(() -> new IllegalParamException("PayPal 响应缺少 approve 链接"));
        return new CreateOrderResult(
                requireNotBlankValue(resp.getId(), "PayPal Order ID 为空"),
                approveUrl,
                toJson(request),
                toJson(resp)
        );
    }

    /**
     * 查询 PayPal Order (用于复用已有 externalId 获取 approve link 或状态)
     *
     * @param paypalOrderId PayPal Order ID
     * @return 查询结果
     */
    @Override
    public @NotNull GetOrderResult getOrder(@NotNull String paypalOrderId) {
        requireNotBlank(paypalOrderId, "paypalOrderId 不能为空");
        String bearer = "Bearer " + requireAccessToken();

        String url = properties.getBaseUrl() + SHOW_ORDER_DETAILS_PATH_TEMPLATE.fill("id", paypalOrderId);
        PayPalGetOrderRespond resp = executeOrThrow(api.getOrder(url, bearer), "查询 PayPal Order 失败");

        String approveUrl = findApproveUrl(resp.getLinks()).orElse(null);
        CaptureInfo capture = firstCapture(resp.getPurchaseUnits());

        return new GetOrderResult(
                requireNotBlankValue(resp.getId(), "PayPal Order ID 为空"),
                resp.getStatus() == null ? "" : resp.getStatus(),
                approveUrl,
                capture == null ? null : capture.captureId,
                capture == null ? null : capture.captureTime,
                toJson(resp)
        );
    }

    /**
     * Capture PayPal Order (确认扣款)
     *
     * @param cmd Capture 命令
     * @return Capture 结果
     */
    @Override
    public @NotNull CaptureOrderResult captureOrder(@NotNull CaptureOrderCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");
        requireNotBlank(cmd.paypalOrderId(), "paypalOrderId 不能为空");
        String bearer = "Bearer " + requireAccessToken();

        String url = properties.getBaseUrl() + CAPTURE_PAYMENT_ORDER_TEMPLATE.fill("id", cmd.paypalOrderId());
        String requestId = cmd.idempotencyKey() == null || cmd.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : cmd.idempotencyKey();

        Map<String, Object> body = new LinkedHashMap<>();
        if (cmd.note() != null && !cmd.note().isBlank())
            body.put("note_to_payer", cmd.note().strip());

        PayPalCaptureOrderRespond resp = executeOrThrow(api.captureOrder(
                url,
                bearer,
                requestId,
                "return=representation",
                body
        ), "Capture PayPal Order 失败");

        CaptureInfo capture = firstCapture(resp.getPurchaseUnits());
        return new CaptureOrderResult(
                requireNotBlankValue(resp.getId(), "PayPal Order ID 为空"),
                capture == null ? null : capture.captureId,
                capture == null ? null : capture.captureTime,
                resp.getStatus() == null ? "" : resp.getStatus(),
                toJson(body),
                toJson(resp)
        );
    }

    /**
     * Refund PayPal Capture (按 capture_id 退款)
     *
     * @param cmd Refund 命令
     * @return Refund 结果
     */
    @Override
    public @NotNull RefundCaptureResult refundCapture(@NotNull RefundCaptureCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");
        requireNotBlank(cmd.captureId(), "captureId 不能为空");
        String bearer = "Bearer " + requireAccessToken();

        String amountMajor = currencyConfigService.get(cmd.currency()).toMajor(cmd.amountMinor()).toPlainString();
        PayPalRefundCaptureRequest request = new PayPalRefundCaptureRequest(
                new PayPalAmount(cmd.currency(), amountMajor),
                cmd.note()
        );

        String url = properties.getBaseUrl() + REFUND_CAPTURED_PAYMENT_TEMPLATE.fill("capture_id", cmd.captureId());
        String requestId = cmd.idempotencyKey() == null || cmd.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : cmd.idempotencyKey();

        PayPalRefundCaptureRespond resp = executeOrThrow(
                api.refundCapture(
                        url,
                        bearer,
                        requestId,
                        "return=representation",
                        request
                ),
                "Refund PayPal Capture 失败");

        return new RefundCaptureResult(
                cmd.captureId(),
                resp.getId(),
                resp.getStatus() == null ? "" : resp.getStatus(),
                toJson(request),
                toJson(resp)
        );
    }

    /**
     * 校验 PayPal Webhook 请求来源 (验签 + 防重放)
     *
     * <p>建议在通过该方法校验后, 才进入领域逻辑更新本地支付单与订单冗余字段</p>
     *
     * @param cmd 验签命令 (包含签名相关 Header 与 webhook_event body)
     */
    @Override
    public void verifyWebhookAndReplayProtection(@NotNull VerifyWebhookCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");
        requireNotBlank(cmd.eventIdForDedupe(), "eventId 不能为空");

        // 1) 防重放：transmission_id + event_id 组合去重
        String dedupeKey = WEBHOOK_DEDUPE_PREFIX + cmd.transmissionId() + ":" + cmd.eventIdForDedupe();
        boolean first = markOnce(dedupeKey, cmd.replayTtl());
        if (!first)
            return;

        // 2) 时钟偏差校验 (避免过旧/过未来的请求)
        validateTransmissionTime(cmd.transmissionTime());

        // 3) 调用 PayPal verify-webhook-signature
        requireNotBlank(properties.getWebhookId(), "paypal.webhook-id 未配置");
        String bearer = "Bearer " + requireAccessToken();
        PayPalVerifyWebhookSignatureRequest body = new PayPalVerifyWebhookSignatureRequest(
                cmd.authAlgo(),
                cmd.certUrl(),
                cmd.transmissionId(),
                cmd.transmissionSig(),
                cmd.transmissionTime(),
                properties.getWebhookId(),
                cmd.webhookEvent()
        );

        String url = properties.getBaseUrl() + VERIFY_WEBHOOK_SIGNATURE_TEMPLATE;
        PayPalVerifyWebhookSignatureRespond resp = executeOrThrow(api.verifyWebhookSignature(url, bearer, body), "PayPal Webhook 验签失败");
        if (resp.getVerificationStatus() == null || !"SUCCESS".equalsIgnoreCase(resp.getVerificationStatus()))
            throw new IllegalParamException("PayPal Webhook 验签失败: " + resp.getVerificationStatus());
    }

    /**
     * 从 PayPal Webhook event 中尽可能提取 PayPal Order ID (external_id)
     *
     * <p>不同 event_type 的资源结构不一致, 因此返回 Optional</p>
     *
     * @param webhookEvent webhook_event
     * @return PayPal Order ID (若可提取)
     */
    @Override
    public @NotNull Optional<String> tryExtractPayPalOrderId(@NotNull Map<String, Object> webhookEvent) {
        requireNotNull(webhookEvent, "webhookEvent 不能为空");

        Object eventTypeObj = webhookEvent.get("event_type");
        String eventType = eventTypeObj == null ? "" : String.valueOf(eventTypeObj);

        Map<String, Object> resource = asMap(webhookEvent.get("resource"));
        if (resource == null)
            return Optional.empty();

        // 1) CHECKOUT.ORDER.*：resource.id 通常为 order_id
        if (eventType.toUpperCase(Locale.ROOT).contains("CHECKOUT.ORDER")) {
            String id = asString(resource.get("id"));
            if (id != null && !id.isBlank())
                return Optional.of(id.strip());
        }

        // 2) PAYMENT.CAPTURE.*：order_id 通常位于 resource.supplementary_data.related_ids.order_id
        String nested = nestedString(resource, "supplementary_data", "related_ids", "order_id");
        if (nested != null && !nested.isBlank())
            return Optional.of(nested.strip());

        // 3) 兜底：若 resource.id 存在也返回 (可能是 order_id)
        String id = asString(resource.get("id"));
        if (id != null && !id.isBlank())
            return Optional.of(id.strip());

        return Optional.empty();
    }

    // ========================= 内部辅助 =========================

    /**
     * 获取 PayPal 的访问令牌(access token), 该方法确保返回的 access token 是有效的, 如果当前缓存中的 token 即将过期或已过期,
     * 则会重新请求一个新的 token 并更新缓存
     *
     * @return 有效的 PayPal 访问令牌
     * @throws IllegalParamException 如果无法获取到有效 token 或者客户端 ID 和密钥未正确配置
     */
    private @NotNull String requireAccessToken() {
        TokenCache cached = tokenCache;
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMs > now + 30_000)
            return cached.token;

        synchronized (this) {
            cached = tokenCache;
            now = System.currentTimeMillis();
            if (cached != null && cached.expiresAtMs > now + 30_000)
                return cached.token;

            requireNotBlank(properties.getClientId(), "paypal.client-id 未配置");
            requireNotBlank(properties.getClientSecret(), "paypal.client-secret 未配置");

            String basic = Base64.getEncoder().encodeToString((properties.getClientId() + ":" + properties.getClientSecret())
                    .getBytes(StandardCharsets.UTF_8));
            String url = properties.getBaseUrl() + GET_ACCESS_TOKEN_TEMPLATE;
            PayPalAccessTokenRespond resp = executeOrThrow(api.token(url, "Basic " + basic, "client_credentials"), "获取 PayPal AccessToken 失败");

            String token = resp.getAccessToken();
            if (token == null || token.isBlank())
                throw new IllegalParamException("PayPal access_token 为空");
            long expiresIn = resp.getExpiresIn() == null ? 300L : resp.getExpiresIn();
            tokenCache = new TokenCache(token, now + expiresIn * 1000L);
            return token;
        }
    }

    /**
     * 执行给定的 <code>Call</code> 并根据响应结果抛出异常或返回响应体
     *
     * <p>该方法会尝试执行传入的 <code>Call</code>, 如果响应状态码指示失败, 或者响应体为空,
     * 则会抛出一个带有指定错误信息的 {@link IllegalParamException} 异常
     * 如果执行过程中遇到其他异常, 也会被包装成 {@link IllegalParamException} 抛出</p>
     *
     * @param <T>  响应体类型
     * @param call 要执行的 <code>Call</code>
     * @param msg  在发生错误时附加到异常消息中的自定义错误信息
     * @return 成功执行后从响应中获取的非空响应体
     * @throws IllegalParamException 当响应状态码指示请求失败, 响应体为空, 或者在执行 <code>Call</code> 期间出现任何异常时抛出
     */
    private <T> T executeOrThrow(Call<T> call, String msg) {
        try {
            Response<T> resp = call.execute();
            if (!resp.isSuccessful()) {
                String err = null;
                try (ResponseBody body = resp.errorBody()) {
                    if (body != null)
                        err = body.string();
                    throw new IllegalParamException(msg + ", http 代码: " + resp.code() + ", 错误响应体: " + err);
                }
            }
            T body = resp.body();
            if (body == null)
                throw new IllegalParamException(msg + ", 响应体为空");
            return body;
        } catch (Exception e) {
            if (e instanceof IllegalParamException)
                throw (IllegalParamException) e;
            throw new IllegalParamException(msg + ": " + e.getMessage());
        }
    }

    /**
     * 将给定的对象转换为 JSON 字符串
     *
     * @param any 待转换为 JSON 的对象
     * @return 转换后的 JSON 字符串, 如果转换过程中发生异常, 则返回 {@code "{}"}
     */
    private String toJson(Object any) {
        try {
            return objectMapper.writeValueAsString(any);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 从给定的 PayPal 链接列表中查找并返回 approve URL
     *
     * <p>该方法遍历传入的链接列表, 查找关系为 {@code "approve"} 的链接, 并返回其 href 值, 如果没有找到符合条件的链接, 则返回空 {@code Optional}</p>
     *
     * @param links 一个可能为空或包含 {@link PayPalLink} 对象的列表, 每个对象代表 PayPal API 返回的一个链接
     * @return 包含批准链接的 {@code Optional<String>} (如果找到), 或者空 {@code Optional} (如果没有找到)
     */
    private Optional<String> findApproveUrl(@Nullable List<PayPalLink> links) {
        if (links == null || links.isEmpty())
            return Optional.empty();
        for (PayPalLink link : links) {
            if (link == null)
                continue;
            if (link.getRel() != null && "approve".equalsIgnoreCase(link.getRel()) && link.getHref() != null && !link.getHref().isBlank())
                return Optional.of(link.getHref());
        }
        return Optional.empty();
    }

    /**
     * @param captureId   捕获 ID
     * @param captureTime 捕获时间
     */
    private record CaptureInfo(String captureId, OffsetDateTime captureTime) {
    }

    /**
     * 从给定的购买单元列表中尝试获取第一个捕获信息
     *
     * <p>该方法遍历传入的购买单元列表, 并尝试通过反射调用每个对象的 <code>getPayments</code> 和 <code>getCaptures</code>
     * 方法来获取捕获信息, 如果找到有效的捕获信息, 则返回包含捕获 ID 和创建时间的 {@link CaptureInfo} 对象</p>
     *
     * @param purchaseUnits 购买单元列表, 可以为空或包含任何实现了 <code>getPayments</code> 方法的对象
     * @return 如果找到有效的捕获信息, 则返回一个 {@link CaptureInfo} 对象; 否则返回 null
     */
    private @Nullable CaptureInfo firstCapture(@Nullable List<?> purchaseUnits) {
        if (purchaseUnits == null || purchaseUnits.isEmpty())
            return null;

        // 支持 PayPalGetOrderRespond 与 PayPalCaptureOrderRespond 的相同结构
        for (Object pu : purchaseUnits) {
            if (pu == null)
                continue;
            try {
                Object payments = pu.getClass().getMethod("getPayments").invoke(pu);
                if (payments == null)
                    continue;
                Object captures = payments.getClass().getMethod("getCaptures").invoke(payments);
                if (!(captures instanceof List<?> list) || list.isEmpty())
                    continue;
                Object cap = list.get(0);
                if (cap == null)
                    continue;
                String id = (String) cap.getClass().getMethod("getId").invoke(cap);
                OffsetDateTime createTime = (OffsetDateTime) cap.getClass().getMethod("getCreateTime").invoke(cap);
                if (id != null && !id.isBlank())
                    return new CaptureInfo(id, createTime);
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }

    /**
     * 尝试在 Redis 中设置一个键值对, 仅当该键不存在时才设置成功
     *
     * <p>此方法用于实现简单的去重逻辑, 例如在处理 Webhook 请求时防止重复处理相同的事件</p>
     *
     * @param key 要设置的键, 必须非空
     * @param ttl 设置的键值对的有效时间, 必须非空
     * @return 如果键被成功设置(即之前不存在), 则返回 true; 否则返回 false
     */
    private boolean markOnce(@NotNull String key, @NotNull Duration ttl) {
        Boolean ok = redis.execute((RedisCallback<Boolean>) connection -> {
            byte[] k = key.getBytes(StandardCharsets.UTF_8);
            byte[] v = "1".getBytes(StandardCharsets.UTF_8);
            return connection.stringCommands().set(
                    k,
                    v,
                    Expiration.from(ttl),
                    RedisStringCommands.SetOption.SET_IF_ABSENT
            );
        });
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 校验 PayPal Webhook 传输时间的有效性
     *
     * <p>该方法解析传入的传输时间, 并与当前 UTC 时间进行比较, 如果两者之间的偏差超过了预设或配置的最大允许偏差, 则抛出异常</p>
     *
     * @param transmissionTime 从 PayPal Webhook 事件中接收到的传输时间字符串, 必须非空
     * @throws IllegalParamException 如果传输时间超出允许的时间偏差范围, 或者无法解析给定的传输时间字符串
     */
    private void validateTransmissionTime(@NotNull String transmissionTime) {
        try {
            OffsetDateTime tt = OffsetDateTime.parse(transmissionTime);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            Duration skew = properties.getClockSkew() == null ? Duration.ofMinutes(5) : properties.getClockSkew();
            Duration diff = Duration.between(tt, now).abs();
            if (diff.compareTo(skew) > 0)
                throw new IllegalParamException("PayPal Webhook transmission_time 超出允许偏差: " + diff);
        } catch (IllegalParamException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalParamException("PayPal Webhook transmission_time 解析失败");
        }
    }

    /**
     * 将给定的对象转换为一个 <code>Map<String, Object></code>, 如果对象是 <code>Map</code> 类型,
     * 则会尝试将其键值对复制到一个新的 <code>LinkedHashMap</code> 中, 其中所有键都会被转换成字符串形式
     * 如果原 <code>Map</code> 中的键为 <code>null</code>, 则该键值对会被忽略
     *
     * @param o 要转换的对象
     * @return 如果输入对象是 <code>Map</code> 类型, 返回转换后的 <code>Map<String, Object></code>; 否则返回 <code>null</code>
     */
    private static @Nullable Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null)
                    continue;
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return null;
    }

    /**
     * 将给定的对象转换为字符串形式
     *
     * <p>如果对象为 {@code null}, 则返回 {@code null}否则, 使用 {@link String#valueOf(Object)} 方法将对象转换为字符串</p>
     *
     * @param o 待转换的对象
     * @return 转换后的字符串, 如果输入对象为 {@code null}, 则返回 {@code null}
     */
    private static @Nullable String asString(Object o) {
        if (o == null)
            return null;
        return String.valueOf(o);
    }

    /**
     * 从给定的映射中, 根据提供的路径逐层查找并返回最终值作为字符串
     *
     * <p>此方法接收一个映射和一个可变参数路径, 沿着路径在映射中查找值, 如果在任何点上找不到对应键或者找到的值不是映射,
     * 则返回 {@code null}, 如果成功遍历完整个路径, 则尝试将最后一个值转换为字符串后返回</p>
     *
     * @param map  映射, 从中开始查找路径
     * @param path 可变参数列表, 表示要遍历的键路径
     * @return 如果能够沿着指定路径找到非空值, 并且该值可以被成功转换为字符串, 则返回这个字符串; 否则返回 {@code null}
     */
    private static @Nullable String nestedString(@NotNull Map<String, Object> map, String... path) {
        Object cur = map;
        for (String p : path) {
            if (!(cur instanceof Map<?, ?> m))
                return null;
            cur = m.get(p);
            if (cur == null)
                return null;
        }
        return asString(cur);
    }

    /**
     * 用于存储和管理 PayPal 访问令牌(access token)及其过期时间的记录类
     *
     * <p>此类提供了一个简单的结构来保存从 PayPal 获取的访问令牌以及该令牌的有效截止时间(以毫秒为单位)
     * 这有助于在处理 PayPal API 请求时确保使用的令牌是有效的, 并且能够根据需要更新缓存中的令牌</p>
     *
     * @param token       访问令牌, 必须非空
     * @param expiresAtMs 令牌过期时间(毫秒), 时间戳
     */
    private record TokenCache(@NotNull String token, long expiresAtMs) {
    }

    /**
     * 检查给定的字符串是否非空且不只包含空白字符, 如果是则抛出异常
     *
     * <p>该方法用于确保传入的字符串值既不是 null 也不是仅由空白字符组成， 如果输入字符串不符合要求,
     * 则会抛出一个 {@link IllegalParamException}, 其中包含提供的错误信息</p>
     *
     * @param value 待检查的字符串, 可以为 null 或者为空白字符串
     * @param msg   当 value 为 null 或者空白时要抛出的异常信息, 必须非空
     * @return 如果 value 非空且不只包含空白字符, 则返回 value 的 trim 版本
     * @throws IllegalParamException 如果 value 为 null 或者空白, 抛出此异常并附带提供的 msg 作为错误信息
     */
    private static @NotNull String requireNotBlankValue(@Nullable String value, @NotNull String msg) {
        requireNotBlank(value, msg);
        return value.strip();
    }
}

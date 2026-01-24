package shopping.international.trigger.controller.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.payment.PaymentDetailRespond;
import shopping.international.api.resp.payment.PaymentListItemRespond;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.payment.AdminPaymentDetail;
import shopping.international.domain.model.vo.payment.AdminPaymentListItemView;
import shopping.international.domain.model.vo.payment.AdminPaymentSearchCriteria;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.domain.service.payment.IAdminPaymentService;
import shopping.international.domain.service.payment.impl.PaymentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理侧支付单接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/payments")
public class AdminPaymentController {

    /**
     * 管理侧支付查询服务
     */
    private final IAdminPaymentService adminPaymentService;

    /**
     * 支付领域服务 (用于 ops sync)
     */
    private final IPaymentService paymentService;

    /**
     * 货币配置服务 (用于金额展示换算)
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * JSON 工具 (用于 payload 字符串解析)
     */
    private final ObjectMapper objectMapper;

    /**
     * 管理侧分页查询支付单
     *
     * @param orderNo     订单号
     * @param externalId  外部ID
     * @param channel     支付渠道
     * @param status      支付状态
     * @param createdFrom 创建时间起始点(格式: yyyy-MM-dd HH:mm:ss)
     * @param createdTo   创建时间结束点(格式: yyyy-MM-dd HH:mm:ss)
     * @param page        页码, 默认值为 1
     * @param size        每页显示条数, 默认值为 20, 最小值为 1, 最大值为 200
     * @return 包含 {@link PaymentListItemRespond} 列表的 {@link ResponseEntity}, 其中还包含了分页元数据如总条目数等
     */
    @GetMapping
    public ResponseEntity<Result<List<PaymentListItemRespond>>> list(@RequestParam(value = "order_no", required = false) String orderNo,
                                                                     @RequestParam(value = "external_id", required = false) String externalId,
                                                                     @RequestParam(value = "channel", required = false) String channel,
                                                                     @RequestParam(value = "status", required = false) String status,
                                                                     @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                     @RequestParam(value = "created_to", required = false) String createdTo,
                                                                     @RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        PaymentChannel channelEnum = parseEnum(channel, PaymentChannel.class);
        PaymentStatus statusEnum = parseEnum(status, PaymentStatus.class);
        LocalDateTime from = parseDateTime(createdFrom);
        LocalDateTime to = parseDateTime(createdTo);

        AdminPaymentSearchCriteria criteria = AdminPaymentSearchCriteria.builder()
                .orderNo(orderNo)
                .externalId(externalId)
                .channel(channelEnum)
                .status(statusEnum)
                .createdFrom(from)
                .createdTo(to)
                .build();

        PageQuery pageQuery = PageQuery.of(page, Math.min(Math.max(size, 1), 200), 200);
        PageResult<AdminPaymentListItemView> pageData = adminPaymentService.pagePayments(criteria, pageQuery);
        List<PaymentListItemRespond> data = pageData.items().stream().map(row -> {
            CurrencyConfig cfg = currencyConfigService.get(row.currency());
            return PaymentListItemRespond.builder()
                    .paymentId(row.paymentId())
                    .orderId(row.orderId())
                    .orderNo(row.orderNo())
                    .externalId(row.externalId())
                    .channel(row.channel())
                    .status(row.status())
                    .amount(cfg.toMajor(row.amountMinor()).toPlainString())
                    .currency(row.currency())
                    .lastPolledAt(row.lastPolledAt())
                    .lastNotifiedAt(row.lastNotifiedAt())
                    .createdAt(row.createdAt())
                    .updatedAt(row.updatedAt())
                    .build();
        }).toList();

        return ResponseEntity.ok(
                Result.ok(data, Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build())
        );
    }

    /**
     * 管理侧查询支付单详情
     *
     * @param paymentId 支付单ID, 用于标识一个特定的支付记录
     * @return 包含支付单详情的响应实体, 如果找不到对应的支付单, 则返回 {@code NOT_FOUND} 状态码及错误消息
     */
    @GetMapping("/{payment_id}")
    public ResponseEntity<Result<PaymentDetailRespond>> detail(@PathVariable("payment_id") Long paymentId) {
        AdminPaymentDetail detail = adminPaymentService.getPaymentDetail(paymentId);
        CurrencyConfig cfg = currencyConfigService.get(detail.currency());

        PaymentDetailRespond resp = PaymentDetailRespond.builder()
                .paymentId(detail.paymentId())
                .orderId(detail.orderId())
                .orderNo(detail.orderNo())
                .externalId(detail.externalId())
                .channel(detail.channel())
                .status(detail.status())
                .amount(cfg.toMajor(detail.amountMinor()).toPlainString())
                .currency(detail.currency())
                .requestPayload(toJsonNode(detail.requestPayload()))
                .responsePayload(toJsonNode(detail.responsePayload()))
                .notifyPayload(toJsonNode(detail.notifyPayload()))
                .lastPolledAt(detail.lastPolledAt())
                .lastNotifiedAt(detail.lastNotifiedAt())
                .createdAt(detail.createdAt())
                .updatedAt(detail.updatedAt())
                .build();
        return ResponseEntity.ok(Result.ok(resp));
    }

    /**
     * 运维/排障: 强制查询 PayPal 刷新支付状态
     *
     * @param paymentId 支付 ID, 用于标识要同步的支付记录
     * @return 包含状态码和消息的结果, 成功时返回 accepted 状态
     * @see PaymentService#opsSync(Long)
     */
    @PostMapping("/{payment_id}/sync")
    public ResponseEntity<Result<Void>> sync(@PathVariable("payment_id") Long paymentId) {
        paymentService.opsSync(paymentId);
        return ResponseEntity.status(ApiCode.ACCEPTED.toHttpStatus()).body(Result.accepted("Accepted"));
    }

    /**
     * 将给定的 JSON 字符串转换为 {@code JsonNode} 对象
     *
     * @param json 一个 JSON 格式的字符串 如果为空或空白, 则方法返回 null
     * @return 转换后的 {@code JsonNode} 对象 若转换失败或输入无效, 返回 null
     */
    private @Nullable JsonNode toJsonNode(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将给定的字符串解析为 <code>LocalDateTime</code> 对象
     *
     * @param v 待解析的日期时间字符串 如果为空或仅包含空白字符, 则返回 null
     * @return 解析后的 <code>LocalDateTime</code> 对象 如果输入无效, 抛出异常
     * @throws IllegalParamException 当提供的字符串格式不正确时抛出
     */
    private static @Nullable LocalDateTime parseDateTime(@Nullable String v) {
        if (v == null || v.isBlank())
            return null;
        try {
            return LocalDateTime.parse(v.strip(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new IllegalParamException("时间格式不合法: " + v);
        }
    }

    /**
     * 将字符串转换为指定类型的枚举值
     *
     * @param value    待转换的字符串, 可以为 null
     * @param enumType 目标枚举类型
     * @return 如果 value 为 null 或者空白, 返回 null; 否则返回与 value 匹配的枚举值。如果找不到匹配的枚举值, 则抛出异常
     * @throws IllegalParamException 当 value 不是 enumType 的有效名称时抛出
     */
    private static <E extends Enum<E>> @Nullable E parseEnum(@Nullable String value, Class<E> enumType) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Enum.valueOf(enumType, value.strip());
        } catch (Exception e) {
            throw new IllegalParamException("枚举值不合法: " + value);
        }
    }
}


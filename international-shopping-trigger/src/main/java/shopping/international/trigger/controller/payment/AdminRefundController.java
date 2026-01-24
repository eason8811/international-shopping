package shopping.international.trigger.controller.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.payment.AdminRefundDetailRespond;
import shopping.international.api.resp.payment.RefundListItemRespond;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.payment.AdminRefundDetail;
import shopping.international.domain.model.vo.payment.AdminRefundListItemView;
import shopping.international.domain.model.vo.payment.AdminRefundSearchCriteria;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.payment.IAdminPaymentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理侧退款单接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/refunds")
public class AdminRefundController {

    /**
     * 管理侧支付查询服务
     */
    private final IAdminPaymentService adminPaymentService;

    /**
     * 货币配置服务（用于金额展示换算）
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * JSON 工具（用于 payload 字符串解析）
     */
    private final ObjectMapper objectMapper;

    /**
     * 管理侧分页查询退款单
     *
     * @param orderNo     订单号
     * @param externalId  外部ID
     * @param channel     付款渠道
     * @param status      退款状态
     * @param createdFrom 创建时间起始点(格式: yyyy-MM-dd HH:mm:ss)
     * @param createdTo   创建时间结束点(格式: yyyy-MM-dd HH:mm:ss)
     * @param page        请求的页码, 默认值为 1
     * @param size        每页显示的数量, 默认值为 20, 最小值为 1, 最大值限制在 200
     * @return 包含退款列表及分页信息的 {@link ResponseEntity}
     * <p>
     * 此方法根据提供的查询条件, 如订单号、外部ID等, 返回满足条件的退款记录列表, 同时返回分页相关的信息, 包括总条目数、当前页码以及每页大小
     */
    @GetMapping
    public ResponseEntity<Result<List<RefundListItemRespond>>> list(@RequestParam(value = "order_no", required = false) String orderNo,
                                                                    @RequestParam(value = "external_id", required = false) String externalId,
                                                                    @RequestParam(value = "channel", required = false) String channel,
                                                                    @RequestParam(value = "status", required = false) String status,
                                                                    @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                    @RequestParam(value = "created_to", required = false) String createdTo,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        PaymentChannel channelEnum = parseEnum(channel, PaymentChannel.class);
        RefundStatus statusEnum = parseEnum(status, RefundStatus.class);
        LocalDateTime from = parseDateTime(createdFrom);
        LocalDateTime to = parseDateTime(createdTo);

        AdminRefundSearchCriteria criteria = AdminRefundSearchCriteria.builder()
                .orderNo(orderNo)
                .externalId(externalId)
                .channel(channelEnum)
                .status(statusEnum)
                .createdFrom(from)
                .createdTo(to)
                .build();

        PageQuery pageQuery = PageQuery.of(page, Math.min(Math.max(size, 1), 200), 200);
        PageResult<AdminRefundListItemView> pageData = adminPaymentService.pageRefunds(criteria, pageQuery);
        List<RefundListItemRespond> data = pageData.items().stream().map(row -> {
            CurrencyConfig cfg = currencyConfigService.get(row.currency());
            return RefundListItemRespond.builder()
                    .refundId(row.refundId())
                    .refundNo(row.refundNo())
                    .orderId(row.orderId())
                    .orderNo(row.orderNo())
                    .paymentId(row.paymentId())
                    .externalId(row.externalId())
                    .channel(row.channel())
                    .status(row.status())
                    .amount(cfg.toMajor(row.amountMinor()).toPlainString())
                    .currency(row.currency())
                    .createdAt(row.createdAt())
                    .updatedAt(row.updatedAt())
                    .build();
        }).toList();

        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * 管理侧查询退款单详情
     *
     * @param refundId 退款 ID
     * @return 包含退款详情的 {@link ResponseEntity}, 如果找不到指定退款单, 则返回 NOT_FOUND 状态码及错误信息
     */
    @GetMapping("/{refund_id}")
    public ResponseEntity<Result<AdminRefundDetailRespond>> detail(@PathVariable("refund_id") Long refundId) {
        AdminRefundDetail detail = adminPaymentService.getRefundDetail(refundId);
        CurrencyConfig cfg = currencyConfigService.get(detail.currency());

        AdminRefundDetailRespond resp = AdminRefundDetailRespond.builder()
                .refundId(detail.refundId())
                .refundNo(detail.refundNo())
                .orderId(detail.orderId())
                .orderNo(detail.orderNo())
                .paymentId(detail.paymentId())
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


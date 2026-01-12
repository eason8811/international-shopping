package shopping.international.trigger.controller.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.orders.OrderCancelRequest;
import shopping.international.api.req.orders.OrderChangeAddressRequest;
import shopping.international.api.req.orders.OrderCreateRequest;
import shopping.international.api.req.orders.OrderRefundRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.orders.*;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.AddressSnapshot;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户侧订单接口 {@code /users/me/orders}
 *
 * <p>职责:</p>
 * <ul>
 *     <li>下单试算</li>
 *     <li>创建订单 (预占库存)</li>
 *     <li>查询订单列表/详情</li>
 *     <li>取消订单、修改地址、申请退款</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/orders")
public class OrderController {

    /**
     * 用户侧订单领域服务
     */
    private final IOrderService orderService;
    /**
     * 货币配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 下单试算 (价格/库存/折扣/运费)
     *
     * @param req 请求体
     * @return 预览结果
     */
    @PostMapping("/preview")
    public ResponseEntity<Result<OrderPreviewRespond>> preview(@RequestBody OrderCreateRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        List<IOrderService.ItemInput> items = req.getItems() == null
                ? null
                : req.getItems().stream()
                .map(it -> new IOrderService.ItemInput(it.getSkuId(), it.getQuantity()))
                .toList();

        IOrderService.PreviewComputation preview = orderService.preview(
                userId,
                req.getSource(),
                items,
                req.getAddressId(),
                req.getCurrency(),
                req.getDiscountCode(),
                req.getBuyerRemark(),
                req.getLocale()
        );
        CurrencyConfig currencyConfig = currencyConfigService.get(preview.currency());
        if (req.getDiscountCode() != null && !preview.usedDiscount())
            return ResponseEntity.ok(Result.ok(toRespond(preview, currencyConfig), preview.discountFailureReason()));
        return ResponseEntity.ok(Result.ok(toRespond(preview, currencyConfig)));
    }

    /**
     * 列出我的订单 (分页)
     *
     * @param page        页码
     * @param size        每页大小
     * @param status      状态过滤 (可为空)
     * @param createdFrom 创建时间起 (可为空)
     * @param createdTo   创建时间止 (可为空)
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<Result<List<OrderSummaryRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                  @RequestParam(value = "created_to", required = false) String createdTo) {
        Long userId = requireCurrentUserId();
        int safeSize = Math.min(Math.max(size, 1), 200);
        OrderStatus statusEnum = parseEnum(status, OrderStatus.class);
        LocalDateTime from = parseDateTime(createdFrom);
        LocalDateTime to = parseDateTime(createdTo);

        PageQuery pageQuery = PageQuery.of(page, safeSize, 200);
        PageResult<IOrderService.OrderSummaryRow> pageData = orderService.listMyOrders(userId, pageQuery, statusEnum, from, to);
        List<OrderSummaryRespond> data = pageData.items().stream().map(row -> OrderSummaryRespond.builder()
                .orderNo(row.orderNo())
                .status(row.status())
                .itemsCount(row.itemsCount())
                .totalAmount(currencyConfigService.get(row.currency()).toMajor(row.totalAmountMinor()).toPlainString())
                .discountAmount(currencyConfigService.get(row.currency()).toMajor(row.discountAmountMinor()).toPlainString())
                .shippingAmount(currencyConfigService.get(row.currency()).toMajor(row.shippingAmountMinor()).toPlainString())
                .payAmount(currencyConfigService.get(row.currency()).toMajor(row.payAmountMinor()).toPlainString())
                .currency(row.currency())
                .payChannel(row.payChannel())
                .payStatus(row.payStatus())
                .payTime(row.payTime())
                .createdAt(row.createdAt())
                .build()).toList();

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
     * 创建订单 (预占库存)
     *
     * @param req 请求体
     * @return 已创建订单
     */
    @PostMapping
    public ResponseEntity<Result<OrderDetailRespond>> create(@RequestBody OrderCreateRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        List<IOrderService.ItemInput> items = req.getItems() == null
                ? null
                : req.getItems().stream()
                .map(it -> new IOrderService.ItemInput(it.getSkuId(), it.getQuantity()))
                .toList();

        Order created = orderService.create(
                userId,
                req.getSource(),
                items,
                req.getAddressId(),
                req.getCurrency(),
                req.getDiscountCode(),
                req.getBuyerRemark(),
                req.getLocale()
        );
        CurrencyConfig currencyConfig = currencyConfigService.get(created.getCurrency());
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(toRespond(created, currencyConfig)));
    }

    /**
     * 获取我的订单详情
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/{order_no}")
    public ResponseEntity<Result<OrderDetailRespond>> detail(@PathVariable("order_no") String orderNo) {
        Long userId = requireCurrentUserId();
        Optional<Order> order = orderService.getMyOrder(userId, OrderNo.of(orderNo));
        return order.map(value -> ResponseEntity.ok(Result.ok(toRespond(value, currencyConfigService.get(value.getCurrency())))))
                .orElseGet(() -> ResponseEntity
                        .status(ApiCode.NOT_FOUND.toHttpStatus())
                        .body(Result.error(ApiCode.NOT_FOUND, "订单不存在"))
                );
    }

    /**
     * 取消订单 (用户侧)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 取消后的订单
     */
    @PostMapping("/{order_no}/cancel")
    public ResponseEntity<Result<OrderDetailRespond>> cancel(@PathVariable("order_no") String orderNo,
                                                             @RequestBody OrderCancelRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        Order cancelled = orderService.cancel(userId, OrderNo.of(orderNo), req.getReason());
        return ResponseEntity.ok(Result.ok(toRespond(cancelled, currencyConfigService.get(cancelled.getCurrency()))));
    }

    /**
     * 修改订单地址 (仅一次)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 修改后的订单
     */
    @PostMapping("/{order_no}/change-address")
    public ResponseEntity<Result<OrderDetailRespond>> changeAddress(@PathVariable("order_no") String orderNo,
                                                                    @RequestBody OrderChangeAddressRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        Order updated = orderService.changeAddress(userId, OrderNo.of(orderNo), req.getAddressId(), req.getNote());
        return ResponseEntity.ok(Result.ok(toRespond(updated, currencyConfigService.get(updated.getCurrency()))));
    }

    /**
     * 申请订单退款 (置为 REFUNDING)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 已接受 (202)
     */
    @PostMapping("/{order_no}/refund-request")
    public ResponseEntity<Result<OrderDetailRespond>> refundRequest(@PathVariable("order_no") String orderNo,
                                                                    @RequestBody OrderRefundRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        Order updated = orderService.requestRefund(
                userId,
                OrderNo.of(orderNo),
                req.getReasonCode(),
                req.getReasonText(),
                req.getAttachments()
        );
        CurrencyConfig currencyConfig = currencyConfigService.get(updated.getCurrency());
        return ResponseEntity.status(ApiCode.ACCEPTED.toHttpStatus())
                .body(Result.of(true, ApiCode.ACCEPTED, "已接受", toRespond(updated, currencyConfig), null));
    }

    /**
     * PreviewComputation → OrderPreviewRespond
     *
     * @param preview 预览结果
     * @return 响应
     */
    private static OrderPreviewRespond toRespond(IOrderService.PreviewComputation preview, CurrencyConfig currencyConfig) {
        List<OrderItemRespond> items = preview.items()
                .stream()
                .map(it -> toRespond(it, currencyConfig))
                .toList();
        List<DiscountAppliedViewRespond> breakdown = preview.discountApplied().stream()
                .map(row -> DiscountAppliedViewRespond.builder()
                        .discountCodeId(row.discountCodeId())
                        .orderItemId(null)
                        .appliedScope(row.appliedScope())
                        .appliedAmount(currencyConfig.toMajor(row.appliedAmountMinor()).toPlainString())
                        .createdAt(null)
                        .build())
                .toList();
        return OrderPreviewRespond.builder()
                .items(items)
                .itemsCount(
                        items.stream()
                                .map(OrderItemRespond::getQuantity)
                                .mapToInt(Integer::intValue)
                                .sum()
                )
                .totalAmount(preview.totalAmount().toMajorString(currencyConfig))
                .discountAmount(preview.discountAmount().toMajorString(currencyConfig))
                .shippingAmount(preview.shippingAmount().toMajorString(currencyConfig))
                .payAmount(preview.payAmount().toMajorString(currencyConfig))
                .currency(preview.currency())
                .discountBreakdown(breakdown)
                .build();
    }

    /**
     * Order → OrderDetailRespond
     *
     * @param order 订单聚合
     * @return 响应
     */
    private static OrderDetailRespond toRespond(Order order, CurrencyConfig currencyConfig) {
        return OrderDetailRespond.builder()
                .orderNo(order.getOrderNo().getValue())
                .status(order.getStatus())
                .itemsCount(order.getItemsCount())
                .totalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().toMajorString(currencyConfig))
                .discountAmount(order.getDiscountAmount() == null ? null : order.getDiscountAmount().toMajorString(currencyConfig))
                .shippingAmount(order.getShippingAmount() == null ? null : order.getShippingAmount().toMajorString(currencyConfig))
                .payAmount(order.getPayAmount() == null ? null : order.getPayAmount().toMajorString(currencyConfig))
                .currency(order.getCurrency())
                .payChannel(order.getPayChannel())
                .payStatus(order.getPayStatus())
                .paymentExternalId(order.getPaymentExternalId())
                .payTime(order.getPayTime())
                .addressSnapshot(toRespond(order.getAddressSnapshot()))
                .buyerRemark(order.getBuyerRemark() == null ? null : order.getBuyerRemark().getValue())
                .cancelReason(order.getCancelReason() == null ? null : order.getCancelReason().getValue())
                .cancelTime(order.getCancelTime())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(it -> toRespond(it, currencyConfig)).toList())
                .addressChanged(order.isAddressChanged())
                .build();
    }

    /**
     * AddressSnapshot → AddressSnapshotRespond
     *
     * @param snapshot 地址快照
     * @return 响应
     */
    private static AddressSnapshotRespond toRespond(@Nullable AddressSnapshot snapshot) {
        if (snapshot == null)
            return null;
        return AddressSnapshotRespond.builder()
                .receiverName(snapshot.getReceiverName())
                .phone(snapshot.getPhone())
                .country(snapshot.getCountry())
                .province(snapshot.getProvince())
                .city(snapshot.getCity())
                .district(snapshot.getDistrict())
                .addressLine1(snapshot.getAddressLine1())
                .addressLine2(snapshot.getAddressLine2())
                .zipcode(snapshot.getZipcode())
                .build();
    }

    /**
     * OrderItem → OrderItemRespond
     *
     * @param item 明细实体
     * @return 响应
     */
    private static OrderItemRespond toRespond(OrderItem item, CurrencyConfig currencyConfig) {
        Map<String, Object> skuAttrs = item.getSkuAttrs();
        if (skuAttrs != null && !skuAttrs.isEmpty() && skuAttrs.containsKey("list_price") && skuAttrs.get("list_price") instanceof Number) {
            long listPrice = ((Number) skuAttrs.get("list_price")).longValue();
            skuAttrs.put("list_price", currencyConfig.toMajor(listPrice));
        }
        if (skuAttrs != null && !skuAttrs.isEmpty() && skuAttrs.containsKey("sale_price") && skuAttrs.get("sale_price") instanceof Number) {
            long salePrice = ((Number) skuAttrs.get("sale_price")).longValue();
            skuAttrs.put("sale_price", currencyConfig.toMajor(salePrice));
        }
        return OrderItemRespond.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .skuId(item.getSkuId())
                .discountCodeId(item.getDiscountCodeId())
                .title(item.getTitle())
                .skuAttrs(skuAttrs)
                .coverImageUrl(item.getCoverImageUrl())
                .unitPrice(item.getUnitPrice() == null ? null : item.getUnitPrice().toMajorString(currencyConfig))
                .quantity(item.getQuantity())
                .subtotalAmount(item.getSubtotalAmount() == null ? null : item.getSubtotalAmount().toMajorString(currencyConfig))
                .build();
    }

    /**
     * 解析 ISO-8601 date-time 到 {@link LocalDateTime}
     *
     * @param value 字符串 (可为空)
     * @return LocalDateTime 或 null
     */
    private static @Nullable LocalDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        String trimmed = value.strip();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ex) {
                throw new IllegalParamException("时间格式不合法: " + value);
            }
        }
    }

    /**
     * 解析枚举 (字符串为空返回 null)
     *
     * @param value    字符串
     * @param enumType 枚举类型
     * @param <E>      枚举泛型
     * @return 枚举或 null
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

    /**
     * 从安全上下文中解析当前用户 ID
     *
     * @return 当前用户 ID
     * @throws AccountException 未登录或无法解析
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}

package shopping.international.trigger.controller.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.orders.AdminConfirmRefundRequest;
import shopping.international.api.req.orders.OrderCancelRequest;
import shopping.international.api.req.orders.OrderCloseRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.orders.*;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.InventoryChangeType;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.PayChannel;
import shopping.international.domain.model.enums.orders.PayStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.AddressSnapshot;
import shopping.international.domain.model.vo.orders.AdminOrderSearchCriteria;
import shopping.international.domain.model.vo.orders.InventoryLogSearchCriteria;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 管理侧订单接口 {@code /admin/orders}
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin")
public class AdminOrderController {

    /**
     * 管理侧订单领域服务
     */
    private final IAdminOrderService adminOrderService;

    /**
     * 搜索/筛选订单 (管理侧)
     *
     * @param page              页码
     * @param size              每页大小
     * @param orderNo           订单号 (可为空)
     * @param userId            用户 ID (可为空)
     * @param status            订单状态 (可为空)
     * @param payStatus         支付状态 (可为空)
     * @param payChannel        支付通道 (可为空)
     * @param paymentExternalId 支付外部单号 (可为空)
     * @param createdFrom       创建时间起 (可为空)
     * @param createdTo         创建时间止 (可为空)
     * @return 分页结果
     */
    @GetMapping("/orders")
    public ResponseEntity<Result<List<AdminOrderDetailRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(value = "order_no", required = false) String orderNo,
                                                                      @RequestParam(value = "user_id", required = false) Long userId,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(value = "pay_status", required = false) String payStatus,
                                                                      @RequestParam(value = "pay_channel", required = false) String payChannel,
                                                                      @RequestParam(value = "payment_external_id", required = false) String paymentExternalId,
                                                                      @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                      @RequestParam(value = "created_to", required = false) String createdTo) {
        PageQuery pageQuery = PageQuery.of(page, size, 500);
        AdminOrderSearchCriteria criteria = AdminOrderSearchCriteria.builder()
                .orderNo(orderNo)
                .userId(userId)
                .status(parseEnum(status, OrderStatus.class))
                .payStatus(parseEnum(payStatus, PayStatus.class))
                .payChannel(parseEnum(payChannel, PayChannel.class))
                .paymentExternalId(paymentExternalId)
                .createdFrom(parseDateTime(createdFrom))
                .createdTo(parseDateTime(createdTo))
                .build();
        criteria.validate();
        PageResult<IAdminOrderService.AdminOrderListItemView> pageData = adminOrderService.list(criteria, pageQuery);
        List<AdminOrderDetailRespond> data = pageData.items().stream().map(AdminOrderController::toRespond).toList();
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
     * 获取订单详情 (管理侧)
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/orders/{order_no}")
    public ResponseEntity<Result<AdminOrderDetailRespond>> detail(@PathVariable("order_no") String orderNo) {
        Optional<Order> order = adminOrderService.getDetail(OrderNo.of(orderNo));
        return order.map(value -> ResponseEntity.ok(Result.ok(toRespond(value))))
                .orElseGet(() -> ResponseEntity
                        .status(ApiCode.NOT_FOUND.toHttpStatus())
                        .body(Result.error(ApiCode.NOT_FOUND, "订单不存在"))
                );
    }

    /**
     * 查询订单状态流转日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @GetMapping("/orders/{order_no}/status-logs")
    public ResponseEntity<Result<List<OrderStatusLogRespond>>> statusLogs(@PathVariable("order_no") String orderNo) {
        List<OrderStatusLog> logs = adminOrderService.listStatusLogs(OrderNo.of(orderNo));
        List<OrderStatusLogRespond> data = logs.stream().map(row -> OrderStatusLogRespond.builder()
                .id(row.getId())
                .orderId(row.getOrderId())
                .eventSource(row.getEventSource())
                .fromStatus(row.getFromStatus())
                .toStatus(row.getToStatus())
                .note(row.getNote())
                .createdAt(row.getCreatedAt())
                .build()).toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 查询订单关联的库存变动日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @GetMapping("/orders/{order_no}/inventory-logs")
    public ResponseEntity<Result<List<InventoryLogRespond>>> inventoryLogs(@PathVariable("order_no") String orderNo) {
        List<InventoryLog> logs = adminOrderService.listInventoryLogs(OrderNo.of(orderNo));
        List<InventoryLogRespond> data = logs.stream().map(row -> InventoryLogRespond.builder()
                .id(row.getId())
                .skuId(row.getSkuId())
                .orderId(row.getOrderId())
                .changeType(row.getChangeType())
                .quantity(row.getQuantity())
                .reason(row.getReason())
                .createdAt(row.getCreatedAt())
                .build()).toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 分页查询库存变动日志
     *
     * @param page        页码
     * @param size        每页大小
     * @param changeType  变更类型 (可为空)
     * @param skuId       SKU ID (可为空)
     * @param orderId     订单 ID (可为空)
     * @param quantityMin 数量下限 (可为空)
     * @param quantityMax 数量上限 (可为空)
     * @param createdFrom 时间起 (可为空)
     * @param createdTo   时间止 (可为空)
     * @return 分页结果
     */
    @GetMapping("/inventory/logs")
    public ResponseEntity<Result<List<InventoryLogRespond>>> pageInventoryLogs(@RequestParam(defaultValue = "1") int page,
                                                                               @RequestParam(defaultValue = "20") int size,
                                                                               @RequestParam(value = "change_type", required = false) String changeType,
                                                                               @RequestParam(value = "sku_id", required = false) Long skuId,
                                                                               @RequestParam(value = "order_id", required = false) Long orderId,
                                                                               @RequestParam(value = "quantity_min", required = false) Integer quantityMin,
                                                                               @RequestParam(value = "quantity_max", required = false) Integer quantityMax,
                                                                               @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                               @RequestParam(value = "created_to", required = false) String createdTo) {
        PageQuery pageQuery = PageQuery.of(page, size, 500);
        InventoryLogSearchCriteria criteria = InventoryLogSearchCriteria.builder()
                .changeType(parseEnum(changeType, InventoryChangeType.class))
                .skuId(skuId)
                .orderId(orderId)
                .quantityMin(quantityMin)
                .quantityMax(quantityMax)
                .createdFrom(parseDateTime(createdFrom))
                .createdTo(parseDateTime(createdTo))
                .build();
        criteria.validate();
        PageResult<InventoryLog> pageData = adminOrderService.pageInventoryLogs(criteria, pageQuery);
        List<InventoryLogRespond> data = pageData.items().stream().map(row -> InventoryLogRespond.builder()
                .id(row.getId())
                .skuId(row.getSkuId())
                .orderId(row.getOrderId())
                .changeType(row.getChangeType())
                .quantity(row.getQuantity())
                .reason(row.getReason())
                .createdAt(row.getCreatedAt())
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
     * 取消订单 (管理侧)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 取消后的订单详情
     */
    @PostMapping("/orders/{order_no}/cancel")
    public ResponseEntity<Result<AdminOrderDetailRespond>> cancel(@PathVariable("order_no") String orderNo,
                                                                  @RequestBody OrderCancelRequest req) {
        req.validate();
        Order cancelled = adminOrderService.cancel(OrderNo.of(orderNo), req.getReason());
        return ResponseEntity.ok(Result.ok(toRespond(cancelled)));
    }

    /**
     * 关闭订单 (管理侧)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 关闭后的订单详情
     */
    @PostMapping("/orders/{order_no}/close")
    public ResponseEntity<Result<AdminOrderDetailRespond>> close(@PathVariable("order_no") String orderNo,
                                                                 @RequestBody OrderCloseRequest req) {
        req.validate();
        Order closed = adminOrderService.close(OrderNo.of(orderNo), req.getReason());
        return ResponseEntity.ok(Result.ok(toRespond(closed)));
    }

    /**
     * 确认退款 (管理侧)
     *
     * @param orderNo 订单号
     * @param req     请求体
     * @return 订单详情
     */
    @PostMapping("/orders/{order_no}/refund/confirm")
    public ResponseEntity<Result<AdminOrderDetailRespond>> confirmRefund(@PathVariable("order_no") String orderNo,
                                                                         @RequestBody AdminConfirmRefundRequest req) {
        req.validate();
        Order refunded = adminOrderService.confirmRefund(OrderNo.of(orderNo), req.getNote());
        return ResponseEntity.ok(Result.ok(toRespond(refunded)));
    }

    /**
     * AdminOrderListItemView → AdminOrderDetailRespond (列表行映射)
     *
     * @param row 列表行
     * @return 响应
     */
    private static AdminOrderDetailRespond toRespond(IAdminOrderService.AdminOrderListItemView row) {
        return AdminOrderDetailRespond.builder()
                .id(row.id())
                .userId(row.userId())
                .orderNo(row.orderNo())
                .status(row.status())
                .itemsCount(row.itemsCount())
                .totalAmount(row.totalAmount())
                .discountAmount(row.discountAmount())
                .shippingAmount(row.shippingAmount())
                .payAmount(row.payAmount())
                .currency(row.currency())
                .payChannel(row.payChannel())
                .payStatus(row.payStatus())
                .paymentExternalId(row.paymentExternalId())
                .payTime(row.payTime())
                .createdAt(row.createdAt())
                .updatedAt(row.updatedAt())
                .build();
    }

    /**
     * Order → AdminOrderDetailRespond (详情映射)
     *
     * @param order 订单聚合
     * @return 响应
     */
    private static AdminOrderDetailRespond toRespond(Order order) {
        return AdminOrderDetailRespond.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNo(order.getOrderNo().getValue())
                .status(order.getStatus())
                .itemsCount(order.getItemsCount())
                .totalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().getAmount().toPlainString())
                .discountAmount(order.getDiscountAmount() == null ? null : order.getDiscountAmount().getAmount().toPlainString())
                .shippingAmount(order.getShippingAmount() == null ? null : order.getShippingAmount().getAmount().toPlainString())
                .payAmount(order.getPayAmount() == null ? null : order.getPayAmount().getAmount().toPlainString())
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
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream().map(AdminOrderController::toRespond).toList())
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
     * @param item 明细
     * @return 响应
     */
    private static OrderItemRespond toRespond(OrderItem item) {
        return OrderItemRespond.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .skuId(item.getSkuId())
                .discountCodeId(item.getDiscountCodeId())
                .title(item.getTitle())
                .skuAttrs(item.getSkuAttrs())
                .coverImageUrl(item.getCoverImageUrl())
                .unitPrice(item.getUnitPrice() == null ? null : item.getUnitPrice().getAmount().toPlainString())
                .quantity(item.getQuantity())
                .subtotalAmount(item.getSubtotalAmount() == null ? null : item.getSubtotalAmount().getAmount().toPlainString())
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
}

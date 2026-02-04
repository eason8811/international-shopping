package shopping.international.infrastructure.adapter.repository.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.adapter.repository.payment.IAdminPaymentRepository;
import shopping.international.domain.adapter.repository.payment.IPaymentRepository;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.enums.payment.*;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.AddressSnapshot;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.domain.service.payment.impl.PaymentService;
import shopping.international.infrastructure.dao.orders.OrdersMapper;
import shopping.international.infrastructure.dao.orders.po.OrdersPO;
import shopping.international.infrastructure.dao.payment.PaymentOrderMapper;
import shopping.international.infrastructure.dao.payment.PaymentRefundMapper;
import shopping.international.infrastructure.dao.payment.po.PaymentOrderPO;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundPO;
import shopping.international.infrastructure.dao.user.UserAccountMapper;
import shopping.international.infrastructure.dao.user.po.UserAccountPO;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.NotFoundException;
import shopping.international.types.exceptions.PayPalException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 基于 MyBatis-Plus 的支付领域仓储实现
 *
 * <ul>
 *     <li>该仓储在基础设施层实现跨领域一致性: 同步 {@code orders.pay_channel / orders.pay_status / orders.payment_external_id}</li>
 *     <li>所有状态变更尽量使用 CAS (按旧状态更新) 以保证并发安全</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepository implements IPaymentRepository, IAdminPaymentRepository {

    /**
     * orders Mapper (用于锁单与冗余字段同步)
     */
    private final OrdersMapper ordersMapper;
    /**
     * payment_order Mapper
     */
    private final PaymentOrderMapper paymentOrderMapper;
    /**
     * payment_refund Mapper
     */
    private final PaymentRefundMapper paymentRefundMapper;
    /**
     * JSON 序列化/反序列化器 (用于持久化 notifyPayload 等)
     */
    private final ObjectMapper objectMapper;
    /**
     * user_account Mapper
     */
    private final UserAccountMapper userAccountMapper;
    /**
     * @see IOrderRepository
     */
    private final IOrderRepository orderRepository;

    /**
     * 在同库事务内准备 PayPal Checkout (锁定订单行, 关闭旧待支付支付单, 创建/复用 PAYPAL 支付单, 同步 orders 冗余字段)
     *
     * @param userId  用户 ID
     * @param orderNo 订单号 (orders.order_no)
     * @return 事务内准备结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull PayPalCheckoutResult preparePayPalCheckout(@NotNull Long userId, @NotNull String orderNo) {
        UserAccountPO userAccountPO = userAccountMapper.selectById(userId);
        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getOrderNo, orderNo)
                .eq(OrdersPO::getUserId, userId)
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        if (!"CREATED".equals(order.getStatus()) && !"PENDING_PAYMENT".equals(order.getStatus()))
            throw new ConflictException("订单状态不允许支付");
        if (PaymentStatus.SUCCESS.name().equals(order.getPayStatus()))
            throw new ConflictException("订单已支付成功");

        Long orderId = order.getId();

        // 并发协议: orders.active_payment_id 作为 "当前有效支付尝试" 的权威闸门
        // 仅 active_payment_id 指向的 payment_order 才允许推进 orders 的冗余支付字段与订单状态
        PaymentOrderPO paymentOrderPO = null;
        Long activePaymentId = order.getActivePaymentId();
        if (activePaymentId != null)
            paymentOrderPO = paymentOrderMapper.selectById(activePaymentId);

        // 1) 优先复用当前有效支付尝试 (PAYPAL 且仍处于 INIT/PENDING)
        boolean canReuse = paymentOrderPO != null
                && PaymentChannel.PAYPAL.name().equals(paymentOrderPO.getChannel())
                && (PaymentStatus.INIT.name().equals(paymentOrderPO.getStatus()) || PaymentStatus.PENDING.name().equals(paymentOrderPO.getStatus()));

        // 2) 若当前有效为占位支付单 (NONE/NONE), 则原地升级为 PAYPAL/INIT
        boolean canUpgradePlaceholder = paymentOrderPO != null
                && PaymentChannel.NONE.name().equals(paymentOrderPO.getChannel())
                && PaymentStatus.NONE.name().equals(paymentOrderPO.getStatus());

        if (paymentOrderPO != null && PaymentStatus.SUCCESS.name().equals(paymentOrderPO.getStatus()))
            throw new ConflictException("支付单已成功, 无法重复发起支付");

        if (!canReuse && canUpgradePlaceholder) {
            int upgraded = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentOrderPO.getId())
                    .eq(PaymentOrderPO::getChannel, PaymentChannel.NONE.name())
                    .eq(PaymentOrderPO::getStatus, PaymentStatus.NONE.name())
                    .set(PaymentOrderPO::getChannel, PaymentChannel.PAYPAL.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.INIT.name())
                    .set(PaymentOrderPO::getAmount, order.getPayAmount())
                    .set(PaymentOrderPO::getCurrency, order.getCurrency())
                    .set(PaymentOrderPO::getExternalId, null));
            if (upgraded <= 0)
                throw new ConflictException("占位支付单并发升级失败");
            paymentOrderPO = paymentOrderMapper.selectById(paymentOrderPO.getId());
        }

        // 3) 若不可复用, 则关闭旧的有效尝试(仅 INIT/PENDING)并创建新支付单作为有效尝试
        else if (!canReuse) {
            if (paymentOrderPO != null)
                paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                        .eq(PaymentOrderPO::getId, paymentOrderPO.getId())
                        .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                        .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));

            PaymentOrderPO created = PaymentOrderPO.builder()
                    .orderId(orderId)
                    .externalId(null)
                    .channel(PaymentChannel.PAYPAL.name())
                    .amount(order.getPayAmount())
                    .currency(order.getCurrency())
                    .status(PaymentStatus.INIT.name())
                    .build();
            paymentOrderMapper.insert(created);
            paymentOrderPO = created;
        }

        // 4) 关闭该订单下除 "当前有效尝试" 外的所有 INIT/PENDING 支付单 (切换渠道/重复发起的并发闸门)
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId)
                .ne(PaymentOrderPO::getId, paymentOrderPO.getId())
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));

        // 5) 同步 orders 冗余字段 + active_payment_id
        boolean hasExternalId = paymentOrderPO.getExternalId() != null && !paymentOrderPO.getExternalId().isBlank();
        PaymentStatus targetPayStatus = hasExternalId ? PaymentStatus.PENDING : PaymentStatus.INIT;

        ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, orderId)
                .set(OrdersPO::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                .set(OrdersPO::getPayStatus, targetPayStatus)
                .set(OrdersPO::getPaymentExternalId, paymentOrderPO.getExternalId())
                .set(OrdersPO::getActivePaymentId, paymentOrderPO.getId()));
        orderRepository.insertStatusLog(
                orderId,
                OrderStatusEventSource.USER,
                OrderStatus.valueOf(order.getStatus()),
                OrderStatus.PENDING_PAYMENT,
                "创建支付单"
        );

        // 6) 确保支付单状态与 externalId 一致 (不覆盖 SUCCESS)
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentOrderPO.getId())
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, targetPayStatus.name()));

        AddressSnapshot addressSnapshot;
        try {
            addressSnapshot = objectMapper.readValue(order.getAddressSnapshot(), AddressSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new PayPalException("地址快照反序列化异常", e);
        }
        LocalDateTime orderCreatedAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();
        return PayPalCheckoutResult.builder()
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .currency(order.getCurrency())
                .totalAmount(order.getPayAmount())
                .itemTotal(order.getTotalAmount())
                .shipping(order.getShippingAmount())
                .handling(0L)
                .taxTotal(order.getTaxAmount())
                .shippingDiscount(0L)
                .discount(order.getDiscountAmount())
                .fullName(addressSnapshot.getReceiverName())
                .emailAddress(userAccountPO.getEmail())
                .phoneCountryCode(userAccountPO.getPhoneCountryCode())
                .phoneNationalNumber(userAccountPO.getPhoneNationalNumber())
                .addressLine1(addressSnapshot.getDistrict() + " " + addressSnapshot.getAddressLine1())
                .addressLine2(addressSnapshot.getAddressLine2())
                .adminArea2(addressSnapshot.getCity())
                .adminArea1(addressSnapshot.getProvince())
                .postalCode(addressSnapshot.getZipcode())
                .countryCode(addressSnapshot.getCountry())
                .channel(PaymentChannel.PAYPAL)
                .paymentStatus(targetPayStatus)
                .orderCreatedAt(orderCreatedAt)
                .paymentId(paymentOrderPO.getId())
                .paypalOrderId(paymentOrderPO.getExternalId())
                .build();
    }

    /**
     * 回填 PayPal Order ID, 并将支付单推进为 PENDING, 同时同步 orders.payment_external_id / orders.pay_status
     *
     * @param paymentId       支付单 ID
     * @param externalId      PayPal Order ID
     * @param requestPayload  下单请求报文 (JSON 字符串)
     * @param responsePayload 下单响应报文 (JSON 字符串)
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ConflictException.class)
    public void bindPayPalOrder(@NotNull Long paymentId, @NotNull String externalId, @Nullable String requestPayload, @Nullable String responsePayload) {
        PaymentOrderPO po = paymentOrderMapper.selectById(paymentId);
        if (po == null)
            throw new NotFoundException("支付单不存在");

        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, po.getOrderId())
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        // 在 orders 行锁内重新读取 payment_order, 避免并发下拿到旧 external_id 导致错误覆盖 orders.payment_external_id
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");
        if (!order.getId().equals(payment.getOrderId()))
            throw new ConflictException("支付单与订单不匹配");

        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持回填 PAYPAL 支付单");

        String existingExternalId = payment.getExternalId();
        if (existingExternalId != null && !existingExternalId.isBlank() && !existingExternalId.equals(externalId))
            throw new ConflictException("支付单 externalId 不一致, 无法回填");

        // 兼容旧数据: 若 active_payment_id 为空但 orders.payment_external_id 已指向该 paypalOrderId, 则认定为当前有效尝试并补齐 active_payment_id
        if (order.getActivePaymentId() == null && externalId.equals(order.getPaymentExternalId())) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, order.getId())
                    .isNull(OrdersPO::getActivePaymentId)
                    .set(OrdersPO::getActivePaymentId, paymentId));
            order.setActivePaymentId(paymentId);
        }

        // 经过上面的归一化, isActiveAttempt 与条件 (active_payment_id == null && orders.payment_external_id == 当前 external_id) 同真同假
        boolean isActiveAttempt = order.getActivePaymentId() != null && order.getActivePaymentId().equals(paymentId);
        boolean orderPayable = ("CREATED".equals(order.getStatus()) || "PENDING_PAYMENT".equals(order.getStatus()))
                && !PaymentStatus.SUCCESS.name().equals(order.getPayStatus());

        boolean shouldCloseAttempt = !isActiveAttempt
                || !orderPayable
                || PaymentStatus.CLOSED.name().equals(payment.getStatus())
                || PaymentStatus.FAIL.name().equals(payment.getStatus())
                || PaymentStatus.EXCEPTION.name().equals(payment.getStatus());

        if (shouldCloseAttempt) {
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
            throw new ConflictException("支付尝试已失效或订单已不可支付, 回填被拒绝");
        }

        // 幂等回填 external_id 与 payload (external_id 已存在时不覆盖)
        if (existingExternalId == null || existingExternalId.isBlank())
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .and(w -> w
                            .isNull(PaymentOrderPO::getExternalId).or()
                            .eq(PaymentOrderPO::getExternalId, "")
                    )
                    .set(PaymentOrderPO::getExternalId, externalId)
                    .set(PaymentOrderPO::getRequestPayload, requestPayload)
                    .set(PaymentOrderPO::getResponsePayload, responsePayload));
        else
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .set(PaymentOrderPO::getRequestPayload, requestPayload)
                    .set(PaymentOrderPO::getResponsePayload, responsePayload));

        // 当前有效尝试: 推进 payment_order -> PENDING 与同步 orders 冗余字段
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.PENDING.name()));

        ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, order.getId())
                .ne(OrdersPO::getPayStatus, PaymentStatus.SUCCESS.name())
                .set(OrdersPO::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                .set(OrdersPO::getPayStatus, PaymentStatus.PENDING.name())
                .set(OrdersPO::getPaymentExternalId, externalId)
                .set(OrdersPO::getActivePaymentId, paymentId));
        orderRepository.insertStatusLog(
                order.getId(),
                OrderStatusEventSource.USER,
                OrderStatus.valueOf(order.getStatus()),
                OrderStatus.PENDING_PAYMENT,
                "回填支付单网关信息"
        );
    }

    /**
     * 用户侧取消本次支付尝试: payment_order.status INIT/PENDING -> CLOSED (CAS), 并同步 orders.pay_status -> CLOSED
     *
     * @param userId    用户 ID
     * @param paymentId 支付单 ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull PaymentResultView cancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId) {
        PaymentOrderPO po = paymentOrderMapper.selectById(paymentId);
        if (po == null)
            throw new NotFoundException("支付单不存在");

        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, po.getOrderId())
                .eq(OrdersPO::getUserId, userId)
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");

        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持取消 PAYPAL 支付单");

        // 幂等: 已关闭直接返回
        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.getStatus());
        if (PaymentStatus.CLOSED == currentStatus)
            return new PaymentResultView(paymentId, order.getOrderNo(), PaymentStatus.CLOSED, payment.getExternalId(), "已关闭 (幂等返回)");

        if (PaymentStatus.SUCCESS == currentStatus)
            throw new ConflictException("支付已成功, 无法取消本次支付尝试");

        int updated = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
        if (updated <= 0)
            throw new ConflictException("支付单状态已变更, 取消支付单失败");

        // 兼容旧数据: 若 active_payment_id 为空但 orders.payment_external_id 已指向该 paypalOrderId, 则认定为当前有效尝试并补齐 active_payment_id
        if (order.getActivePaymentId() == null && payment.getExternalId() != null
                && !payment.getExternalId().isBlank() && payment.getExternalId().equals(order.getPaymentExternalId())) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, order.getId())
                    .isNull(OrdersPO::getActivePaymentId)
                    .set(OrdersPO::getActivePaymentId, paymentId));
            order.setActivePaymentId(paymentId);
        }

        boolean isActiveAttempt = order.getActivePaymentId() != null && order.getActivePaymentId().equals(paymentId);

        // 仅当取消的是 "当前有效支付尝试" 时才同步 orders.pay_status, 避免影响已切换的其它尝试
        if (isActiveAttempt)
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, payment.getOrderId())
                    .ne(OrdersPO::getPayStatus, PaymentStatus.SUCCESS.name())
                    .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                    .set(OrdersPO::getPayStatus, PaymentStatus.CLOSED.name()));

        return new PaymentResultView(paymentId, order.getOrderNo(), PaymentStatus.CLOSED, payment.getExternalId(), "OK");
    }

    /**
     * 获取用于 capture 的目标信息 (不加锁)
     *
     * @param userId    用户 ID
     * @param paymentId 支付单 ID
     * @return capture 目标
     */
    @Override
    public @NotNull CaptureTarget getCaptureTarget(@NotNull Long userId, @NotNull Long paymentId) {
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");
        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, payment.getOrderId())
                .eq(OrdersPO::getUserId, userId)
                .last("limit 1"));
        if (PaymentStatus.CLOSED.name().equals(payment.getStatus()))
            throw new ConflictException("支付单已关闭, 无法 capture");

        CaptureTarget captureTarget = buildCaptureTarget(payment, order);
        boolean isActiveAttempt = order.getActivePaymentId() != null && order.getActivePaymentId().equals(paymentId);
        boolean legacyActiveAttempt = order.getActivePaymentId() == null && payment.getExternalId().equals(order.getPaymentExternalId());
        if (!isActiveAttempt && !legacyActiveAttempt)
            throw new ConflictException("当前支付单不是订单的有效支付尝试, 无法 capture");
        return captureTarget;
    }

    /**
     * 获取用于同步/回调处理的目标信息 (不做用户校验)
     *
     * @param paymentId 支付单 ID
     * @return capture 目标
     */
    @Override
    public @NotNull CaptureTarget getCaptureTargetForOps(@NotNull Long paymentId) {
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");
        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, payment.getOrderId())
                .last("limit 1"));
        return buildCaptureTarget(payment, order);
    }

    /**
     * 获取退款目标信息
     *
     * @param refundId 退款单 ID
     * @return 目标
     */
    @Override
    public RefundTarget getRefundTargetForOps(@NotNull Long refundId) {
        PaymentRefundPO refundPO = paymentRefundMapper.selectById(refundId);
        if (refundPO == null)
            throw new NotFoundException("退款单不存在");
        if (refundPO.getExternalRefundId() == null || refundPO.getExternalRefundId().isBlank())
            throw new ConflictException("退款单尚未生成外部退款单号, 无法退款");
        return new RefundTarget(
                refundId,
                refundPO.getOrderId(),
                refundPO.getExternalRefundId(),
                refundPO.getPaymentOrderId(),
                RefundStatus.valueOf(refundPO.getStatus())
        );
    }

    /**
     * 获取退款目标信息 (用于 WebHook)
     * <p>
     * 先使用 {@code external_refund_id} 查, 如果不命中则查 {@code payment_order_id} 对应的支付单下还在
     * 进行中 (状态为 {@code INIT/PENDING}, 且 {@code external_refund_id} 为空) 的退款单, 如果还不命中就根据 cmd 中的信息新建一个退款单
     * <p>
     * 幂等键使用统一 scheme: {@code ppref-{paymentOrderId}-{scope}-{key}}
     *
     * @param cmd 外部退款单 ID
     * @return 目标
     */
    @Override
    public RefundTarget getRefundTargetForWebhook(@NotNull PayPalRefundWebhookCommand cmd) {
        PaymentRefundPO refundPO = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getExternalRefundId, cmd.externalRefundId())
                .last("limit 1")
        );
        if (refundPO == null) {
            refundPO = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                    .eq(PaymentRefundPO::getPaymentOrderId, cmd.paymentOrderId())
                    .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name())
                    .and(q -> q
                            .isNull(PaymentRefundPO::getExternalRefundId).or()
                            .eq(PaymentRefundPO::getExternalRefundId, "")
                    )
                    .orderByDesc(PaymentRefundPO::getCreatedAt)
                    .last("limit 1"));
        }
        if (refundPO == null) {
            String clientRefundNo = PaymentService.RefundClientRefundNoUtils.webhook(cmd.paymentOrderId(), cmd.externalRefundId());
            PaymentRefundPO po = PaymentRefundPO.builder()
                    .refundNo(cmd.refundNo())
                    .orderId(cmd.orderId())
                    .paymentOrderId(cmd.paymentOrderId())
                    .externalRefundId(cmd.externalRefundId())
                    .clientRefundNo(clientRefundNo)
                    .amount(cmd.amountMinor())
                    .currency(cmd.currency())
                    .itemsAmount(null)
                    .shippingAmount(null)
                    .status(cmd.status().name())
                    .reasonCode(RefundReasonCode.OTHER.name())
                    .reasonText(null)
                    .initiator(RefundInitiator.SYSTEM.name())
                    .ticketId(null)
                    .requestPayload(null)
                    .responsePayload(null)
                    .notifyPayload(toJsonOrNull(cmd.webhookEvent()))
                    .lastPolledAt(null)
                    .lastNotifiedAt(cmd.notifiedAt())
                    .build();
            try {
                paymentRefundMapper.insert(po);
                return new RefundTarget(
                        po.getId(),
                        po.getOrderId(),
                        po.getExternalRefundId(),
                        po.getPaymentOrderId(),
                        RefundStatus.valueOf(po.getStatus())
                );
            } catch (DuplicateKeyException e) {
                // 兜底: 并发插入, 可能命中 uk_refund_external 或 uk_refund_req_dedupe
                PaymentRefundPO reread = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                        .eq(PaymentRefundPO::getExternalRefundId, cmd.externalRefundId())
                        .last("limit 1")
                );
                if (reread == null) {
                    reread = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                            .eq(PaymentRefundPO::getPaymentOrderId, cmd.paymentOrderId())
                            .eq(PaymentRefundPO::getClientRefundNo, clientRefundNo)
                            .last("limit 1"));
                }
                if (reread == null)
                    throw new ConflictException("同一幂等键的退款单已被插入, 但是回读失败, orderId: "
                            + cmd.orderId() + ", paymentOrderId: " + cmd.paymentOrderId()
                            + ", externalRefundId: " + cmd.externalRefundId(), e);
                return new RefundTarget(
                        reread.getId(),
                        reread.getOrderId(),
                        reread.getExternalRefundId() == null || reread.getExternalRefundId().isBlank()
                                ? cmd.externalRefundId()
                                : reread.getExternalRefundId(),
                        reread.getPaymentOrderId(),
                        RefundStatus.valueOf(reread.getStatus())
                );
            }
        }

        return new RefundTarget(
                refundPO.getId(),
                refundPO.getOrderId(),
                cmd.externalRefundId(),
                refundPO.getPaymentOrderId(),
                RefundStatus.valueOf(refundPO.getStatus())
        );
    }

    /**
     * 全局检查: 判断某个支付单下是否存在 "进行中/已成功" 的退款单
     *
     * <p>用于在调用网关发起退款前做硬保护: 只要存在 {@code INIT/PENDING/SUCCESS} 之一就不再发起新的退款调用</p>
     *
     * @param paymentOrderId 支付单 ID
     * @return true 表示已存在进行中/成功退款
     */
    @Override
    public boolean existsRefundInProgressOrSuccess(@NotNull Long paymentOrderId) {
        Long cnt = paymentRefundMapper.selectCount(new LambdaQueryWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getPaymentOrderId, paymentOrderId)
                .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name(), RefundStatus.SUCCESS.name())
                .last("limit 1"));
        return cnt != null && cnt > 0;
    }

    /**
     * 获取用于 capture 的目标信息, 包括支付单和订单的相关数据
     *
     * @param payment 支付单实体
     * @param order   订单实体
     * @return 返回一个包含捕获目标信息的 {@link CaptureTarget} 对象
     * @throws NotFoundException 如果订单不存在
     * @throws ConflictException 如果出现以下情况之一:
     *                           <ul>
     *                             <li>支付渠道不是 PAYPAL</li>
     *                             <li>支付单尚未生成外部单号</li>
     *                             <li>支付单已关闭</li>
     *                             <li>当前支付单不是订单的有效支付尝试</li>
     *                           </ul>
     */
    @NotNull
    private CaptureTarget buildCaptureTarget(PaymentOrderPO payment, OrdersPO order) {
        if (order == null)
            throw new NotFoundException("订单不存在");
        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持 PAYPAL 支付单 capture");
        if (payment.getExternalId() == null || payment.getExternalId().isBlank())
            throw new ConflictException("支付单尚未生成外部单号, 无法 capture");

        return new CaptureTarget(
                payment.getId(),
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt(),
                order.getCurrency(),
                order.getPayAmount() == null ? 0L : order.getPayAmount(),
                payment.getExternalId(),
                PaymentStatus.valueOf(payment.getStatus())
        );
    }

    /**
     * 在同库事务内应用 PayPal capture 结果 (更新 payment_order 与同步 orders 冗余字段)
     * <p>该方法应承载幂等与并发安全的 "权威落库逻辑" </p>
     *
     * @param cmd PayPal Capture 结果应用命令
     * @return 支付结果视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull PaymentResultView applyPayPalCaptureResult(@NotNull PayPalCaptureApplyCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");

        // 1) 锁单, 避免与取消/关单并发竞争造成 "成功支付后被覆盖"
        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, cmd.orderId())
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        LocalDateTime orderCreatedAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();

        PaymentOrderPO payment = paymentOrderMapper.selectById(cmd.paymentId());
        if (payment == null)
            throw new NotFoundException("支付单不存在");
        if (!cmd.orderId().equals(payment.getOrderId()))
            throw new IllegalParamException("支付单与订单不匹配");

        // 并发协议: 仅 orders.active_payment_id 指向的 payment_order 才允许写入 orders 冗余支付字段/订单状态
        // 兼容旧数据: 若 active_payment_id 为空但 orders.payment_external_id 已指向该 paypalOrderId, 则认定为有效尝试并补齐 active_payment_id
        if (order.getActivePaymentId() == null && cmd.paypalOrderId().equals(order.getPaymentExternalId())) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, order.getId())
                    .isNull(OrdersPO::getActivePaymentId)
                    .set(OrdersPO::getActivePaymentId, cmd.paymentId()));
            order.setActivePaymentId(cmd.paymentId());
        }

        // 经过上面的归一化, isActiveAttempt 与条件 (active_payment_id == null && orders.payment_external_id == 当前 external_id) 同真同假
        boolean isActiveAttempt = order.getActivePaymentId() != null && order.getActivePaymentId().equals(cmd.paymentId());
        boolean orderPayable = ("CREATED".equals(order.getStatus()) || "PENDING_PAYMENT".equals(order.getStatus()))
                && !PaymentStatus.SUCCESS.name().equals(order.getPayStatus());

        boolean orderClosedOrCancelled = "CANCELLED".equals(order.getStatus()) || "CLOSED".equals(order.getStatus());
        boolean attemptPayable = PaymentStatus.INIT.name().equals(payment.getStatus()) || PaymentStatus.PENDING.name().equals(payment.getStatus());
        boolean isLate = cmd.captureTime().isAfter(orderCreatedAt.plus(cmd.ttl()));

        // 权威判定: SUCCESS/EXCEPTION/FAIL + 是否推进订单为 PAID
        PaymentStatus effectivePaymentStatus;
        PaymentStatus effectiveOrderPayStatus;
        boolean canAdvanceOrderStatusToPaid = false;
        LocalDateTime payTime = null;

        if (!cmd.captureSuccess()) {
            effectivePaymentStatus = PaymentStatus.FAIL;
            effectiveOrderPayStatus = PaymentStatus.FAIL;
        } else {
            payTime = cmd.captureTime();
            boolean canAdvance = isActiveAttempt && orderPayable && attemptPayable && !orderClosedOrCancelled && !isLate;
            if (canAdvance) {
                effectivePaymentStatus = PaymentStatus.SUCCESS;
                effectiveOrderPayStatus = PaymentStatus.SUCCESS;
                canAdvanceOrderStatusToPaid = true;
            } else {
                effectivePaymentStatus = PaymentStatus.EXCEPTION;
                effectiveOrderPayStatus = PaymentStatus.EXCEPTION;
            }
        }

        // 若订单已退款, 忽略 capture 结果对状态的推进, 仅补充审计字段 (避免 CLOSED -> EXCEPTION 污染与触发无意义的自动退款)
        // 注意: 该分支仍需校验 externalId 一致性, 防止错单写入
        boolean captureIdNotBlank = cmd.captureId() != null && !cmd.captureId().isBlank();
        if (OrderStatus.REFUNDED.name().equals(order.getStatus())) {
            String existingExternalId = payment.getExternalId();
            if (existingExternalId != null && !existingExternalId.isBlank() && !existingExternalId.equals(cmd.paypalOrderId()))
                throw new ConflictException("支付单现有 externalId 与当前 capture 的 externalId 不一致, 回填失败");

            // capture_id / external_id 的补齐需要独立幂等条件, 避免互相叠加 WHERE 导致漏更新
            if (captureIdNotBlank) {
                paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                        .eq(PaymentOrderPO::getId, cmd.paymentId())
                        .and(w -> w
                                .isNull(PaymentOrderPO::getCaptureId).or()
                                .eq(PaymentOrderPO::getCaptureId, "")
                        )
                        .set(PaymentOrderPO::getCaptureId, cmd.captureId().strip()));
            }
            if (existingExternalId == null || existingExternalId.isBlank()) {
                paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                        .eq(PaymentOrderPO::getId, cmd.paymentId())
                        .and(w -> w
                                .isNull(PaymentOrderPO::getExternalId).or()
                                .eq(PaymentOrderPO::getExternalId, "")
                        )
                        .set(PaymentOrderPO::getExternalId, cmd.paypalOrderId()));
            }

            String notifyJson = toJsonOrNull(cmd.notifyPayload());
            boolean shouldUpdateAudit = false;
            LambdaUpdateWrapper<PaymentOrderPO> auditW = new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, cmd.paymentId());
            if (notifyJson != null && !notifyJson.isBlank() && !"{}".equals(notifyJson)) {
                auditW.set(PaymentOrderPO::getNotifyPayload, notifyJson);
                shouldUpdateAudit = true;
            }
            if (cmd.responsePayload() != null && !cmd.responsePayload().isBlank() && !"{}".equals(cmd.responsePayload())) {
                auditW.set(PaymentOrderPO::getResponsePayload, cmd.responsePayload());
                shouldUpdateAudit = true;
            }
            if (cmd.lastNotifiedAt() != null) {
                auditW.set(PaymentOrderPO::getLastNotifiedAt, cmd.lastNotifiedAt());
                shouldUpdateAudit = true;
            }
            if (shouldUpdateAudit)
                paymentOrderMapper.update(null, auditW);

            String paypalOrderId = (existingExternalId != null && !existingExternalId.isBlank()) ? existingExternalId : cmd.paypalOrderId();
            return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), PaymentStatus.valueOf(payment.getStatus()), paypalOrderId, "订单已退款 (忽略 capture 状态推进)");
        }

        // 幂等: SUCCESS 不允许被覆盖
        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.getStatus());
        if (PaymentStatus.SUCCESS == currentStatus)
            return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), PaymentStatus.SUCCESS, payment.getExternalId(), "已 SUCCESS (忽略更新)");

        String notifyJson = toJsonOrNull(cmd.notifyPayload());

        String existingExternalId = payment.getExternalId();
        if (existingExternalId != null && !existingExternalId.isBlank() && !existingExternalId.equals(cmd.paypalOrderId()))
            throw new ConflictException("支付单现有 externalId 与当前 capture 的 externalId 不一致, 回填失败");

        LambdaUpdateWrapper<PaymentOrderPO> paymentW = new LambdaUpdateWrapper<>();
        paymentW.eq(PaymentOrderPO::getId, cmd.paymentId())
                .ne(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name())
                .set(PaymentOrderPO::getStatus, effectivePaymentStatus.name())
                .set(PaymentOrderPO::getNotifyPayload, notifyJson)
                .set(PaymentOrderPO::getLastNotifiedAt, cmd.lastNotifiedAt());
        if (captureIdNotBlank)
            paymentW.set(PaymentOrderPO::getCaptureId, cmd.captureId().strip());
        if (existingExternalId == null || existingExternalId.isBlank())
            paymentW.and(w -> w
                            .isNull(PaymentOrderPO::getExternalId).or()
                            .eq(PaymentOrderPO::getExternalId, "")
                    )
                    .set(PaymentOrderPO::getExternalId, cmd.paypalOrderId());
        if (cmd.responsePayload() != null && !cmd.responsePayload().isBlank())
            paymentW.set(PaymentOrderPO::getResponsePayload, cmd.responsePayload());

        int updatedPayment = paymentOrderMapper.update(null, paymentW);
        if (updatedPayment <= 0)
            throw new ConflictException("支付单 capture 信息回填失败, 已经支付成功或已被并发回填");

        // 3) 仅对 "当前有效尝试" 同步 orders 冗余字段与订单状态, 避免旧尝试回调污染订单
        if (isActiveAttempt) {
            LambdaUpdateWrapper<OrdersPO> payW = new LambdaUpdateWrapper<>();
            payW.eq(OrdersPO::getId, cmd.orderId())
                    .ne(OrdersPO::getPayStatus, PaymentStatus.SUCCESS.name())
                    .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                    .set(OrdersPO::getPayStatus, effectiveOrderPayStatus.name())
                    .set(OrdersPO::getPaymentExternalId, cmd.paypalOrderId())
                    .set(OrdersPO::getActivePaymentId, cmd.paymentId());
            if (payTime != null)
                payW.set(OrdersPO::getPayTime, payTime);
            ordersMapper.update(null, payW);

            if (canAdvanceOrderStatusToPaid) {
                ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                        .eq(OrdersPO::getId, cmd.orderId())
                        .in(OrdersPO::getStatus, OrderStatus.CREATED.name(), OrderStatus.PENDING_PAYMENT.name())
                        .set(OrdersPO::getStatus, OrderStatus.PAID));
                orderRepository.insertStatusLog(
                        cmd.orderId(),
                        cmd.eventSource(),
                        OrderStatus.valueOf(order.getStatus()),
                        OrderStatus.PAID,
                        cmd.statusLogNote()
                );
            }
        }

        return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), effectivePaymentStatus, cmd.paypalOrderId(), "OK");
    }

    /**
     * 按 PayPal Order ID 查找本地支付单 ID (payment_order.external_id 唯一)
     *
     * @param paypalOrderId PayPal Order ID
     * @return 支付单 ID (可为空)
     */
    @Override
    public @NotNull Optional<Long> findPaymentIdByPayPalOrderId(@NotNull String paypalOrderId) {
        PaymentOrderPO po = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getExternalId, paypalOrderId)
                .last("limit 1"));
        return po == null ? Optional.empty() : Optional.ofNullable(po.getId());
    }

    /**
     * 扫描需要同步的支付单 (用于低频兜底任务)
     *
     * @param limit 最大数量
     * @return 候选列表
     */
    @Override
    public @NotNull List<SyncCandidate> listSyncCandidates(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<PaymentOrderPO> rows = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getChannel, PaymentChannel.PAYPAL.name())
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .isNotNull(PaymentOrderPO::getExternalId)
                .orderByAsc(PaymentOrderPO::getUpdatedAt)
                .last("limit " + safeLimit));
        if (rows == null || rows.isEmpty())
            return List.of();
        return rows.stream()
                .map(r -> new SyncCandidate(
                        r.getId(),
                        r.getExternalId(),
                        PaymentStatus.valueOf(r.getStatus())
                ))
                .toList();
    }

    /**
     * 扫描需要同步的退款单 (用于低频兜底任务)
     *
     * @param limit 最大数量
     * @return 候选列表
     */
    @Override
    public @NotNull List<RefundSyncCandidate> listRefundSyncCandidates(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<PaymentRefundPO> rows = paymentRefundMapper.selectList(new LambdaQueryWrapper<PaymentRefundPO>()
                .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name())
                .isNotNull(PaymentRefundPO::getExternalRefundId)
                .orderByAsc(PaymentRefundPO::getUpdatedAt)
                .last("limit " + safeLimit));
        if (rows == null || rows.isEmpty())
            return List.of();
        return rows.stream()
                .map(r -> new RefundSyncCandidate(
                        r.getId(),
                        r.getExternalRefundId(),
                        PaymentStatus.valueOf(r.getStatus())
                ))
                .toList();
    }

    /**
     * 记录轮询时间与轮询报文 (可为空)
     *
     * @param paymentId       支付单 ID
     * @param polledAt        轮询时间
     * @param responsePayload 轮询响应报文 (JSON, 可为空)
     * @param captureId       PayPal capture_id (可为空)
     */
    @Override
    public void markPolled(@NotNull Long paymentId,
                           @NotNull LocalDateTime polledAt,
                           @Nullable String responsePayload,
                           @Nullable String captureId) {
        LambdaUpdateWrapper<PaymentOrderPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PaymentOrderPO::getId, paymentId)
                .set(PaymentOrderPO::getLastPolledAt, polledAt);
        if (captureId != null && !captureId.isBlank())
            wrapper.eq(PaymentOrderPO::getId, paymentId)
                    .and(w -> w
                            .isNull(PaymentOrderPO::getCaptureId).or()
                            .eq(PaymentOrderPO::getCaptureId, "")
                    )
                    .set(PaymentOrderPO::getCaptureId, captureId.strip());
        if (responsePayload != null && !responsePayload.isBlank() && !"{}".equals(responsePayload))
            wrapper.set(PaymentOrderPO::getResponsePayload, responsePayload);
        paymentOrderMapper.update(null, wrapper);
    }

    /**
     * 记录轮询时间与轮询报文 (可为空)
     *
     * @param refundId        退款单 ID
     * @param polledAt        轮询时间
     * @param responsePayload 轮询响应报文 (JSON, 可为空)
     */
    @Override
    public void markRefundPolled(@NotNull Long refundId, LocalDateTime polledAt, @NotNull String responsePayload) {
        LambdaUpdateWrapper<PaymentRefundPO> wrapper = new LambdaUpdateWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getId, refundId)
                .set(PaymentRefundPO::getLastPolledAt, polledAt);
        if (!responsePayload.isBlank() && !"{}".equals(responsePayload))
            wrapper.set(PaymentRefundPO::getResponsePayload, responsePayload);
        paymentRefundMapper.update(null, wrapper);
    }

    /**
     * 运维/兜底: 关闭 PayPal 支付尝试 (不做用户校验)
     *
     * <p>用于 PayPal 侧 order.status=VOIDED 等场景: 将当前支付单推进为 CLOSED (CAS), 并在其为当前有效尝试时同步 orders.pay_status -> CLOSED</p>
     *
     * @param paymentId 支付单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePayPalPaymentForOps(@NotNull Long paymentId) {
        PaymentOrderPO po = paymentOrderMapper.selectById(paymentId);
        if (po == null)
            throw new NotFoundException("支付单不存在");

        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, po.getOrderId())
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");

        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持关闭 PAYPAL 支付单");

        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.getStatus());
        if (PaymentStatus.CLOSED == currentStatus || PaymentStatus.SUCCESS == currentStatus)
            return;
        if (PaymentStatus.INIT != currentStatus && PaymentStatus.PENDING != currentStatus)
            return;

        int updated = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
        if (updated <= 0) {
            PaymentOrderPO reRead = paymentOrderMapper.selectById(paymentId);
            if (reRead == null)
                throw new ConflictException("支付单已不存在");
            PaymentStatus reReadStatus = PaymentStatus.valueOf(reRead.getStatus());
            if (PaymentStatus.CLOSED == reReadStatus || PaymentStatus.SUCCESS == reReadStatus)
                return;
            throw new ConflictException("支付单状态已变更, 关闭支付单失败");
        }

        // 兼容旧数据: 若 active_payment_id 为空但 orders.payment_external_id 已指向该 paypalOrderId, 则认定为当前有效尝试并补齐 active_payment_id
        if (order.getActivePaymentId() == null && payment.getExternalId() != null
                && !payment.getExternalId().isBlank() && payment.getExternalId().equals(order.getPaymentExternalId())) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, order.getId())
                    .isNull(OrdersPO::getActivePaymentId)
                    .set(OrdersPO::getActivePaymentId, paymentId));
            order.setActivePaymentId(paymentId);
        }

        boolean isActiveAttempt = order.getActivePaymentId() != null && order.getActivePaymentId().equals(paymentId);

        // 仅当关闭的是 "当前有效支付尝试" 时才同步 orders.pay_status, 避免影响已切换的其它尝试
        if (isActiveAttempt)
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, payment.getOrderId())
                    .ne(OrdersPO::getPayStatus, PaymentStatus.SUCCESS.name())
                    .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                    .set(OrdersPO::getPayStatus, PaymentStatus.CLOSED.name()));
    }

    /**
     * 写入自动退款记录 (用于晚到支付等自动退款补偿)
     *
     * <p>该方法为 Payment 域的事实表落库, 不负责订单域库存处理</p>
     *
     * @param orderId          订单 ID
     * @param paymentOrderId   支付单 ID
     * @param refundNo         退款单号 (业务侧生成)
     * @param externalRefundId 网关退款单号 (可空)
     * @param amountMinor      退款金额 (最小货币单位)
     * @param currency         币种
     * @param status           退款状态
     * @param requestPayload   退款请求报文 (JSON, 可空)
     * @param responsePayload  退款响应报文 (JSON, 可空)
     * @return 退款单主键 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Long insertRefund(@NotNull Long orderId,
                                      @NotNull Long paymentOrderId,
                                      @NotNull String refundNo,
                                      @Nullable String externalRefundId,
                                      @Nullable String clientRefundNo,
                                      long amountMinor,
                                      @NotNull String currency,
                                      @NotNull RefundStatus status,
                                      @Nullable String requestPayload,
                                      @Nullable String responsePayload) {
        PaymentRefundPO po = PaymentRefundPO.builder()
                .refundNo(refundNo)
                .orderId(orderId)
                .paymentOrderId(paymentOrderId)
                .externalRefundId(externalRefundId)
                .clientRefundNo(clientRefundNo)
                .amount(amountMinor)
                .currency(currency)
                .itemsAmount(null)
                .shippingAmount(null)
                .status(status.name())
                .reasonCode(RefundReasonCode.EXCEPTION.name())
                .reasonText(null)
                .initiator(RefundInitiator.SYSTEM.name())
                .ticketId(null)
                .requestPayload(requestPayload)
                .responsePayload(responsePayload)
                .notifyPayload(null)
                .lastPolledAt(null)
                .lastNotifiedAt(null)
                .build();
        try {
            paymentRefundMapper.insert(po);
            return po.getId();
        } catch (DuplicateKeyException e) {
            if (clientRefundNo == null || clientRefundNo.isBlank())
                throw e;
            return findRefundIdByDedupeKey(paymentOrderId, clientRefundNo)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * 检查是否存在指定的退款去重键
     *
     * @param paymentOrderId 支付单 ID, 用于关联特定支付记录
     * @param clientRefundNo 商户侧或客户端提供的幂等键, 用于防止重复退款
     * @return 如果存在具有相同 <code>clientRefundNo</code> 的退款记录, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    @Override
    public boolean existsRefundDedupeKey(@NotNull Long paymentOrderId, @NotNull String clientRefundNo) {
        if (clientRefundNo.isBlank())
            return false;
        Long cnt = paymentRefundMapper.selectCount(new LambdaQueryWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getPaymentOrderId, paymentOrderId)
                .eq(PaymentRefundPO::getClientRefundNo, clientRefundNo)
                .last("limit 1"));
        return cnt != null && cnt > 0;
    }

    /**
     * 根据条件尝试将订单状态从退款中{@link OrderStatus#REFUNDING}更新为已退款{@link OrderStatus#REFUNDED}
     * 仅当订单当前处于 REFUNDING 状态时, 才会进行状态变更; 否则方法直接返回不做任何处理
     *
     * @param target             退款目标信息 {@link RefundTarget}
     * @param refundResult       查询 Refund 结果
     * @param notifyPayload      回调请求体
     * @param refundResultStatus 查询 Refund 结果的状态
     * @param eventSource        订单状态变更事件来源 {@link OrderStatusEventSource}
     * @param note               备注信息, 记录退款成功的额外说明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefundResult(@NotNull RefundTarget target, @NotNull IPayPalPort.GetRefundResult refundResult,
                                  @NotNull Map<String, Object> notifyPayload, @NotNull RefundStatus refundResultStatus,
                                  @NotNull OrderStatusEventSource eventSource, @NotNull String note) {
        orderRepository.applyRefundResult(
                target,
                refundResult,
                notifyPayload,
                refundResultStatus,
                eventSource,
                note
        );
    }

    /**
     * 根据支付订单 ID 和客户退款编号查找退款记录的 ID
     *
     * @param paymentOrderId 支付订单 ID, 不能为空
     * @param clientRefundNo 客户退款编号, 不能为空
     * @return 如果找到对应的退款记录, 则返回包含该退款记录 ID 的 Optional 对象; 否则返回一个空的 Optional
     */
    private Optional<Long> findRefundIdByDedupeKey(@NotNull Long paymentOrderId, @NotNull String clientRefundNo) {
        PaymentRefundPO row = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                .select(PaymentRefundPO::getId)
                .eq(PaymentRefundPO::getPaymentOrderId, paymentOrderId)
                .eq(PaymentRefundPO::getClientRefundNo, clientRefundNo)
                .last("limit 1"));
        return row == null ? Optional.empty() : Optional.ofNullable(row.getId());
    }

    // ========================= 管理侧读模型实现 =========================

    /**
     * 分页查询管理侧支付单列表
     *
     * @param criteria  查询条件 包含订单 ID, 订单号, 支付渠道等信息
     * @param pageQuery 分页参数 指定了请求的页码和每页的数据量
     * @return 分页结果 包含当前页的支付单列表数据以及满足条件的支付单总数
     */
    @Override
    public @NotNull PageResult<AdminPaymentListItemView> pagePayments(@NotNull AdminPaymentSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        List<PaymentOrderPO> rows = paymentOrderMapper.pageAdminPayments(
                criteria.getOrderNo(),
                criteria.getExternalId(),
                criteria.getChannel() == null ? null : criteria.getChannel().name(),
                criteria.getStatus() == null ? null : criteria.getStatus().name(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = paymentOrderMapper.countAdminPayments(
                criteria.getOrderNo(),
                criteria.getExternalId(),
                criteria.getChannel() == null ? null : criteria.getChannel().name(),
                criteria.getStatus() == null ? null : criteria.getStatus().name(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );
        if (rows == null || rows.isEmpty())
            return PageResult.<AdminPaymentListItemView>builder()
                    .items(List.of())
                    .total(total)
                    .build();
        List<AdminPaymentListItemView> items = rows.stream().map(r -> new AdminPaymentListItemView(
                r.getId(),
                r.getOrderId(),
                r.getOrderNo(),
                r.getExternalId(),
                PaymentChannel.valueOf(r.getChannel()),
                PaymentStatus.valueOf(r.getStatus()),
                r.getAmount() == null ? 0L : r.getAmount(),
                r.getCurrency(),
                r.getLastPolledAt(),
                r.getLastNotifiedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        )).toList();
        return PageResult.<AdminPaymentListItemView>builder()
                .items(items)
                .total(total)
                .build();
    }

    /**
     * 根据支付单 ID 获取管理侧支付单详情
     *
     * @param paymentId 支付单 ID 必须提供
     * @return 返回一个包含 {@link AdminPaymentDetail} 的 {@link Optional} 对象, 如果没有找到对应的支付单则返回空的 {@link Optional}
     */
    @Override
    public @NotNull Optional<AdminPaymentDetail> getPaymentDetail(@NotNull Long paymentId) {
        PaymentOrderPO r = paymentOrderMapper.selectDetail(paymentId);
        if (r == null)
            return Optional.empty();
        return Optional.of(new AdminPaymentDetail(
                r.getId(),
                r.getOrderId(),
                r.getOrderNo(),
                r.getExternalId(),
                PaymentChannel.valueOf(r.getChannel()),
                PaymentStatus.valueOf(r.getStatus()),
                r.getAmount() == null ? 0L : r.getAmount(),
                r.getCurrency(),
                r.getRequestPayload(),
                r.getResponsePayload(),
                r.getNotifyPayload(),
                r.getLastPolledAt(),
                r.getLastNotifiedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        ));
    }

    /**
     * 分页查询管理侧退款单列表
     *
     * @param criteria  查询条件 包含订单 ID, 订单号, 支付渠道等信息
     * @param pageQuery 分页参数 指定了请求的页码和每页的数据量
     * @return 分页结果 包含当前页的退款单列表数据以及满足条件的退款单总数
     */
    @Override
    public @NotNull PageResult<AdminRefundListItemView> pageRefunds(@NotNull AdminRefundSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        List<PaymentRefundPO> rows = paymentRefundMapper.pageAdminRefunds(
                criteria.getOrderNo(),
                criteria.getExternalId(),
                criteria.getChannel() == null ? null : criteria.getChannel().name(),
                criteria.getStatus() == null ? null : criteria.getStatus().name(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = paymentRefundMapper.countAdminRefunds(
                criteria.getOrderNo(),
                criteria.getExternalId(),
                criteria.getChannel() == null ? null : criteria.getChannel().name(),
                criteria.getStatus() == null ? null : criteria.getStatus().name(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );
        if (rows == null || rows.isEmpty())
            return PageResult.<AdminRefundListItemView>builder()
                    .items(List.of())
                    .total(total)
                    .build();
        List<AdminRefundListItemView> items = rows.stream().map(r -> new AdminRefundListItemView(
                r.getId(),
                r.getRefundNo(),
                r.getOrderId(),
                r.getOrderNo(),
                r.getPaymentOrderId(),
                r.getExternalRefundId(),
                PaymentChannel.valueOf(r.getChannel()),
                RefundStatus.valueOf(r.getStatus()),
                r.getAmount() == null ? 0L : r.getAmount(),
                r.getCurrency(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        )).toList();
        return PageResult.<AdminRefundListItemView>builder()
                .items(items)
                .total(total)
                .build();
    }

    /**
     * 根据退款单 ID 获取管理侧退款单详情
     *
     * @param refundId 退款单 ID 必须提供
     * @return 返回一个包含 {@link AdminRefundDetail} 的 {@link Optional} 对象, 如果没有找到对应的退款单则返回空的 {@link Optional}
     */
    @Override
    public @NotNull Optional<AdminRefundDetail> getRefundDetail(@NotNull Long refundId) {
        PaymentRefundPO r = paymentRefundMapper.selectDetailWithItems(refundId);
        if (r == null)
            return Optional.empty();
        return Optional.of(new AdminRefundDetail(
                r.getId(),
                r.getRefundNo(),
                r.getOrderId(),
                r.getOrderNo(),
                r.getPaymentOrderId(),
                r.getExternalRefundId(),
                PaymentChannel.valueOf(r.getChannel()),
                RefundStatus.valueOf(r.getStatus()),
                r.getAmount() == null ? 0L : r.getAmount(),
                r.getCurrency(),
                r.getRequestPayload(),
                r.getResponsePayload(),
                r.getNotifyPayload(),
                r.getLastPolledAt(),
                r.getLastNotifiedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        ));
    }

    /**
     * 将给定对象转换为 JSON 字符串, 如果转换失败或对象为空, 则返回 null.
     *
     * @param any 要转换的对象, 可以是 null
     * @return 对象的 JSON 表示形式, 如果转换失败或输入为 null, 则返回 null
     */
    private @Nullable String toJsonOrNull(@Nullable Object any) {
        if (any == null)
            return null;
        try {
            return objectMapper.writeValueAsString(any);
        } catch (Exception e) {
            return null;
        }
    }
}

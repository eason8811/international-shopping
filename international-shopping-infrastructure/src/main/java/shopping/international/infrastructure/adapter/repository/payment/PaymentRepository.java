package shopping.international.infrastructure.adapter.repository.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.payment.IAdminPaymentRepository;
import shopping.international.domain.adapter.repository.payment.IPaymentRepository;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.enums.payment.RefundInitiator;
import shopping.international.domain.model.enums.payment.RefundReasonCode;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.infrastructure.dao.orders.OrdersMapper;
import shopping.international.infrastructure.dao.orders.po.OrdersPO;
import shopping.international.infrastructure.dao.payment.PaymentOrderMapper;
import shopping.international.infrastructure.dao.payment.PaymentRefundMapper;
import shopping.international.infrastructure.dao.payment.po.PaymentOrderPO;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundPO;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
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
     * 在同库事务内准备 PayPal Checkout (锁定订单行, 关闭旧待支付支付单, 创建/复用 PAYPAL 支付单, 同步 orders 冗余字段)
     *
     * @param userId  用户 ID
     * @param orderNo 订单号 (orders.order_no)
     * @return 事务内准备结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull PayPalCheckoutResult preparePayPalCheckout(@NotNull Long userId, @NotNull String orderNo) {
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

        // 1) 关闭该订单下所有 INIT/PENDING 的支付单 (换渠道并发闸门) 
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));

        // 2) 创建/复用 PAYPAL 支付单: 优先复用已存在的 PAYPAL 行, 否则尝试升级占位 NONE/NONE, 仍无则插入新行
        PaymentOrderPO paymentOrderPO = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId)
                .eq(PaymentOrderPO::getChannel, PaymentChannel.PAYPAL.name())
                .last("limit 1"));

        if (paymentOrderPO != null) {
            if (PaymentStatus.SUCCESS.name().equals(paymentOrderPO.getStatus()))
                throw new ConflictException("支付单已成功, 无法重复发起同渠道支付");
        } else {
            PaymentOrderPO placeholder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getOrderId, orderId)
                    .eq(PaymentOrderPO::getChannel, PaymentChannel.NONE.name())
                    .eq(PaymentOrderPO::getStatus, PaymentStatus.NONE.name())
                    .last("limit 1"));
            if (placeholder != null) {
                int updated = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                        .eq(PaymentOrderPO::getId, placeholder.getId())
                        .eq(PaymentOrderPO::getChannel, PaymentChannel.NONE.name())
                        .eq(PaymentOrderPO::getStatus, PaymentStatus.NONE.name())
                        .set(PaymentOrderPO::getChannel, PaymentChannel.PAYPAL.name())
                        .set(PaymentOrderPO::getStatus, PaymentStatus.INIT.name())
                        .set(PaymentOrderPO::getAmount, order.getPayAmount())
                        .set(PaymentOrderPO::getCurrency, order.getCurrency())
                        .set(PaymentOrderPO::getExternalId, null));
                if (updated <= 0)
                    throw new ConflictException("占位支付单并发升级失败");
                paymentOrderPO = paymentOrderMapper.selectById(placeholder.getId());
            } else {
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
        }

        // 3) 同步 orders 冗余字段: 订单状态推进为 PENDING_PAYMENT (若尚未进入待支付) 
        String targetOrderStatus = "CREATED".equals(order.getStatus()) ? "PENDING_PAYMENT" : order.getStatus();
        String targetPayStatus = (paymentOrderPO.getExternalId() != null && !paymentOrderPO.getExternalId().isBlank())
                ? PaymentStatus.PENDING.name()
                : PaymentStatus.INIT.name();

        ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, orderId)
                .set(OrdersPO::getStatus, targetOrderStatus)
                .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                .set(OrdersPO::getPayStatus, targetPayStatus)
                .set(OrdersPO::getPaymentExternalId, paymentOrderPO.getExternalId()));

        // 4) 确保没有 externalId 的支付单状态为 INIT, 有 externalId 的支付单状态为 PENDING
        if (paymentOrderPO.getExternalId() != null && !paymentOrderPO.getExternalId().isBlank())
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentOrderPO.getId())
                    .ne(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.PENDING.name()));
        else
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentOrderPO.getId())
                    .ne(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.INIT.name()));

        LocalDateTime orderCreatedAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();
        return new PayPalCheckoutResult(
                paymentOrderPO.getId(),
                orderId,
                order.getOrderNo(),
                orderCreatedAt,
                order.getCurrency(),
                order.getPayAmount() == null ? 0L : order.getPayAmount(),
                PaymentChannel.PAYPAL,
                paymentOrderPO.getExternalId() == null ? PaymentStatus.INIT : PaymentStatus.PENDING,
                paymentOrderPO.getExternalId()
        );
    }

    /**
     * 回填 PayPal Order ID, 并将支付单推进为 PENDING, 同时同步 orders.payment_external_id / orders.pay_status
     *
     * @param paymentId       支付单 ID
     * @param paypalOrderId   PayPal Order ID
     * @param requestPayload  下单请求报文 (JSON 字符串)
     * @param responsePayload 下单响应报文 (JSON 字符串)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPayPalOrder(@NotNull Long paymentId, @NotNull String paypalOrderId, @Nullable String requestPayload, @Nullable String responsePayload) {
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");

        // 幂等: external_id 已存在时不覆盖 (避免并发重复创建网关订单导致覆盖)
        if (payment.getExternalId() == null || payment.getExternalId().isBlank()) {
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .and(w ->
                            w.isNull(PaymentOrderPO::getExternalId).or()
                                    .eq(PaymentOrderPO::getExternalId, "")
                    )
                    .set(PaymentOrderPO::getExternalId, paypalOrderId)
                    .set(PaymentOrderPO::getRequestPayload, requestPayload)
                    .set(PaymentOrderPO::getResponsePayload, responsePayload));
            // 并发下可能已经被回填, 无需报错
        } else
            // 仅补齐 payload
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .set(PaymentOrderPO::getRequestPayload, requestPayload)
                    .set(PaymentOrderPO::getResponsePayload, responsePayload));

        // 轻量并发控制: 仅在 "订单仍可支付" 的条件下推进 orders 冗余字段为 PENDING (CAS) 
        // 说明: 
        // 1) 若该订单在 prepare 之后被取消/关闭, 则这里的更新会因 status 条件不满足而失败
        // 2) 若用户取消的是 "本次支付尝试" (仅关单, 不取消订单), 则 orders.pay_status 会被推进为 CLOSED, 这里通过 pay_status=INIT 约束避免覆盖回 PENDING
        int synced = ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, payment.getOrderId())
                .in(OrdersPO::getStatus, "CREATED", "PENDING_PAYMENT")
                .eq(OrdersPO::getPayStatus, PaymentStatus.INIT.name())
                .set(OrdersPO::getPayChannel, PaymentChannel.PAYPAL.name())
                .set(OrdersPO::getPayStatus, PaymentStatus.PENDING.name())
                .set(OrdersPO::getPaymentExternalId, paypalOrderId));

        if (synced > 0) {
            // orders 推进成功, 则同步 payment_order -> PENDING (仅推进 INIT/PENDING, 不覆盖 FAIL/EXCEPTION/SUCCESS 等) 
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.PENDING.name()));
            return;
        }

        // orders 未推进成功: 
        // - 可能订单已取消/已关闭/已成功
        // - 也可能 orders 已是 PENDING (重复回填/并发重试) 
        OrdersPO current = ordersMapper.selectById(payment.getOrderId());
        if (current == null)
            throw new NotFoundException("订单不存在");

        boolean alreadyPending = ("CREATED".equals(current.getStatus()) || "PENDING_PAYMENT".equals(current.getStatus()))
                && PaymentStatus.PENDING.name().equals(current.getPayStatus())
                && paypalOrderId.equals(current.getPaymentExternalId());
        if (alreadyPending) {
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                    .eq(PaymentOrderPO::getId, paymentId)
                    .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                    .set(PaymentOrderPO::getStatus, PaymentStatus.PENDING.name()));
            return;
        }

        // 兜底: 若订单已不可支付, 则尝试关闭本次支付尝试 (仅关闭 INIT/PENDING, 避免覆盖其它语义状态) 
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
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
    public @NotNull PaymentResultView txCancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId) {
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentId);
        if (payment == null)
            throw new NotFoundException("支付单不存在");

        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, payment.getOrderId())
                .eq(OrdersPO::getUserId, userId)
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持取消 PAYPAL 支付单");

        // 幂等: 已关闭直接返回
        if (PaymentStatus.CLOSED.name().equals(payment.getStatus()))
            return new PaymentResultView(paymentId, order.getOrderNo(), PaymentStatus.CLOSED, payment.getExternalId(), "已关闭 (幂等返回) ");

        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus()))
            throw new ConflictException("支付已成功, 无法取消本次支付尝试");

        int updated = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
        if (updated <= 0)
            throw new ConflictException("支付单状态已变更, 无法取消");

        // 同步 orders.pay_status -> CLOSED (仅当未成功支付)
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
        OrdersPO order = ordersMapper.selectById(payment.getOrderId());
        if (order == null)
            throw new NotFoundException("订单不存在");
        if (!PaymentChannel.PAYPAL.name().equals(payment.getChannel()))
            throw new ConflictException("仅支持 PAYPAL 支付单");
        if (payment.getExternalId() == null || payment.getExternalId().isBlank())
            throw new ConflictException("支付单 externalId 为空");

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
     * 在同库事务内应用 capture 结果 (CAS 更新 payment_order 与同步 orders 冗余字段)
     *
     * <p>该方法应承载幂等与并发安全的 "权威落库逻辑" </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull PaymentResultView txApplyCaptureResult(@NotNull CaptureApplyCommand cmd) {
        requireNotNull(cmd, "cmd 不能为空");

        // 1) 锁单, 避免与取消/关单并发竞争造成 "成功支付后被覆盖" 
        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, cmd.orderId())
                .last("limit 1 for update"));
        if (order == null)
            throw new NotFoundException("订单不存在");

        PaymentOrderPO payment = paymentOrderMapper.selectById(cmd.paymentId());
        if (payment == null)
            throw new NotFoundException("支付单不存在");
        if (!cmd.orderId().equals(payment.getOrderId()))
            throw new IllegalParamException("支付单与订单不匹配");

        // 幂等: 已是 SUCCESS 直接返回
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus()) && cmd.newPaymentStatus() == PaymentStatus.SUCCESS) {
            return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), PaymentStatus.SUCCESS, payment.getExternalId(), "已 SUCCESS (幂等返回) ");
        }

        // 2) CAS 更新 payment_order.status
        PaymentStatus current = PaymentStatus.valueOf(payment.getStatus());
        if (current == PaymentStatus.SUCCESS) {
            // SUCCESS 不允许被覆盖
            return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), PaymentStatus.SUCCESS, payment.getExternalId(), "已 SUCCESS (忽略更新) ");
        }

        String notifyJson = toJsonOrNull(cmd.notifyPayload());

        int updatedPayment = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, cmd.paymentId())
                .eq(PaymentOrderPO::getStatus, current.name())
                .set(PaymentOrderPO::getStatus, cmd.newPaymentStatus().name())
                .set(PaymentOrderPO::getExternalId, cmd.paypalOrderId())
                .set(PaymentOrderPO::getResponsePayload, cmd.responsePayload())
                .set(PaymentOrderPO::getNotifyPayload, notifyJson)
                .set(PaymentOrderPO::getLastNotifiedAt, cmd.lastNotifiedAt()));
        if (updatedPayment <= 0)
            throw new ConflictException("支付单状态已变更, 落库失败");

        // 3) 同步 orders 冗余字段 (幂等: 不回退 SUCCESS) 
        LambdaUpdateWrapper<OrdersPO> ow = new LambdaUpdateWrapper<>();
        ow.eq(OrdersPO::getId, cmd.orderId())
                .set(OrdersPO::getPayChannel, cmd.channel().name())
                .set(OrdersPO::getPayStatus, cmd.newOrderPayStatus().name())
                .set(OrdersPO::getPaymentExternalId, cmd.paypalOrderId());
        if (cmd.payTime() != null)
            ow.set(OrdersPO::getPayTime, cmd.payTime());
        if (cmd.newOrderStatus() != null && !cmd.newOrderStatus().isBlank())
            ow.set(OrdersPO::getStatus, cmd.newOrderStatus());

        // 不覆盖已成功支付的订单支付状态
        ow.ne(OrdersPO::getPayStatus, PaymentStatus.SUCCESS.name());
        ordersMapper.update(null, ow);

        return new PaymentResultView(cmd.paymentId(), order.getOrderNo(), cmd.newPaymentStatus(), cmd.paypalOrderId(), "OK");
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
     * 记录轮询时间与轮询报文 (可为空)
     *
     * @param paymentId       支付单 ID
     * @param polledAt        轮询时间
     * @param responsePayload 轮询响应报文 (JSON, 可为空)
     */
    @Override
    public void markPolled(@NotNull Long paymentId, @NotNull LocalDateTime polledAt, @Nullable String responsePayload) {
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentId)
                .set(PaymentOrderPO::getLastPolledAt, polledAt)
                .set(PaymentOrderPO::getResponsePayload, responsePayload));
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
                .clientRefundNo(null)
                .amount(amountMinor)
                .currency(currency)
                .itemsAmount(null)
                .shippingAmount(null)
                .status(status.name())
                .reasonCode(RefundReasonCode.OTHER.name())
                .reasonText(null)
                .initiator(RefundInitiator.SYSTEM.name())
                .ticketId(null)
                .requestPayload(requestPayload)
                .responsePayload(responsePayload)
                .notifyPayload(null)
                .lastPolledAt(null)
                .lastNotifiedAt(null)
                .build();
        paymentRefundMapper.insert(po);
        return po.getId();
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

package shopping.international.domain.model.aggregate.orders;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 订单聚合根, 对应表 orders
 *
 * <p>聚合职责: 维护订单状态机、金额口径与下单快照</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class Order implements Verifiable {
    /**
     * 订单主键 ID
     */
    private final Long id;
    /**
     * 订单号
     */
    private final OrderNo orderNo;
    /**
     * 订单所属用户 ID
     */
    private final Long userId;

    /**
     * 订单状态
     *
     * @see OrderStatus
     */
    private OrderStatus status;
    /**
     * 订单中的商品总件数
     */
    private int itemsCount;
    /**
     * 订单总金额
     */
    private Money totalAmount;
    /**
     * 订单折扣金额
     */
    private Money discountAmount;
    /**
     * 订单运费金额
     */
    private Money shippingAmount;
    /**
     * 订单税费金额
     */
    private Money taxAmount;
    /**
     * 订单应付金额
     */
    private Money payAmount;
    /**
     * 订单结算货币
     */
    private final String currency;

    /**
     * 订单支付通道
     */
    private PayChannel payChannel;
    /**
     * 订单支付状态
     */
    private PayStatus payStatus;
    /**
     * 订单关联的支付单 外部 ID
     */
    private String paymentExternalId;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     *
     */
    private AddressSnapshot addressSnapshot;
    /**
     * 买家留言
     */
    private final BuyerRemark buyerRemark;

    /**
     * 订单取消原因
     */
    private CancelReason cancelReason;
    /**
     * 订单取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 仅一次改址的领域状态 (可由 Redis 标记投影/注入)
     */
    private boolean addressChanged;

    /**
     * 最近一次退款申请原因 (可选: 便于在应用层发起支付退款/工单编排)
     */
    private OrderRefundReason lastRefundReason;

    /**
     * 订单下的订单项列表
     */
    @NotNull
    private final List<OrderItem> items;

    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;

    /**
     * 创建订单对象
     *
     * @param id                订单唯一标识符
     * @param orderNo           订单号 {@link OrderNo}
     * @param userId            用户 ID
     * @param status            订单状态 {@link OrderStatus}
     * @param totalAmount       订单总额
     * @param discountAmount    折扣金额
     * @param shippingAmount    运费
     * @param payAmount         应付金额
     * @param currency          货币代码
     * @param payChannel        支付渠道 {@link PayChannel}
     * @param payStatus         支付状态 {@link PayStatus}
     * @param paymentExternalId 外部支付系统中的支付流水号
     * @param payTime           支付时间
     * @param addressSnapshot   地址快照信息 {@link AddressSnapshot}
     * @param buyerRemark       买家备注信息 {@link BuyerRemark}
     * @param cancelReason      取消原因 {@link CancelReason}
     * @param cancelTime        取消时间
     * @param addressChanged    地址是否变更
     * @param lastRefundReason  最后一次退款的原因 {@link OrderRefundReason}
     * @param items             订单项列表
     * @param createdAt         创建时间
     * @param updatedAt         更新时间
     */
    private Order(Long id, OrderNo orderNo, Long userId, OrderStatus status,
                  Money totalAmount, Money discountAmount, Money shippingAmount, Money taxAmount, Money payAmount,
                  String currency, PayChannel payChannel, PayStatus payStatus, String paymentExternalId, LocalDateTime payTime,
                  AddressSnapshot addressSnapshot, BuyerRemark buyerRemark,
                  CancelReason cancelReason, LocalDateTime cancelTime,
                  boolean addressChanged, OrderRefundReason lastRefundReason,
                  List<OrderItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        requireNotNull(orderNo, "订单号不能为空");
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(status, "订单状态不能为空");
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        requireNotNull(totalAmount, "订单总额不能为空");
        requireNotNull(discountAmount, "折扣金额不能为空");
        requireNotNull(shippingAmount, "运费不能为空");
        requireNotNull(taxAmount, "税费不能为空");
        requireNotNull(payAmount, "应付金额不能为空");
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.status = status;
        this.currency = normalizedCurrency;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.shippingAmount = shippingAmount;
        this.taxAmount = taxAmount;
        this.payAmount = payAmount;
        this.payChannel = payChannel == null ? PayChannel.NONE : payChannel;
        this.payStatus = payStatus == null ? PayStatus.NONE : payStatus;
        this.paymentExternalId = paymentExternalId == null ? null : paymentExternalId.strip();
        this.payTime = payTime;
        this.addressSnapshot = addressSnapshot;
        this.buyerRemark = buyerRemark;
        this.cancelReason = cancelReason;
        this.cancelTime = cancelTime;
        this.addressChanged = addressChanged;
        this.lastRefundReason = lastRefundReason;
        this.items = normalizeFieldList(items, Verifiable::validate);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        recalcAmountsFromItems();
    }

    /**
     * 创建一个新的订单实例
     *
     * @param orderNo         订单号 {@link OrderNo}
     * @param userId          用户 ID
     * @param currency        货币代码
     * @param items           订单项列表
     * @param discountAmount  可为空, 折扣金额
     * @param shippingAmount  可为空, 运费
     * @param addressSnapshot 可为空, 地址快照信息 {@link AddressSnapshot}
     * @param buyerRemark     可为空, 买家备注信息 {@link BuyerRemark}
     * @return 新创建的订单对象
     */
    public static Order create(OrderNo orderNo, Long userId, String currency,
                               List<OrderItem> items,
                               @Nullable Money discountAmount,
                               @Nullable Money shippingAmount,
                               @Nullable AddressSnapshot addressSnapshot,
                               @Nullable BuyerRemark buyerRemark) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        Money discount = discountAmount == null ? Money.zero(normalizedCurrency) : discountAmount;
        Money shipping = shippingAmount == null ? Money.zero(normalizedCurrency) : shippingAmount;
        Money tax = Money.zero(normalizedCurrency);
        Order order = new Order(null, orderNo, userId, OrderStatus.CREATED,
                Money.zero(normalizedCurrency), discount, shipping, tax, Money.zero(normalizedCurrency),
                normalizedCurrency, PayChannel.NONE, PayStatus.NONE, null, null,
                addressSnapshot, buyerRemark, null, null, false, null,
                items == null ? List.of() : items, LocalDateTime.now(), LocalDateTime.now());
        order.recalculateAmounts(discount, shipping, tax);
        return order;
    }

    /**
     * 从给定的参数中重构一个订单对象
     *
     * @param id                订单唯一标识符
     * @param orderNo           订单号 {@link OrderNo}
     * @param userId            用户 ID
     * @param status            订单状态 {@link OrderStatus}
     * @param totalAmount       订单总额
     * @param discountAmount    折扣金额
     * @param shippingAmount    运费
     * @param payAmount         应付金额
     * @param currency          货币代码
     * @param payChannel        支付渠道 {@link PayChannel}
     * @param payStatus         支付状态 {@link PayStatus}
     * @param paymentExternalId 外部支付系统中的支付流水号
     * @param payTime           支付时间
     * @param addressSnapshot   地址快照信息 {@link AddressSnapshot}
     * @param buyerRemark       买家备注信息 {@link BuyerRemark}
     * @param cancelReason      取消原因 {@link CancelReason}
     * @param cancelTime        取消时间
     * @param addressChanged    地址是否变更
     * @param lastRefundReason  最后一次退款原因
     * @param items             订单项列表
     * @param createdAt         创建时间
     * @param updatedAt         更新时间
     * @return 重构后的订单对象
     */
    public static Order reconstitute(Long id, OrderNo orderNo, Long userId, OrderStatus status,
                                     Money totalAmount, Money discountAmount, Money shippingAmount, Money taxAmount, Money payAmount,
                                     String currency, PayChannel payChannel, PayStatus payStatus,
                                     String paymentExternalId, LocalDateTime payTime,
                                     AddressSnapshot addressSnapshot, BuyerRemark buyerRemark,
                                     CancelReason cancelReason, LocalDateTime cancelTime,
                                     boolean addressChanged, @Nullable OrderRefundReason lastRefundReason,
                                     List<OrderItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, orderNo, userId, status, totalAmount, discountAmount, shippingAmount, taxAmount, payAmount,
                currency, payChannel, payStatus, paymentExternalId, payTime,
                addressSnapshot, buyerRemark, cancelReason, cancelTime,
                addressChanged, lastRefundReason, items, createdAt, updatedAt);
    }

    /**
     * 修改订单的收货地址 (聚合内只维护 "不允许重复修改" 的规则, 具体幂等/次数限制可由外部端口辅助)
     *
     * @param newAddress 新的收货地址快照, 不能为空
     * @param note       可选, 修改地址时的备注信息
     * @throws IllegalParamException 如果订单已修改过地址
     */
    public void changeAddress(@NotNull AddressSnapshot newAddress, @Nullable String note) {
        requireNotNull(newAddress, "收货地址不能为空");
        if (addressChanged)
            throw new ConflictException("订单已修改过地址");
        requireStatus(canChangeAddressStatus(status), "状态不允许修改地址");
        this.addressSnapshot = newAddress;
        this.addressChanged = true;
    }

    /**
     * 将订单状态标记为待支付
     *
     * <p>此方法会检查当前订单状态是否允许转换到待支付状态, 只有当订单状态为 {@code CREATED} 时, 才能成功转换
     * 如果当前订单状态不允许转换, 则抛出异常</p>
     *
     * @throws IllegalParamException 如果当前订单状态不允许转换到待支付状态
     */
    public void markPendingPayment() {
        requireStatus(this.status == OrderStatus.CREATED, this.status + " 状态不允许进入待支付");
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 标记订单支付已发起
     *
     * @param channel           支付渠道, 不能为空
     * @param paymentExternalId 外部支付系统中的支付流水号, 不能为空
     * @throws IllegalParamException 如果支付通道或支付 externalId 为空, 或者当前订单状态不允许发起支付
     */
    public void markPaymentInitiated(@NotNull PayChannel channel, @NotNull String paymentExternalId) {
        requireNotNull(channel, "支付通道不能为空");
        requireNotNull(paymentExternalId, "支付 externalId 不能为空");
        requireStatus(canPayStatus(status), status + " 状态不允许发起支付");
        if (this.status == OrderStatus.CREATED)
            this.status = OrderStatus.PENDING_PAYMENT;
        this.payChannel = channel;
        this.paymentExternalId = paymentExternalId.strip();
        this.payStatus = PayStatus.INIT;
    }

    /**
     * 标记订单为已支付状态
     *
     * <p>此方法会检查当前订单状态是否允许转换到已支付状态, 只有当订单状态满足条件时, 才能成功转换
     * 如果当前订单状态不允许转换, 则抛出异常</p>
     *
     * @param paymentExternalId 外部支付系统中的支付流水号, 不能为空
     * @param channel           支付渠道, 不能为空
     * @param payTime           支付时间, 不能为空
     * @throws IllegalArgumentException 如果支付 externalId, 支付通道或支付时间为空, 或者当前订单状态不允许标记为已支付
     */
    public void markPaid(@NotNull String paymentExternalId, @NotNull PayChannel channel, @NotNull LocalDateTime payTime) {
        requireNotNull(paymentExternalId, "支付 externalId 不能为空");
        requireNotNull(channel, "支付通道不能为空");
        requireNotNull(payTime, "支付时间不能为空");
        requireStatus(canMarkPaid(status), status + " 状态不允许标记已支付");
        this.paymentExternalId = paymentExternalId.strip();
        this.payChannel = channel;
        this.payStatus = PayStatus.SUCCESS;
        this.payTime = payTime;
        this.status = OrderStatus.PAID;
    }

    /**
     * 取消当前订单
     *
     * <p>此方法会检查当前订单状态是否允许取消, 并根据给定的取消原因和事件来源更新订单信息, 如果订单状态不允许取消, 则抛出异常</p>
     *
     * @param reason 取消原因, 不能为空
     * @param source 事件来源, 不能为空
     * @throws IllegalArgumentException 如果取消原因或事件来源为空, 或者当前订单状态不允许取消
     */
    public void cancel(@NotNull CancelReason reason, @NotNull OrderStatusEventSource source) {
        requireNotNull(reason, "取消原因不能为空");
        requireNotNull(source, "事件来源不能为空");
        requireStatus(canCancel(status), status + " 状态不允许取消");
        this.cancelReason = reason;
        this.cancelTime = LocalDateTime.now();
        this.status = OrderStatus.CANCELLED;
        if (this.payStatus != PayStatus.SUCCESS && this.payStatus != PayStatus.CLOSED)
            this.payStatus = PayStatus.CLOSED;
    }

    /**
     * 请求退款的方法, 用于根据给定的退款原因代码发起一个退款申请
     *
     * @param reasonCode  退款的原因代码, 必须非空
     * @param reasonText  可选参数, 提供更详细的退款理由
     * @param attachments 可选参数, 上传与退款相关的附件列表, 如图片或文件链接
     * @throws IllegalArgumentException 如果 {@code reasonCode} 为空 或 当前订单状态不允许请求退款
     */
    public void requestRefund(@NotNull OrderRefundReasonCode reasonCode, @Nullable String reasonText, @Nullable List<String> attachments) {
        requireNotNull(reasonCode, "退款原因不能为空");
        requireStatus(canRequestRefund(status), status + " 状态不允许申请退款");
        this.lastRefundReason = OrderRefundReason.of(reasonCode, reasonText, attachments);
        this.status = OrderStatus.REFUNDING;
    }

    /**
     * 管理侧确认订单退款操作
     *
     * <p>此方法用于将当前订单的状态从 {@code REFUNDING} 更新为 {@code REFUNDED}
     * 在调用此方法之前, 必须确保订单状态已经是 {@code REFUNDING}, 否则会抛出异常
     *
     * @param note 可选参数 一个字符串, 用于记录退款时的备注信息, 如果没有提供, 则默认为空
     * @throws IllegalStateException 当前订单状态不是 {@code REFUNDING} 时抛出此异常
     */
    public void confirmRefund(@Nullable String note) {
        requireStatus(this.status == OrderStatus.REFUNDING, "状态不允许确认退款");
        this.status = OrderStatus.REFUNDED;
    }

    /**
     * 关闭当前订单, 并设置关单原因及状态
     *
     * @param reason 关单的原因 不能为空
     * @throws IllegalArgumentException 如果传入的关单原因为空 或者 当前订单的状态不允许关闭时抛出此异常
     */
    public void close(@NotNull String reason) {
        requireNotNull(reason, "关单原因不能为空");
        requireStatus(canClose(status), "状态不允许关闭");
        this.status = OrderStatus.CLOSED;
    }

    /**
     * 将订单状态标记为已履约完成
     * <p>
     * 该方法首先检查当前订单的状态是否为 {@code PAID}, 如果状态不是 {@code PAID}, 则抛出异常, 因为只有在支付完成后才能进行履约操作
     * 如果状态检查通过, 则将订单状态更新为 {@code FULFILLED}
     *
     * @throws IllegalStateException 如果当前订单状态不是 {@code PAID}
     */
    public void markFulfilled() {
        requireStatus(this.status == OrderStatus.PAID, "状态不允许履约完成");
        this.status = OrderStatus.FULFILLED;
    }

    /**
     * 断言“状态机约束”满足, 不满足则抛出冲突异常
     *
     * <p>用于区分:</p>
     * <ul>
     *     <li>字段/参数不合法 → {@link IllegalParamException} (HTTP 400)</li>
     *     <li>状态机/并发语义冲突 → {@link ConflictException} (HTTP 409)</li>
     * </ul>
     *
     * @param ok  状态机约束是否满足
     * @param msg 不满足时的提示信息
     * @throws ConflictException 当 ok 为 false 时抛出
     */
    private static void requireStatus(boolean ok, String msg) {
        if (!ok)
            throw new ConflictException(msg);
    }

    /**
     * 重新计算订单金额, 包括折扣和运费 (折扣/运费变更后调用)
     *
     * @param discountAmount 折扣金额, 必须非空且货币类型需与订单内其他金额一致
     * @param shippingAmount 运费, 必须非空且货币类型需与订单内其他金额一致
     *                       <p>
     *                       此方法会检查提供的折扣金额和运费是否为 {@code null}, 如果是, 则抛出异常, 同时还会确保这些金额的货币类型与其他相关金额一致
     *                       在调整了折扣金额和运费后, 该方法会更新支付总金额, 即商品总额减去折扣加上运费, 注意, 折扣金额不能超过商品总额
     * @throws IllegalArgumentException 如果折扣金额大于商品总额或传入的参数为空
     */
    public void recalculateAmounts(@NotNull Money discountAmount, @NotNull Money shippingAmount, @NotNull Money taxAmount) {
        requireNotNull(discountAmount, "折扣金额不能为空");
        requireNotNull(shippingAmount, "运费不能为空");
        requireNotNull(taxAmount, "税费不能为空");
        ensureCurrency(discountAmount);
        ensureCurrency(shippingAmount);
        ensureCurrency(taxAmount);
        recalcAmountsFromItems();
        require(discountAmount.compareTo(totalAmount) <= 0, "折扣金额不能大于商品总额");
        this.discountAmount = discountAmount;
        this.shippingAmount = shippingAmount;
        this.taxAmount = taxAmount;
        this.payAmount = totalAmount.subtract(discountAmount).add(shippingAmount).add(taxAmount);
    }

    /**
     * 重新计算订单的总金额及数量等信息基于当前订单明细项
     * <p>
     * 此方法会遍历所有订单明细项, 并根据每个明细项中的小计金额和数量来更新订单的总数目、总金额以及应付金额
     * 同时, 该方法还会确保货币单位的一致性, 并处理折扣和运费对最终支付金额的影响。
     * <p>
     * 注意: 在调用此方法之前, 应保证 {@code items} 不为空且每个 {@code OrderItem} 对象都是有效的。
     *
     * @throws NullPointerException     如果 {@code items} 或者任意一个 {@code OrderItem} 为 null
     * @throws IllegalArgumentException 如果发现任何货币单位不匹配的情况
     */
    private void recalcAmountsFromItems() {
        requireNotNull(items, "订单明细不能为空");
        long totalMinor = 0L;
        int count = 0;
        for (OrderItem item : items) {
            requireNotNull(item, "订单明细不能为空");
            item.validate();
            Money subtotal = item.getSubtotalAmount();
            ensureCurrency(subtotal);
            totalMinor = Math.addExact(totalMinor, subtotal.getAmountMinor());
            count = Math.addExact(count, item.getQuantity());
        }
        this.itemsCount = count;
        this.totalAmount = Money.ofMinor(this.currency, totalMinor);
        ensureCurrency(this.discountAmount);
        ensureCurrency(this.shippingAmount);
        ensureCurrency(this.taxAmount);
        this.payAmount = totalAmount.subtract(this.discountAmount).add(this.shippingAmount).add(this.taxAmount);
    }

    /**
     * 确保给定的 <code>Money</code> 对象与当前对象具有相同的币种
     *
     * @param money 需要验证的金额对象 不能为 null, 并且其币种必须与当前实例的币种一致
     * @throws NullPointerException     如果 <code>money</code> 为 null
     * @throws IllegalArgumentException 如果 <code>money</code> 的币种与当前实例的币种不匹配
     */
    private void ensureCurrency(Money money) {
        requireNotNull(money, "金额不能为空");
        require(Objects.equals(this.currency, money.getCurrency()), "币种不一致");
    }

    /**
     * 判断给定的订单状态是否允许取消订单<br/>
     * 只有当订单处于 {@code CREATED}, {@code PENDING_PAYMENT} 或 {@code PAID} 状态时, 才能被取消
     *
     * @param status 订单的状态 {@link OrderStatus}
     * @return 如果订单处于可以被取消的状态, 返回 <code>true</code>; 否则返回 <code>false</code>
     */
    private static boolean canCancel(OrderStatus status) {
        return status == OrderStatus.CREATED || status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAID;
    }

    /**
     * 检查订单状态是否允许进行支付操作<br/>
     * 只有当订单状态为 {@code CREATED} 或者 {@code PENDING_PAYMENT} 时, 才能返回 true
     *
     * @param status 订单当前状态 {@link OrderStatus}
     * @return 如果订单状态允许进行支付操作, 则返回 true; 否则返回 false
     */
    private static boolean canPayStatus(OrderStatus status) {
        return status == OrderStatus.CREATED || status == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 检查订单状态是否允许标记为已支付<br/>
     * 只有当订单状态为 {@code CREATED} 或者 {@code PENDING_PAYMENT} 时, 才能返回 true
     *
     * @param status 订单当前状态 {@link OrderStatus}
     * @return 如果订单状态允许标记为已支付, 则返回 true; 否则返回 false
     */
    private static boolean canMarkPaid(OrderStatus status) {
        return status == OrderStatus.CREATED || status == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 检查给定的订单状态是否允许申请退款<br/>
     * 只有当订单处于 {@code PAID} 或 {@code FULFILLED} 状态时, 才能申请退款
     *
     * @param status 订单当前状态 {@link OrderStatus}
     * @return 如果订单处于已支付或已完成状态则返回 true, 表示可以申请退款; 否则返回 false
     */
    private static boolean canRequestRefund(OrderStatus status) {
        return status == OrderStatus.PAID || status == OrderStatus.FULFILLED;
    }

    /**
     * 判断订单状态是否允许关闭订单<br/>
     * 只有当订单处于 {@code CANCELLED}, {@code REFUNDED} 或 {@code FULFILLED} 状态时, 才能被关闭
     *
     * @param status 订单当前状态 {@link OrderStatus}
     * @return 如果订单状态为已取消, 已退款 或 已完成, 则返回 true, 表示可以关闭该订单; 否则返回 false
     */
    private static boolean canClose(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED || status == OrderStatus.FULFILLED;
    }

    /**
     * 检查是否可以更改订单地址的状态
     *
     * @param status 订单当前状态, 必须是 {@link OrderStatus} 枚举中的一个值
     * @return 如果订单状态允许更改地址则返回 true, 否则返回 false. 允许更改地址的状态包括 CREATED, PENDING_PAYMENT 和 PAID
     */
    private static boolean canChangeAddressStatus(OrderStatus status) {
        return status == OrderStatus.CREATED || status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAID;
    }

    /**
     * 验证订单信息的完整性与合法性
     *
     * @throws IllegalArgumentException 如果验证失败, 抛出此异常, 具体错误信息见异常消息
     */
    @Override
    public void validate() {
        requireNotNull(orderNo, "订单号不能为空");
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(status, "订单状态不能为空");
        requireNotNull(currency, "currency 不能为空");
        requireNotNull(items, "订单明细不能为空");
        ensureCurrency(totalAmount);
        ensureCurrency(discountAmount);
        ensureCurrency(shippingAmount);
        ensureCurrency(taxAmount);
        ensureCurrency(payAmount);
        require(payAmount.getAmountMinor() >= 0, "应付金额不能为负数");
        if (status == OrderStatus.CANCELLED) {
            requireNotNull(cancelReason, "取消原因不能为空");
            requireNotNull(cancelTime, "取消时间不能为空");
        }
    }
}

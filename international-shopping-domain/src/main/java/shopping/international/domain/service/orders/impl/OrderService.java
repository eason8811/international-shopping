package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.orders.IOrderAddressChangePort;
import shopping.international.domain.adapter.repository.orders.ICartRepository;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.adapter.repository.orders.IOrderProductRepository;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户侧订单领域服务默认实现
 *
 * <p>职责:</p>
 * <ul>
 *     <li>装配下单输入 (DIRECT/CART)</li>
 *     <li>校验库存/价格/折扣可用性并进行试算</li>
 *     <li>协调聚合 {@link Order} 与仓储 {@link IOrderRepository} 完成“创建订单并预占库存”等事务性落库</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    /**
     * 购物车仓储, 用于 source=CART 的下单输入
     */
    private final ICartRepository cartRepository;
    /**
     * 订单仓储, 用于订单聚合落库与查询
     */
    private final IOrderRepository orderRepository;
    /**
     * 折扣仓储, 用于校验折扣码并查询映射
     */
    private final IDiscountRepository discountRepository;
    /**
     * 订单域商品快照查询仓储, 用于读取 SKU 价格/库存/标题等
     */
    private final IOrderProductRepository orderProductRepository;
    /**
     * 用户仓储, 用于读取地址并构造订单地址快照
     */
    private final IUserRepository userRepository;
    /**
     * 订单改址标记端口, 用于 "仅一次改址"
     */
    private final IOrderAddressChangePort orderAddressChangePort;

    /**
     * 订单改址标记 TTL
     *
     * <p>终态会主动清理, TTL 用于防止极端情况下的脏数据长期驻留</p>
     */
    private static final Duration ADDRESS_CHANGED_TTL = Duration.ofDays(180);

    /**
     * 下单试算
     *
     * @param userId       当前用户 ID
     * @param source       下单来源
     * @param items        直接下单条目
     * @param addressId    收货地址 ID
     * @param currency     币种
     * @param discountCode 折扣码
     * @param buyerRemark  买家备注
     * @param locale       展示语言
     * @return 预览结果
     */
    @Override
    public @NotNull PreviewComputation preview(@NotNull Long userId,
                                               @NotNull OrderSource source,
                                               @Nullable List<ItemInput> items,
                                               @NotNull Long addressId,
                                               @NotNull String currency,
                                               @Nullable String discountCode,
                                               @Nullable String buyerRemark,
                                               @Nullable String locale) {
        return computePreview(userId, source, items, currency, discountCode, locale);
    }

    /**
     * 创建订单并预占库存
     *
     * @param userId       当前用户 ID
     * @param source       下单来源
     * @param items        直接下单条目
     * @param addressId    收货地址 ID
     * @param currency     币种
     * @param discountCode 折扣码
     * @param buyerRemark  买家备注
     * @param locale       展示语言
     * @return 已创建的订单聚合 (含明细)
     */
    @Override
    public @NotNull Order create(@NotNull Long userId,
                                 @NotNull OrderSource source,
                                 @Nullable List<ItemInput> items,
                                 @NotNull Long addressId,
                                 @NotNull String currency,
                                 @Nullable String discountCode,
                                 @Nullable String buyerRemark,
                                 @Nullable String locale) {
        PreviewComputation computation = computePreview(userId, source, items, currency, discountCode, locale);

        // 1) 地址快照与留言
        AddressSnapshot addressSnapshot = buildAddressSnapshot(userId, addressId);
        BuyerRemark remark = BuyerRemark.ofNullable(buyerRemark);

        // 2) 生成订单聚合 (订单号 26 位)
        OrderNo orderNo = OrderNo.generate();
        Order order = Order.create(
                orderNo,
                userId,
                currency,
                computation.items(),
                computation.discountAmount(),
                computation.shippingAmount(),
                addressSnapshot,
                remark
        );

        // 3) 事务落库 + 预占库存 + 可选清理购物车
        return orderRepository.createOrderAndReserveStock(
                order,
                OrderStatusEventSource.USER,
                null,
                computation.cartItemIdsToDelete(),
                computation.discountApplied()
        );
    }

    /**
     * 列出当前用户订单摘要
     *
     * @param userId      当前用户 ID
     * @param pageQuery   分页请求
     * @param status      状态过滤
     * @param createdFrom 创建时间起
     * @param createdTo   创建时间止
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<OrderSummaryRow> listMyOrders(@NotNull Long userId, @NotNull PageQuery pageQuery,
                                                             @Nullable OrderStatus status,
                                                             @Nullable LocalDateTime createdFrom,
                                                             @Nullable LocalDateTime createdTo) {
        pageQuery.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<OrderSummaryRow> rows = orderRepository.pageUserOrderSummaries(
                userId, status, createdFrom, createdTo, offset, limit
        );
        long total = orderRepository.countUserOrderSummaries(userId, status, createdFrom, createdTo);
        return new PageResult<>(rows, total);
    }

    /**
     * 获取当前用户订单详情
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @return 订单详情 Optional
     */
    @Override
    public @NotNull Optional<Order> getMyOrder(@NotNull Long userId, @NotNull OrderNo orderNo) {
        return orderRepository.findUserOrderDetail(userId, orderNo);
    }

    /**
     * 取消订单 (用户侧)
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @param reason  取消原因
     * @return 取消后的订单
     */
    @Override
    public @NotNull Order cancel(@NotNull Long userId, @NotNull OrderNo orderNo, @NotNull String reason) {
        Order order = orderRepository.findUserOrderDetail(userId, orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));
        OrderStatus from = order.getStatus();
        order.cancel(CancelReason.of(reason), OrderStatusEventSource.USER);
        Order cancelled = orderRepository.cancelAndReleaseStock(order, from, OrderStatusEventSource.USER, reason);

        // 终态清理改址标记
        orderAddressChangePort.clear(orderNo);
        return cancelled;
    }

    /**
     * 修改订单收货地址 (仅一次)
     *
     * @param userId       当前用户 ID
     * @param orderNo      订单号
     * @param newAddressId 新地址 ID
     * @param note         修改备注
     * @return 修改后的订单
     */
    @Override
    public @NotNull Order changeAddress(@NotNull Long userId, @NotNull OrderNo orderNo, @NotNull Long newAddressId, @Nullable String note) {
        Order order = orderRepository.findUserOrderDetail(userId, orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));

        // 1) 抢占“改址权”, 并发下保证仅一次
        boolean ok = orderAddressChangePort.tryMarkChanged(orderNo, ADDRESS_CHANGED_TTL);
        if (!ok)
            throw new ConflictException("订单已修改过地址");

        // 2) 聚合内维护“不允许重复修改”与状态机约束
        AddressSnapshot newSnapshot = buildAddressSnapshot(userId, newAddressId);
        order.changeAddress(newSnapshot, note);

        // 3) 持久化 address_snapshot
        return orderRepository.updateAddressSnapshot(order, newSnapshot);
    }

    /**
     * 申请订单退款 (整单)
     *
     * @param userId      当前用户 ID
     * @param orderNo     订单号
     * @param reasonCode  原因码
     * @param reasonText  原因补充
     * @param attachments 附件 URL
     * @return 更新后的订单
     */
    @Override
    public @NotNull Order requestRefund(@NotNull Long userId, @NotNull OrderNo orderNo,
                                        @NotNull OrderRefundReasonCode reasonCode,
                                        @Nullable String reasonText,
                                        @Nullable List<String> attachments) {
        Order order = orderRepository.findUserOrderDetail(userId, orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));
        OrderStatus from = order.getStatus();
        order.requestRefund(reasonCode, reasonText, attachments);

        String note = buildRefundNote(reasonCode, reasonText, attachments);
        return orderRepository.requestRefund(order, from, OrderStatusEventSource.USER, note);
    }

    /**
     * 计算订单预览 (复用在 preview/create 中)
     *
     * @param userId       用户 ID
     * @param source       来源
     * @param directItems  direct 条目
     * @param currency     币种
     * @param discountCode 折扣码
     * @param locale       展示语言
     * @return 预览计算结果
     */
    private PreviewComputation computePreview(@NotNull Long userId,
                                              @NotNull OrderSource source,
                                              @Nullable List<ItemInput> directItems,
                                              @NotNull String currency,
                                              @Nullable String discountCode,
                                              @Nullable String locale) {
        ResolvedItems resolved = resolveItems(userId, source, directItems);
        require(resolved.items() != null && !resolved.items().isEmpty(), "下单条目不能为空");

        // 1) 合并重复 SKU
        LinkedHashMap<Long, Integer> skuQtyMap = new LinkedHashMap<>();
        for (ItemInput input : resolved.items()) {
            requireNotNull(input, "items 不能为空");
            requireNotNull(input.skuId(), "skuId 不能为空");
            requireNotNull(input.quantity(), "quantity 不能为空");
            require(input.quantity() >= 1, "quantity 必须大于等于 1");
            int merged = skuQtyMap.getOrDefault(input.skuId(), 0);
            skuQtyMap.put(input.skuId(), Math.addExact(merged, input.quantity()));
        }

        List<Long> skuIds = new ArrayList<>(skuQtyMap.keySet());
        List<IOrderService.SkuSaleSnapshot> snapshots = orderProductRepository.listSkuSaleSnapshots(skuIds, locale, currency);
        Map<Long, IOrderService.SkuSaleSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(
                        SkuSaleSnapshot::skuId,
                        Function.identity()
                ));

        // 2) 构造订单明细快照并校验库存/价格
        List<OrderItem> orderItems = new ArrayList<>();
        Money totalAmount = Money.zero(currency);
        for (Long skuId : skuIds) {
            IOrderService.SkuSaleSnapshot snapshot = snapshotMap.get(skuId);
            if (snapshot == null)
                throw new ConflictException("ID 为 : " + skuId + " 的 SKU 不存在或不可售");

            Integer stock = snapshot.stock();
            if (stock == null)
                throw new ConflictException("ID 为 : " + skuId + " 的 SKU 库存不可用");

            int qty = skuQtyMap.get(skuId);
            if (stock < qty)
                throw new ConflictException("ID 为 : " + skuId + " 的 库存不足");

            BigDecimal unitPriceRaw = snapshot.unitPrice();
            if (unitPriceRaw == null)
                throw new ConflictException("ID 为 : " + skuId + " 的 SKU 价格不可用");
            Money unitPrice = Money.of(currency, unitPriceRaw);
            OrderItem item = OrderItem.snapshot(
                    snapshot.productId(),
                    snapshot.skuId(),
                    null,
                    snapshot.title(),
                    snapshot.skuAttrs(),
                    snapshot.coverImageUrl(),
                    unitPrice,
                    qty
            );
            orderItems.add(item);
            totalAmount = totalAmount.add(item.getSubtotalAmount());
        }

        // 3) 运费 (当前实现为 0)
        Money shipping = Money.zero(currency);

        // 4) 折扣试算
        DiscountComputation discountComputation = computeDiscount(currency, orderItems, discountCode);

        Money payAmount = totalAmount.subtract(discountComputation.discountAmount()).add(shipping);

        return new PreviewComputation(
                discountComputation.itemsWithDiscountCode(),
                totalAmount,
                discountComputation.discountAmount(),
                shipping,
                payAmount,
                currency,
                discountComputation.discountCodeId(),
                source == OrderSource.CART ? resolved.cartItemIdsToDelete() : null,
                discountComputation.discountApplied()
        );
    }

    /**
     * 解析 source=DIRECT/CART 的下单条目
     *
     * @param userId      用户 ID
     * @param source      来源
     * @param directItems DIRECT 输入条目
     * @return 解析结果
     */
    private ResolvedItems resolveItems(@NotNull Long userId, @NotNull OrderSource source, @Nullable List<ItemInput> directItems) {
        if (source == OrderSource.DIRECT) {
            if (directItems == null || directItems.isEmpty())
                throw new IllegalParamException("下单来源为 DIRECT 时 items 不能为空");
            return new ResolvedItems(directItems, null);
        }

        // source=CART
        List<CartItem> selected = cartRepository.listSelectedItems(userId);
        if (selected.isEmpty())
            throw new IllegalParamException("购物车中没有勾选的条目");

        List<ItemInput> items = new ArrayList<>();
        List<Long> idsToDelete = new ArrayList<>();
        for (CartItem item : selected) {
            items.add(new ItemInput(item.getSkuId(), item.getQuantity()));
            idsToDelete.add(item.getId());
        }
        return new ResolvedItems(items, idsToDelete);
    }

    /**
     * 计算折扣金额与拆分
     *
     * @param currency     币种
     * @param items        订单明细
     * @param discountCode 折扣码 (可为空)
     * @return 折扣计算结果
     */
    private DiscountComputation computeDiscount(@NotNull String currency, @NotNull List<OrderItem> items, @Nullable String discountCode) {
        if (discountCode == null || discountCode.isBlank())
            return new DiscountComputation(Money.zero(currency), items, null, null);

        DiscountCodeText codeText = DiscountCodeText.ofNullable(discountCode);

        DiscountCode code = discountRepository.findCodeByText(codeText)
                .orElseThrow(() -> new ConflictException("折扣码不存在或不可用"));
        if (code.isExpired(Clock.systemUTC()))
            throw new ConflictException("折扣码已过期");

        DiscountPolicy policy = discountRepository.findPolicyById(code.getPolicyId())
                .orElseThrow(() -> new ConflictException("折扣策略不存在"));

        // 1) 币种约束
        if (policy.getCurrency() != null && !policy.getCurrency().equals(currency))
            throw new ConflictException("折扣码币种不匹配");

        Money orderTotal = Money.zero(currency);
        for (OrderItem item : items)
            orderTotal = orderTotal.add(item.getSubtotalAmount());

        // 2) 门槛约束
        if (policy.getMinOrderAmount() != null) {
            Money min = Money.of(currency, policy.getMinOrderAmount());
            if (orderTotal.compareTo(min) < 0)
                throw new ConflictException("未满足折扣门槛");
        }

        // 3) 计算可用明细集合
        Set<Long> mappedProducts = null;
        if (code.getScopeMode() != DiscountScopeMode.ALL) {
            List<Long> ids = discountRepository.listCodeProductIds(code.getId());
            mappedProducts = new HashSet<>(ids);
        }

        List<OrderItem> eligible = new ArrayList<>();
        for (OrderItem item : items) {
            boolean ok = switch (code.getScopeMode()) {
                case ALL -> true;
                case INCLUDE -> mappedProducts != null && mappedProducts.contains(item.getProductId());
                case EXCLUDE -> mappedProducts == null || !mappedProducts.contains(item.getProductId());
            };
            if (ok)
                eligible.add(item);
        }
        if (eligible.isEmpty())
            throw new ConflictException("折扣码不适用于当前商品");

        // 4) 计算折扣拆分
        List<OrderDiscountApplied> appliedList = new ArrayList<>();
        Money discountAmount;

        if (policy.getApplyScope() == DiscountApplyScope.ORDER) {
            Money base = Money.zero(currency);
            for (OrderItem item : eligible)
                base = base.add(item.getSubtotalAmount());
            Money raw = computeRawDiscount(currency, policy, base);
            Money capped = capDiscount(currency, policy, raw, base);
            discountAmount = capped;

            appliedList.add(new OrderDiscountApplied(code.getId(), DiscountApplyScope.ORDER, null, capped.getAmount().toPlainString()));
        } else {
            // ITEM 级别: 逐行计算
            List<LineDiscount> lineDiscounts = new ArrayList<>();
            Money sum = Money.zero(currency);
            for (OrderItem item : eligible) {
                Money base = item.getSubtotalAmount();
                Money raw = computeRawDiscount(currency, policy, base);
                Money capped = capDiscount(currency, policy, raw, base);
                lineDiscounts.add(new LineDiscount(item.getSkuId(), capped));
                sum = sum.add(capped);
            }

            // maxDiscountAmount 作为订单级上限再次裁剪
            Money max = policy.getMaxDiscountAmount() == null ? null : Money.of(currency, policy.getMaxDiscountAmount());
            if (max != null && sum.compareTo(max) > 0) {
                Money remaining = max;
                List<LineDiscount> adjusted = new ArrayList<>();
                for (LineDiscount ld : lineDiscounts) {
                    if (remaining.isZero()) {
                        adjusted.add(new LineDiscount(ld.skuId, Money.zero(currency)));
                        continue;
                    }
                    Money use = ld.amount.compareTo(remaining) <= 0 ? ld.amount : remaining;
                    adjusted.add(new LineDiscount(ld.skuId, use));
                    remaining = remaining.subtract(use);
                }
                lineDiscounts = adjusted;
                sum = max;
            }
            discountAmount = sum;

            appliedList.addAll(
                    lineDiscounts.stream()
                            .map(ld -> new OrderDiscountApplied(
                                    code.getId(),
                                    DiscountApplyScope.ITEM,
                                    ld.skuId,
                                    ld.amount.getAmount().toPlainString()
                            ))
                            .toList()
            );
        }

        // 5) 将 discount_code_id 回写到明细快照上 (用于 order_item.discount_code_id)
        Set<Long> eligibleSkuIds = eligible.stream()
                .map(OrderItem::getSkuId)
                .collect(Collectors.toSet());

        List<OrderItem> itemsWithCode = new ArrayList<>();
        for (OrderItem item : items) {
            Long dcId = eligibleSkuIds.contains(item.getSkuId()) ? code.getId() : null;
            itemsWithCode.add(OrderItem.reconstitute(
                    item.getId(), item.getOrderId(), item.getProductId(), item.getSkuId(), dcId,
                    item.getTitle(), item.getSkuAttrs(), item.getCoverImageUrl(),
                    item.getUnitPrice(), item.getQuantity(), item.getSubtotalAmount(), item.getCreatedAt()
            ));
        }

        return new DiscountComputation(discountAmount, itemsWithCode, code.getId(), appliedList);
    }

    /**
     * 单行折扣临时结构
     */
    private record LineDiscount(Long skuId, Money amount) {
    }

    /**
     * 计算原始折扣 (不含 maxDiscountAmount 裁剪)
     *
     * @param currency 币种
     * @param policy   策略
     * @param base     折扣计算基数
     * @return 原始折扣金额
     */
    private static Money computeRawDiscount(String currency, DiscountPolicy policy, Money base) {
        if (policy.getStrategyType() == DiscountStrategyType.PERCENT) {
            BigDecimal percent = policy.getPercentOff();
            BigDecimal raw = base.getAmount()
                    .multiply(percent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return Money.of(currency, raw);
        }
        BigDecimal amountOff = policy.getAmountOff();
        return Money.of(currency, amountOff);
    }

    /**
     * 对折扣金额进行裁剪 (不超过 base, 不超过 maxDiscountAmount)
     *
     * @param currency 币种
     * @param policy   策略
     * @param raw      原始折扣金额
     * @param base     基数金额
     * @return 裁剪后的折扣金额
     */
    private static Money capDiscount(String currency, DiscountPolicy policy, Money raw, Money base) {
        Money capped = raw.compareTo(base) > 0 ? base : raw;
        if (policy.getMaxDiscountAmount() != null) {
            Money max = Money.of(currency, policy.getMaxDiscountAmount());
            if (capped.compareTo(max) > 0)
                capped = max;
        }
        return capped;
    }

    /**
     * 构造订单地址快照
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @return 地址快照值对象
     */
    private AddressSnapshot buildAddressSnapshot(@NotNull Long userId, @NotNull Long addressId) {
        return userRepository.findAddressById(userId, addressId)
                .map(addr -> {
                    if (addr.getPhone() == null || addr.getPhone().getValue() == null || addr.getPhone().getValue().isBlank())
                        throw new IllegalParamException("收货地址联系电话不能为空");
                    return AddressSnapshot.of(
                            addr.getReceiverName(),
                            addr.getPhone().getValue(),
                            addr.getCountry(),
                            addr.getProvince(),
                            addr.getCity(),
                            addr.getDistrict(),
                            addr.getAddressLine1(),
                            addr.getAddressLine2(),
                            addr.getZipcode()
                    );
                })
                .orElseThrow(() -> new IllegalParamException("收货地址不存在"));
    }

    /**
     * 构造退款申请的日志备注
     *
     * @param reasonCode  原因码
     * @param reasonText  原因补充
     * @param attachments 附件
     * @return note 文本 (最长 255)
     */
    private static String buildRefundNote(OrderRefundReasonCode reasonCode, @Nullable String reasonText, @Nullable List<String> attachments) {
        String base = "reasonCode=" + reasonCode.name();
        if (reasonText != null && !reasonText.isBlank())
            base += ", reasonText=" + reasonText.strip();
        if (attachments != null && !attachments.isEmpty())
            base += ", attachments=" + attachments.size();
        return base.length() <= 255 ? base : base.substring(0, 255);
    }
}

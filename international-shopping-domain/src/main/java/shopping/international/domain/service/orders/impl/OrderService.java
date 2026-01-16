package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.ICartRepository;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.adapter.repository.orders.IOrderProductRepository;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.common.FxRateLatest;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.common.IFxRateService;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.types.config.FxRateProperties;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.DiscountFailureException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.OrderDiscountRejectedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.*;

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
@Slf4j
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
     * 货币配置服务, 用于金额换算与舍入
     */
    private final ICurrencyConfigService currencyConfigService;
    /**
     * 汇率服务, 用于将金额固化为统一记账币种
     */
    private final IFxRateService fxRateService;
    /**
     * FX 配置(包含全站默认基准币种)
     */
    private final FxRateProperties fxRateProperties;


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
     * @param idempotencyKey 幂等键 (可为空)
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
                                 @Nullable String locale,
                                 @NotNull String idempotencyKey) {
        PreviewComputation computation = computePreview(userId, source, items, currency, discountCode, locale);
        if (discountCode != null && !computation.usedDiscount()) {
            String reason = computation.discountFailureReason() == null ? "折扣码不可用" : computation.discountFailureReason();
            throw new OrderDiscountRejectedException(reason);
        }

        // 1) 地址快照与留言
        AddressSnapshot addressSnapshot = buildAddressSnapshot(userId, addressId);
        BuyerRemark remark = BuyerRemark.ofNullable(buyerRemark);

        // 2) 生成订单聚合 (订单号 26 位)
        OrderNo orderNo = OrderNo.generate();
        Order order = Order.create(
                orderNo,
                userId,
                computation.currency(),
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
                computation.discountApplied(),
                idempotencyKey
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
        return orderRepository.cancelAndReleaseStock(order, from, OrderStatusEventSource.USER, reason);
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

        AddressSnapshot newSnapshot = buildAddressSnapshot(userId, newAddressId);
        order.changeAddress(newSnapshot, note);

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
        String orderCurrency = currency;
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
        List<IOrderService.SkuSaleSnapshot> snapshots = orderProductRepository.listSkuSaleSnapshots(skuIds, locale, orderCurrency);
        Map<Long, IOrderService.SkuSaleSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(
                        SkuSaleSnapshot::skuId,
                        Function.identity()
                ));

        // 汇率缺失/过期时回退 USD: 如果任一 SKU 在目标币种下价格不可用, 则整单回退 USD 重新取价
        if (!"USD".equalsIgnoreCase(orderCurrency)) {
            Map<Long, SkuSaleSnapshot> finalSnapshotMap = snapshotMap;
            boolean missingPrice = skuIds.stream().anyMatch(skuId -> {
                IOrderService.SkuSaleSnapshot s = finalSnapshotMap.get(skuId);
                return s == null || s.unitPriceMinor() == null;
            });
            if (missingPrice) {
                orderCurrency = "USD";
                snapshots = orderProductRepository.listSkuSaleSnapshots(skuIds, locale, orderCurrency);
                snapshotMap = snapshots.stream()
                        .collect(Collectors.toMap(SkuSaleSnapshot::skuId, Function.identity()));
            }
        }

        // 2) 构造订单明细快照并校验库存/价格
        List<OrderItem> orderItems = new ArrayList<>();
        Money totalAmount = Money.zero(orderCurrency);
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

            Long unitPriceMinor = snapshot.unitPriceMinor();
            if (unitPriceMinor == null)
                throw new ConflictException("ID 为 : " + skuId + " 的 SKU 价格不可用");
            Money unitPrice = Money.ofMinor(orderCurrency, unitPriceMinor);
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
        Money shipping = Money.zero(orderCurrency);
        // 3.1) 税费 (当前实现为 0)
        Money tax = Money.zero(orderCurrency);

        // 4) 折扣试算
        DiscountComputation discountComputation = null;
        String discountFailureReason = null;
        try {
            discountComputation = computeDiscount(orderCurrency, orderItems, discountCode);
        } catch (DiscountFailureException e) {
            log.warn("折扣计算失败, 原因: '{}'", e.getMessage());
            discountFailureReason = e.getMessage();
        }


        if (discountComputation != null) {
            Money payAmount = totalAmount.subtract(discountComputation.discountAmount()).add(shipping).add(tax);
            return new PreviewComputation(
                    discountComputation.itemsWithDiscountCode(),
                    totalAmount,
                    discountComputation.discountAmount(),
                    shipping,
                    payAmount,
                    orderCurrency,
                    discountComputation.discountCodeId(),
                    source == OrderSource.CART ? resolved.cartItemIdsToDelete() : null,
                    discountComputation.discountApplied(),
                    true,
                    discountFailureReason
            );
        }

        Money payAmount = totalAmount.add(shipping).add(tax);
        return new PreviewComputation(
                orderItems,
                totalAmount,
                Money.zero(orderCurrency),
                shipping,
                payAmount,
                orderCurrency,
                null,
                source == OrderSource.CART ? resolved.cartItemIdsToDelete() : null,
                List.of(),
                false,
                discountFailureReason
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
            throw new DiscountFailureException("折扣码不能为空");

        DiscountCodeText codeText = DiscountCodeText.ofNullable(discountCode);
        if (codeText == null)
            throw new DiscountFailureException("折扣码不能为空");

        DiscountCode code = discountRepository.findCodeByText(codeText)
                .orElseThrow(() -> new DiscountFailureException("折扣码不存在或不可用"));
        if (code.isExpired(Clock.systemUTC()))
            throw new DiscountFailureException("折扣码已过期");

        DiscountPolicy policy = discountRepository.findPolicyById(code.getPolicyId())
                .orElseThrow(() -> new ConflictException("折扣策略不存在"));

        DiscountPolicyAmount amountConfig = policy.resolveAmount(currency);
        if (amountConfig != null && !currency.equalsIgnoreCase(amountConfig.getCurrency()))
            throw new DiscountFailureException("折扣码不支持当前币种");
        if (policy.getStrategyType() == DiscountStrategyType.AMOUNT
                && (amountConfig == null || amountConfig.getAmountOffMinor() == null))
            throw new DiscountFailureException("折扣码不支持当前币种");

        // 2) 计算可用明细集合
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
            throw new DiscountFailureException("折扣码不适用于当前商品");

        Money totalAmount = Money.zero(currency);
        for (OrderItem item : items)
            totalAmount = totalAmount.add(item.getSubtotalAmount());

        // 3) 门槛约束: 按“可折扣子集”小计判断 (尤其 EXCLUDE 场景)
        if (amountConfig != null && amountConfig.getMinOrderAmountMinor() != null) {
            Money eligibleTotal = Money.zero(currency);
            for (OrderItem item : eligible)
                eligibleTotal = eligibleTotal.add(item.getSubtotalAmount());
            Money min = Money.ofMinor(currency, amountConfig.getMinOrderAmountMinor());
            if (eligibleTotal.compareTo(min) < 0)
                throw new DiscountFailureException("未满足折扣门槛");
        }

        // 4) 计算折扣拆分
        List<IOrderService.OrderDiscountApplied> appliedList = new ArrayList<>();
        Money discountAmount;
        CurrencyConfig currencyConfig = currencyConfigService.get(currency);
        String baseCurrency = normalizeCurrency(fxRateProperties.getBaseCurrency());
        CurrencyConfig baseCurrencyConfig = null;
        FxRateLatest fxRateLatest = null;
        if (baseCurrency.isBlank())
            baseCurrency = "USD";
        if (!baseCurrency.equalsIgnoreCase(currency)) {
            fxRateLatest = fxRateService.getLatest(baseCurrency, currency);
            if (fxRateLatest == null)
                throw new DiscountFailureException("汇率 '" + baseCurrency + "' -> '" + currency + "' 不存在, 无法计算折扣记账金额");
            baseCurrencyConfig = currencyConfigService.get(baseCurrency);
        }

        if (policy.getApplyScope() == DiscountApplyScope.ORDER) {
            Money base = Money.zero(currency);
            for (OrderItem item : eligible)
                base = base.add(item.getSubtotalAmount());
            Money raw = computeRawDiscount(currencyConfig, policy, amountConfig, base);
            Money capped = capDiscount(currency, amountConfig, raw, base);
            if (capped.compareTo(totalAmount) >= 0)
                capped = Money.ofMinor(currency, Math.max(0L, totalAmount.getAmountMinor() - 1L));
            discountAmount = capped;
            if (discountAmount.isZero())
                throw new DiscountFailureException("折扣金额为 0");

            appliedList.add(
                    toAccountingApplied(
                            code.getId(),
                            DiscountApplyScope.ORDER,
                            null,
                            capped.getAmountMinor(),
                            baseCurrency,
                            currency,
                            currencyConfig,
                            baseCurrencyConfig,
                            fxRateLatest
                    )
            );
        } else {
            // ITEM 级别: 逐行计算
            List<LineDiscount> lineDiscounts = new ArrayList<>();
            Money sum = Money.zero(currency);
            for (OrderItem item : eligible) {
                Money base = item.getSubtotalAmount();
                Money raw = computeRawDiscount(currencyConfig, policy, amountConfig, base);
                // AMOUNT 折扣在 ITEM 模式下按“每件立减”语义：单件折扣 * quantity
                if (policy.getStrategyType() == DiscountStrategyType.AMOUNT)
                    raw = raw.multiply(item.getQuantity());
                Money lineCapped = raw.compareTo(base) > 0 ? base : raw;
                lineDiscounts.add(new LineDiscount(item.getSkuId(), lineCapped, base.getAmountMinor()));
                sum = sum.add(lineCapped);
            }

            // maxDiscountAmount 作为订单级上限再次裁剪
            Money max = amountConfig == null || amountConfig.getMaxDiscountAmountMinor() == null
                    ? null
                    : Money.ofMinor(currency, amountConfig.getMaxDiscountAmountMinor());
            if (max != null && sum.compareTo(max) > 0) {
                lineDiscounts = applyOrderCapBySubtotalProportion(currency, max.getAmountMinor(), lineDiscounts);
                sum = max;
            }
            if (sum.compareTo(totalAmount) >= 0) {
                long capMinor = Math.max(0L, totalAmount.getAmountMinor() - 1L);
                lineDiscounts = applyOrderCapBySubtotalProportion(currency, capMinor, lineDiscounts);
                sum = Money.ofMinor(currency, capMinor);
            }
            discountAmount = sum;
            if (discountAmount.isZero())
                throw new DiscountFailureException("折扣金额为 0");

            final String baseCurrencyFinal = baseCurrency;
            final CurrencyConfig baseCurrencyConfigFinal = baseCurrencyConfig;
            final FxRateLatest fxRateLatestFinal = fxRateLatest;

            Map<Long, LineDiscount> lineDiscountBySkuIdMap = lineDiscounts.stream()
                    .collect(Collectors.toMap(LineDiscount::skuId, Function.identity()));
            for (OrderItem item : items) {
                LineDiscount lineDiscount = lineDiscountBySkuIdMap.get(item.getSkuId());
                Map<String, Object> skuAttrs = item.getSkuAttrs();
                if (skuAttrs == null)
                    continue;
                if (lineDiscount == null)
                    continue;
                String discountAmountString = lineDiscount.amount().toMajorString(currencyConfig);
                try {
                    skuAttrs.put("applied_discount_amount", discountAmountString);
                } catch (UnsupportedOperationException ignore) {
                    // ignore immutable map
                }
            }
            appliedList.addAll(lineDiscounts.stream()
                    .filter(ld -> ld.amount.getAmountMinor() > 0)
                    .map(ld ->
                            toAccountingApplied(
                                    code.getId(),
                                    DiscountApplyScope.ITEM,
                                    ld.skuId,
                                    ld.amount.getAmountMinor(),
                                    baseCurrencyFinal,
                                    currency,
                                    currencyConfig,
                                    baseCurrencyConfigFinal,
                                    fxRateLatestFinal
                            )
                    )
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
            itemsWithCode.add(
                    OrderItem.reconstitute(
                            item.getId(),
                            item.getOrderId(),
                            item.getProductId(),
                            item.getSkuId(),
                            dcId,
                            item.getTitle(),
                            item.getSkuAttrs(),
                            item.getCoverImageUrl(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getSubtotalAmount(),
                            item.getCreatedAt()
                    )
            );
        }

        return new DiscountComputation(discountAmount, itemsWithCode, code.getId(), appliedList);
    }

    /**
     * 构造包含 "统一记账币种金额 + FX 快照" 的折扣应用记录
     *
     * @param discountCodeId     折扣码ID 必填
     * @param appliedScope       折扣应用范围 必填
     * @param skuId              库存单位ID 可为空
     * @param appliedAmountMinor 应用金额(最小单位) 必填
     * @param baseCurrency       基础货币 必填
     * @param quoteCurrency      引用货币 必填
     * @param quoteCfg           引用货币配置 必填
     * @param baseCfg            基础货币配置 可为空
     * @param fxRateLatest       最新汇率信息 可为空
     * @return 返回一个 {@link IOrderService.OrderDiscountApplied} 对象, 包含了转换后的折扣应用信息
     */
    private static @NotNull IOrderService.OrderDiscountApplied toAccountingApplied(@NotNull Long discountCodeId,
                                                                                   @NotNull DiscountApplyScope appliedScope,
                                                                                   @Nullable Long skuId,
                                                                                   long appliedAmountMinor,
                                                                                   @NotNull String baseCurrency,
                                                                                   @NotNull String quoteCurrency,
                                                                                   @NotNull CurrencyConfig quoteCfg,
                                                                                   @Nullable CurrencyConfig baseCfg,
                                                                                   @Nullable FxRateLatest fxRateLatest) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return new IOrderService.OrderDiscountApplied(
                    discountCodeId,
                    appliedScope,
                    skuId,
                    appliedAmountMinor,
                    baseCurrency,
                    appliedAmountMinor,
                    null,
                    null,
                    null
            );
        }
        requireNotNull(baseCfg, "baseCfg 不能为空");
        requireNotNull(fxRateLatest, "fxRateLatest 不能为空");
        long baseMinor = convertQuoteMinorToBaseMinor(baseCfg, quoteCfg, appliedAmountMinor, fxRateLatest.rate());
        return new IOrderService.OrderDiscountApplied(
                discountCodeId,
                appliedScope,
                skuId,
                appliedAmountMinor,
                baseCurrency,
                baseMinor,
                fxRateLatest.rate(),
                fxRateLatest.asOf(),
                fxRateLatest.provider()
        );
    }

    /**
     * quoteMinor(订单币种) -> baseMinor(统一记账币种)
     *
     * <p>rate: 1 base = rate quote</p>
     *
     * @param baseCfg    基础货币配置信息, 不能为 null
     * @param quoteCfg   报价货币配置信息, 不能为 null
     * @param quoteMinor 报价货币的小数单位金额, 必须大于等于 0
     * @param rate       汇率, 从报价货币到基础货币的换算比率, 不能为 null
     * @return 转换后的基础货币的小数单位金额
     * @throws IllegalArgumentException 如果 quoteMinor 小于 0
     */
    private static long convertQuoteMinorToBaseMinor(@NotNull CurrencyConfig baseCfg,
                                                     @NotNull CurrencyConfig quoteCfg,
                                                     long quoteMinor,
                                                     @NotNull BigDecimal rate) {
        require(quoteMinor >= 0, "金额不能为负数");
        BigDecimal quoteMajor = quoteCfg.toMajor(quoteMinor);
        BigDecimal baseMajor = quoteMajor.divide(rate, 18, RoundingMode.HALF_UP);
        return baseCfg.toMinorRounded(baseMajor);
    }

    /**
     * 单行折扣临时结构
     */
    private record LineDiscount(Long skuId, Money amount, long subtotalAmountMinor) {
    }

    /**
     * 根据订单小计比例分配订单折扣上限, 并调整每个商品行的折扣金额
     *
     * @param currency                 货币代码 不能为空
     * @param cappedOrderDiscountMinor 订单折扣上限 (以最小货币单位表示) 不能为负数
     * @param lineDiscounts            商品行折扣列表 不能为空
     * @return 调整后的商品行折扣列表
     */
    static @NotNull List<LineDiscount> applyOrderCapBySubtotalProportion(@NotNull String currency,
                                                                         long cappedOrderDiscountMinor,
                                                                         @NotNull List<LineDiscount> lineDiscounts) {
        requireNotNull(currency, "currency 不能为空");
        require(cappedOrderDiscountMinor >= 0, "cappedOrderDiscountMinor 不能为负数");
        requireNotNull(lineDiscounts, "lineDiscounts 不能为空");
        if (lineDiscounts.isEmpty())
            return List.of();

        int n = lineDiscounts.size();
        long[] weights = new long[n];
        long[] caps = new long[n];
        for (int i = 0; i < n; i++) {
            LineDiscount ld = lineDiscounts.get(i);
            weights[i] = Math.max(0L, ld.subtotalAmountMinor);
            caps[i] = ld.amount.getAmountMinor();
        }

        long[] allocated = splitDiscountMinorWithCaps(cappedOrderDiscountMinor, weights, caps);
        List<LineDiscount> adjusted = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LineDiscount ld = lineDiscounts.get(i);
            adjusted.add(new LineDiscount(ld.skuId, Money.ofMinor(currency, allocated[i]), ld.subtotalAmountMinor));
        }
        return adjusted;
    }

    /**
     * 将订单级折扣总额按权重拆分到各行, 同时不超过各行 cap
     *
     * <p>算法：按权重做比例分摊 (largest remainder 处理舍入), 若某行触顶则将剩余折扣在剩余行中继续按权重分摊</p>
     *
     * @param totalMinor 需拆分的折扣总额 (最小单位)
     * @param weights    权重 (例如各行 subtotalMinor), 可为 0
     * @param caps       各行可承接的折扣上限 (最小单位)
     * @return 每行拆分后的折扣 (最小单位) , sum=totalMinor 且每行 <= cap
     */
    static long[] splitDiscountMinorWithCaps(long totalMinor, long[] weights, long[] caps) {
        require(totalMinor >= 0, "totalMinor 不能为负数");
        requireNotNull(weights, "weights 不能为空");
        requireNotNull(caps, "caps 不能为空");
        require(weights.length == caps.length, "weights/caps 长度不一致");

        int n = weights.length;
        long capSum = 0L;
        for (int i = 0; i < n; i++) {
            require(caps[i] >= 0, "cap 不能为负数");
            capSum = Math.addExact(capSum, caps[i]);
        }
        if (totalMinor >= capSum)
            return Arrays.copyOf(caps, n);

        long[] allocated = new long[n];
        boolean[] active = new boolean[n];
        int activeCount = 0;
        for (int i = 0; i < n; i++) {
            if (caps[i] > 0) {
                active[i] = true;
                activeCount++;
            }
        }

        long remaining = totalMinor;
        while (remaining > 0 && activeCount > 0) {
            int[] activeIdx = new int[activeCount];
            long[] activeWeights = new long[activeCount];
            int p = 0;
            for (int i = 0; i < n; i++) {
                if (!active[i])
                    continue;
                long capRemaining = caps[i] - allocated[i];
                if (capRemaining <= 0) {
                    active[i] = false;
                    activeCount--;
                    continue;
                }
                activeIdx[p] = i;
                activeWeights[p] = Math.max(0L, weights[i]);
                p++;
            }
            if (p == 0)
                break;
            if (p != activeCount) {
                activeIdx = Arrays.copyOf(activeIdx, p);
                activeWeights = Arrays.copyOf(activeWeights, p);
                activeCount = p;
            }

            long[] addition = splitMinorByWeights(remaining, activeWeights);
            long unallocated = 0L;
            for (int j = 0; j < activeIdx.length; j++) {
                int i = activeIdx[j];
                long capRemaining = caps[i] - allocated[i];
                long add = addition[j];
                long use = Math.min(add, capRemaining);
                allocated[i] += use;
                unallocated += (add - use);
                if (allocated[i] == caps[i]) {
                    active[i] = false;
                    activeCount--;
                }
            }
            remaining = unallocated;
        }

        return allocated;
    }

    /**
     * 根据给定的权重数组将总数 totalMinor 分配到各个部分
     *
     * @param totalMinor 总数, 必须是非负数
     * @param weights    权重数组, 用于确定每个部分应分配的数量比例, 不能为空
     * @return 返回一个 long 数组, 其中每个元素表示对应于输入 weights 数组中相同位置权重所分配到的数值
     */
    private static long[] splitMinorByWeights(long totalMinor, long[] weights) {
        require(totalMinor >= 0, "totalMinor 不能为负数");
        requireNotNull(weights, "weights 不能为空");
        int n = weights.length;
        long[] result = new long[n];
        if (n == 0 || totalMinor == 0)
            return result;

        long weightSum = 0L;
        for (long weight : weights)
            weightSum = Math.addExact(weightSum, Math.max(0L, weight));

        if (weightSum == 0L) {
            long base = totalMinor / n;
            long rem = totalMinor % n;
            Arrays.fill(result, base);
            for (int i = 0; i < rem; i++)
                result[i]++;
            return result;
        }

        BigDecimal total = BigDecimal.valueOf(totalMinor);
        BigDecimal weightSumBd = BigDecimal.valueOf(weightSum);
        BigDecimal[] remainders = new BigDecimal[n];
        long floorSum = 0L;
        for (int i = 0; i < n; i++) {
            long w = Math.max(0L, weights[i]);
            if (w == 0L) {
                remainders[i] = BigDecimal.ZERO;
                continue;
            }
            BigDecimal exact = total
                    .multiply(BigDecimal.valueOf(w))
                    .divide(weightSumBd, 20, RoundingMode.DOWN);
            long floor = exact.setScale(0, RoundingMode.DOWN).longValueExact();
            result[i] = floor;
            floorSum += floor;
            remainders[i] = exact.subtract(BigDecimal.valueOf(floor));
        }

        long leftover = totalMinor - floorSum;
        if (leftover <= 0L)
            return result;

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++)
            order[i] = i;
        Arrays.sort(order, (a, b) -> {
            int cmp = remainders[b].compareTo(remainders[a]);
            if (cmp != 0)
                return cmp;
            return Integer.compare(a, b);
        });
        for (int k = 0; k < leftover; k++)
            result[order[k]]++;

        return result;
    }

    /**
     * 计算原始折扣 (不含 maxDiscountAmount 裁剪)
     *
     * @param currencyConfig 币种配置
     * @param policy         策略
     * @param amountConfig   币种金额配置 (可能为空, 由策略类型决定)
     * @param base           折扣计算基数
     * @return 原始折扣金额
     */
    private static Money computeRawDiscount(CurrencyConfig currencyConfig, DiscountPolicy policy, DiscountPolicyAmount amountConfig, Money base) {
        if (policy.getStrategyType() == DiscountStrategyType.PERCENT) {
            BigDecimal percent = policy.getPercentOff();
            BigDecimal rawMinor = BigDecimal.valueOf(base.getAmountMinor())
                    .multiply(percent)
                    .divide(BigDecimal.valueOf(100), 0, currencyConfig.roundingMode());
            return Money.ofMinor(currencyConfig.code(), rawMinor.longValueExact());
        }
        if (amountConfig == null || amountConfig.getAmountOffMinor() == null)
            throw new ConflictException("折扣策略未配置该币种的折扣金额");
        return Money.ofMinor(currencyConfig.code(), amountConfig.getAmountOffMinor());
    }

    /**
     * 对折扣金额进行裁剪 (不超过 base, 不超过 maxDiscountAmount)
     *
     * @param currency     币种
     * @param amountConfig 币种金额配置
     * @param raw          原始折扣金额
     * @param base         基数金额
     * @return 裁剪后的折扣金额
     */
    private static Money capDiscount(String currency, DiscountPolicyAmount amountConfig, Money raw, Money base) {
        Money capped = raw.compareTo(base) > 0 ? base : raw;
        if (amountConfig != null && amountConfig.getMaxDiscountAmountMinor() != null) {
            Money max = Money.ofMinor(currency, amountConfig.getMaxDiscountAmountMinor());
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

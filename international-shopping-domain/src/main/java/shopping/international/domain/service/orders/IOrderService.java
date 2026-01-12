package shopping.international.domain.service.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.Money;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.types.enums.FxRateProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户侧订单领域服务接口
 *
 * <p>覆盖用户侧下单链路:</p>
 * <ul>
 *     <li>下单试算 (preview)</li>
 *     <li>创建订单并预占库存</li>
 *     <li>查询订单列表/详情</li>
 *     <li>取消订单, 修改地址, 申请退款</li>
 * </ul>
 */
public interface IOrderService {

    /**
     * SKU 可售快照 (用于下单/试算)
     *
     * <p>该类型是订单域在下单阶段对商品域数据的读取投影, 用于组装订单项快照</p>
     *
     * @param skuId          SKU ID
     * @param productId      商品 ID
     * @param title          商品标题 (按 locale 覆盖后)
     * @param coverImageUrl  商品封面图 URL
     * @param skuAttrs       SKU 属性快照 (可为空)
     * @param unitPriceMinor 单价 (订单币种最小单位)
     * @param stock          当前库存
     */
    record SkuSaleSnapshot(Long skuId,
                           Long productId,
                           String title,
                           String coverImageUrl,
                           @Nullable Map<String, Object> skuAttrs,
                           Long unitPriceMinor,
                           Integer stock) {
    }

    /**
     * 下单条目输入
     *
     * @param skuId    SKU ID
     * @param quantity 数量
     */
    record ItemInput(Long skuId, Integer quantity) {
    }

    /**
     * 订单输入解析结果
     *
     * @param items               下单条目
     * @param cartItemIdsToDelete 需要从购物车中删除的商品项 ID 列表
     */
    record ResolvedItems(List<ItemInput> items, List<Long> cartItemIdsToDelete) {
    }

    /**
     * 订单折扣使用情况
     *
     * <p>用于在订单落库后写入 {@code order_discount_applied}</p>
     *
     * @param discountCodeId         折扣码 ID
     * @param appliedScope           应用范围
     * @param skuId                  明细级折扣关联的 SKU ID (订单级折扣为 null)
     * @param appliedAmountMinor     实际抵扣金额 (最小货币单位)
     * @param baseCurrency           统一记账币种(如 USD)
     * @param appliedAmountBaseMinor 实际抵扣金额(统一记账币种,最小货币单位)
     * @param fxRate                 折扣换算汇率快照(1 base = rate quote), base=baseCurrency, quote=订单币种
     * @param fxAsOf                 汇率时间点/采样时间(快照)
     * @param fxProvider             汇率数据源(快照)
     */
    record OrderDiscountApplied(Long discountCodeId,
                                @NotNull DiscountApplyScope appliedScope,
                                @Nullable Long skuId,
                                long appliedAmountMinor,
                                @NotNull String baseCurrency,
                                long appliedAmountBaseMinor,
                                @Nullable BigDecimal fxRate,
                                @Nullable LocalDateTime fxAsOf,
                                @Nullable FxRateProvider fxProvider) {
    }

    /**
     * 折扣计算内部结果
     *
     * @param discountAmount        折扣金额, 表示应用于订单的总折扣额度
     * @param itemsWithDiscountCode 包含了应用了折扣码的所有订单项
     * @param discountCodeId        指示应用折扣时使用的折扣码标识符
     * @param discountApplied       包含了创建折扣应用记录的信息
     */
    record DiscountComputation(Money discountAmount,
                               @NotNull List<OrderItem> itemsWithDiscountCode,
                               Long discountCodeId,
                               @NotNull List<OrderDiscountApplied> discountApplied) {
        /**
         * 构造函数, 用于初始化折扣计算相关的所有必要信息
         *
         * @param discountAmount        折扣金额, 表示应用于订单的总折扣额度
         * @param itemsWithDiscountCode 包含了应用了折扣码的所有订单项
         * @param discountCodeId        指示应用折扣时使用的折扣码标识符
         * @param discountApplied       包含了创建折扣应用记录的信息
         */
        public DiscountComputation(Money discountAmount,
                                   @Nullable List<OrderItem> itemsWithDiscountCode,
                                   @Nullable Long discountCodeId,
                                   @Nullable List<OrderDiscountApplied> discountApplied) {
            this.discountAmount = discountAmount;
            this.itemsWithDiscountCode = itemsWithDiscountCode == null ? List.of() : List.copyOf(itemsWithDiscountCode);
            this.discountCodeId = discountCodeId;
            this.discountApplied = discountApplied == null ? List.of() : List.copyOf(discountApplied);
        }
    }

    /**
     * 预览计算内部结果
     *
     * @param items                 用于创建订单的商品项列表
     * @param totalAmount           订单总金额
     * @param discountAmount        折扣金额
     * @param shippingAmount        运费金额
     * @param payAmount             应付金额
     * @param currency              货币类型
     * @param discountCodeId        折扣码 ID
     * @param cartItemIdsToDelete   需要从购物车中删除的商品项 ID 列表
     * @param discountApplied       应用到订单上的折扣创建列表
     * @param usedDiscount          是否使用了折扣 (true: 使用了折扣, false: 未使用折扣)
     * @param discountFailureReason 未使用折扣的原因 (可为空)
     */
    record PreviewComputation(List<OrderItem> items,
                              Money totalAmount,
                              Money discountAmount,
                              Money shippingAmount,
                              Money payAmount,
                              String currency,
                              Long discountCodeId,
                              @NotNull List<Long> cartItemIdsToDelete,
                              @NotNull List<OrderDiscountApplied> discountApplied,
                              @NotNull Boolean usedDiscount,
                              @Nullable String discountFailureReason) {
        /**
         * 构造函数, 用于初始化一个预览计算对象, 该对象包含了订单创建时需要的所有金额相关的信息和可能的折扣信息
         *
         * @param items                 用于创建订单的商品项列表
         * @param totalAmount           订单总金额
         * @param discountAmount        折扣金额
         * @param shippingAmount        运费金额
         * @param payAmount             应付金额
         * @param currency              货币类型
         * @param discountCodeId        折扣码 ID
         * @param cartItemIdsToDelete   需要从购物车中删除的商品项 ID 列表
         * @param discountApplied       应用到订单上的折扣创建列表
         * @param usedDiscount          是否使用了折扣 (true: 使用了折扣, false: 未使用折扣)
         * @param discountFailureReason 未使用折扣的原因 (可为空)
         */
        public PreviewComputation(List<OrderItem> items,
                                  Money totalAmount,
                                  Money discountAmount,
                                  Money shippingAmount,
                                  Money payAmount,
                                  String currency,
                                  @Nullable Long discountCodeId,
                                  @Nullable List<Long> cartItemIdsToDelete,
                                  @Nullable List<OrderDiscountApplied> discountApplied,
                                  @NotNull Boolean usedDiscount,
                                  @Nullable String discountFailureReason) {
            this.items = items;
            this.totalAmount = totalAmount;
            this.discountAmount = discountAmount;
            this.shippingAmount = shippingAmount;
            this.payAmount = payAmount;
            this.currency = currency;
            this.discountCodeId = discountCodeId;
            this.cartItemIdsToDelete = cartItemIdsToDelete == null ? List.of() : List.copyOf(cartItemIdsToDelete);
            this.discountApplied = discountApplied == null ? List.of() : List.copyOf(discountApplied);
            this.usedDiscount = usedDiscount;
            this.discountFailureReason = discountFailureReason;
        }
    }

    /**
     * 用户侧订单摘要行
     *
     * @param orderNo             订单号
     * @param status              订单状态
     * @param itemsCount          商品件数
     * @param totalAmountMinor    商品总额 (最小货币单位)
     * @param discountAmountMinor 折扣金额 (最小货币单位)
     * @param shippingAmountMinor 运费 (最小货币单位)
     * @param taxAmountMinor      税费 (最小货币单位)
     * @param payAmountMinor      应付金额 (最小货币单位)
     * @param currency            币种
     * @param payChannel          支付渠道
     * @param payStatus           支付状态
     * @param payTime             支付时间 (可为空)
     * @param createdAt           创建时间
     */
    record OrderSummaryRow(String orderNo,
                           OrderStatus status,
                           Integer itemsCount,
                           long totalAmountMinor,
                           long discountAmountMinor,
                           long shippingAmountMinor,
                           long taxAmountMinor,
                           long payAmountMinor,
                           String currency,
                           PayChannel payChannel,
                           PayStatus payStatus,
                           @Nullable LocalDateTime payTime,
                           LocalDateTime createdAt) {
    }

    /**
     * 下单试算
     *
     * @param userId       当前用户 ID
     * @param source       下单来源
     * @param items        直接下单条目 (source=DIRECT 时必填)
     * @param addressId    收货地址 ID
     * @param currency     币种
     * @param discountCode 折扣码 (可为空)
     * @param buyerRemark  买家备注 (可为空)
     * @param locale       展示语言 (可为空)
     * @return 预览结果
     */
    @NotNull
    PreviewComputation preview(@NotNull Long userId,
                               @NotNull OrderSource source,
                               @Nullable List<ItemInput> items,
                               @NotNull Long addressId,
                               @NotNull String currency,
                               @Nullable String discountCode,
                               @Nullable String buyerRemark,
                               @Nullable String locale);

    /**
     * 创建订单并预占库存
     *
     * @param userId       当前用户 ID
     * @param source       下单来源
     * @param items        直接下单条目 (source=DIRECT 时必填)
     * @param addressId    收货地址 ID
     * @param currency     币种
     * @param discountCode 折扣码 (可为空)
     * @param buyerRemark  买家备注 (可为空)
     * @param locale       展示语言 (可为空)
     * @return 已创建的订单聚合 (含明细)
     */
    @NotNull
    Order create(@NotNull Long userId,
                 @NotNull OrderSource source,
                 @Nullable List<ItemInput> items,
                 @NotNull Long addressId,
                 @NotNull String currency,
                 @Nullable String discountCode,
                 @Nullable String buyerRemark,
                 @Nullable String locale);

    /**
     * 列出当前用户订单摘要
     *
     * @param userId      当前用户 ID
     * @param pageQuery   分页请求
     * @param status      状态过滤 (可为空)
     * @param createdFrom 创建时间起 (含, 可为空)
     * @param createdTo   创建时间止 (含, 可为空)
     * @return 分页结果
     */
    @NotNull
    PageResult<OrderSummaryRow> listMyOrders(@NotNull Long userId, @NotNull PageQuery pageQuery,
                                             @Nullable OrderStatus status,
                                             @Nullable LocalDateTime createdFrom,
                                             @Nullable LocalDateTime createdTo);

    /**
     * 获取当前用户订单详情
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @return 订单聚合 (含明细), 不存在则抛错或返回空由上层决定
     */
    @NotNull
    Optional<Order> getMyOrder(@NotNull Long userId, @NotNull OrderNo orderNo);

    /**
     * 取消订单 (用户侧)
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @param reason  取消原因
     * @return 取消后的订单聚合
     */
    @NotNull
    Order cancel(@NotNull Long userId, @NotNull OrderNo orderNo, @NotNull String reason);

    /**
     * 修改订单收货地址 (仅一次)
     *
     * @param userId       当前用户 ID
     * @param orderNo      订单号
     * @param newAddressId 新地址 ID
     * @param note         修改备注 (可为空)
     * @return 修改后的订单聚合
     */
    @NotNull
    Order changeAddress(@NotNull Long userId, @NotNull OrderNo orderNo, @NotNull Long newAddressId, @Nullable String note);

    /**
     * 申请订单退款 (整单)
     *
     * @param userId      当前用户 ID
     * @param orderNo     订单号
     * @param reasonCode  原因码
     * @param reasonText  原因补充 (可为空)
     * @param attachments 附件 URL 列表 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order requestRefund(@NotNull Long userId, @NotNull OrderNo orderNo,
                        @NotNull OrderRefundReasonCode reasonCode,
                        @Nullable String reasonText,
                        @Nullable List<String> attachments);
}

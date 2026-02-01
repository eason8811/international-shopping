package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧确认退款命令 (支持部分退款 + refund_item 明细)
 *
 * <p>说明:</p>
 * <ul>
 *     <li>该命令仅描述 "本次退款" 要退款的金额拆分与明细, 实际退款结果由网关回调/轮询推进</li>
 *     <li>当 items 为空且 amounts 为空时, 代表整单退款 (由后端按订单支付金额决定)</li>
 * </ul>
 *
 * @param itemsAmountMinor    商品总金额, 以最小货币单位表示, 可以为 <code>null</code>
 * @param shippingAmountMinor 运费金额, 以最小货币单位表示, 可以为 <code>null</code>
 * @param items               退款项列表, 每个元素都是一个 {@link RefundItem} 对象, 列表不能为空
 */
public record ConfirmRefundCommand(@Nullable Long itemsAmountMinor,
                                   @Nullable Long shippingAmountMinor,
                                   @NotNull List<RefundItem> items) implements Verifiable {

    public ConfirmRefundCommand {
        items = List.copyOf(items);
    }

    /**
     * 验证当前退款命令的有效性
     */
    @Override
    public void validate() {
        if (itemsAmountMinor != null)
            require(itemsAmountMinor >= 0, "itemsAmountMinor 不能为负数");
        if (shippingAmountMinor != null)
            require(shippingAmountMinor >= 0, "shippingAmountMinor 不能为负数");
        requireNotNull(items, "items 不能为空");
        for (RefundItem item : items) {
            requireNotNull(item, "items 不能为空");
            item.validate();
        }
    }

    /**
     * 创建一个空的 <code>ConfirmRefundCommand</code> 对象 该对象表示没有指定任何退款金额和明细的情况
     *
     * @return 一个空的 <code>ConfirmRefundCommand</code> 实例, 其中所有金额字段为 <code>null</code>, 明细列表为空
     */
    public static @NotNull ConfirmRefundCommand empty() {
        return new ConfirmRefundCommand(null, null, List.of());
    }

    /**
     * 检查给定的 <code>ConfirmRefundCommand</code> 是否为空
     *
     * @param cmd 待检查的 <code>ConfirmRefundCommand</code> 对象, 可以为 <code>null</code>
     * @return 如果 <code>cmd</code> 符合上述空命令定义, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    public static boolean isEmpty(@Nullable ConfirmRefundCommand cmd) {
        if (cmd == null)
            return true;
        return cmd.itemsAmountMinor() == null && cmd.shippingAmountMinor() == null && cmd.items().isEmpty();
    }

    /**
     * 表示退款项的记录, 包含订单项 ID, 数量, 金额(以最小单位表示)和退款原因等信息
     *
     * <p>此类实现了 {@link Verifiable} 接口, 用于确保创建或更新时数据的有效性</p>
     *
     * @param orderItemId 订单项 ID
     * @param quantity    数量
     * @param amountMinor 价格 minor 形式
     * @param reason      退款理由
     */
    public record RefundItem(@NotNull Long orderItemId,
                             int quantity,
                             @Nullable Long amountMinor,
                             @Nullable String reason) implements Verifiable {
        /**
         * 验证当前退款条目记录的有效性
         */
        @Override
        public void validate() {
            requireNotNull(orderItemId, "refundItem.orderItemId 不能为空");
            require(quantity > 0, "refundItem.quantity 必须大于 0");
            if (amountMinor != null)
                require(amountMinor > 0, "refundItem.amountMinor 必须大于 0");
            if (reason != null)
                require(reason.length() <= 255, "refundItem.reason 长度不能超过 255 个字符");
        }
    }

    /**
     * 设置或更新退款命令中的退款项列表, 此方法会根据传入的退款项列表创建一个新的 <code>ConfirmRefundCommand</code> 实例
     *
     * @param items 一个包含 {@link RefundItem} 的列表, 列表不能为空且每个元素都必须是有效的 <code>RefundItem</code> 对象
     * @return 新的 <code>ConfirmRefundCommand</code> 实例, 其中包含了传入的退款项列表以及当前对象的其他属性
     */
    public @NotNull ConfirmRefundCommand withItems(@NotNull List<RefundItem> items) {
        List<RefundItem> normalized = new ArrayList<>(items);
        return new ConfirmRefundCommand(itemsAmountMinor, shippingAmountMinor, normalized);
    }
}


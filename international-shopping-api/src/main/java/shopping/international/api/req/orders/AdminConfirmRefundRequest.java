package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧确认退款请求体 (AdminConfirmRefundRequest)
 *
 * <p>用于管理侧确认退款时提交可选备注与可选 "部分退款明细" </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminConfirmRefundRequest implements Verifiable {
    /**
     * 备注信息 (可选, 最大长度 255)
     */
    @Nullable
    private String note;

    /**
     * 货品部分退款金额 (可选, major 字符串)
     *
     * <p>当 {@link #items} 提供时, 若此字段为空则可由各明细的 {@code amount} 之和推导</p>
     */
    @Nullable
    private String itemsAmount;

    /**
     * 运费部分退款金额 (可选, major 字符串)
     */
    @Nullable
    private String shippingAmount;

    /**
     * 退款明细 (可选)
     *
     * <p>用于支持部分退款/按订单明细拆分记录若未提供, 代表按整单退款(由后端按订单金额决定)</p>
     */
    @Nullable
    private List<RefundItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundItem implements Verifiable {
        /**
         * 订单明细 ID (order_item.id)
         */
        @Nullable
        private Long orderItemId;

        /**
         * 本次退款数量 (件)
         */
        @Nullable
        private Integer quantity;

        /**
         * 该明细对应的退款金额 (可选, major 字符串; 若不传可由后端按比例推导)
         */
        @Nullable
        private String amount;

        /**
         * 该明细退款原因备注 (可选, 最大长度 255)
         */
        @Nullable
        private String reason;

        @Override
        public void validate() {
            requireNotNull(orderItemId, "items.orderItemId 不能为空");
            requireNotNull(quantity, "items.quantity 不能为空");
            require(quantity > 0, "items.quantity 必须大于 0");
            amount = normalizeNullableField(amount, "items.amount 不能为空", AdminConfirmRefundRequest::isNonNegativeDecimal, "items.amount 数值不合法");
            reason = normalizeNullableField(reason, "items.reason 不能为空", s -> s.length() <= 255, "items.reason 长度不能超过 255 个字符");
        }
    }

    /**
     * 校验并规范化字段
     *
     * <p>若 {@code note} 非空, 则进行去首尾空白并校验最大长度</p>
     */
    @Override
    public void validate() {
        note = normalizeNullableField(note, "note 不能为空", s -> s.length() <= 255, "note 长度不能超过 255 个字符");
        itemsAmount = normalizeNullableField(itemsAmount, "itemsAmount 不能为空", AdminConfirmRefundRequest::isNonNegativeDecimal, "itemsAmount 数值不合法");
        shippingAmount = normalizeNullableField(shippingAmount, "shippingAmount 不能为空", AdminConfirmRefundRequest::isNonNegativeDecimal, "shippingAmount 数值不合法");

        if (items != null) {
            List<RefundItem> normalized = new ArrayList<>();
            for (RefundItem item : items) {
                requireNotNull(item, "items 不能为空");
                item.validate();
                normalized.add(item);
            }
            items = normalized;
        }
    }

    /**
     * 检查给定的字符串是否表示一个非负的小数
     *
     * <p>该方法尝试将传入的字符串解析为 {@link BigDecimal} 类型, 并检查其是否大于等于 0, 如果输入的字符串无法被正确解析成小数, 则返回 false</p>
     *
     * @param raw 待检查的原始字符串, 不允许为 null
     * @return 如果字符串代表一个非负小数则返回 true, 否则返回 false
     */
    private static boolean isNonNegativeDecimal(@NotNull String raw) {
        try {
            BigDecimal bd = new BigDecimal(raw.strip());
            return bd.compareTo(BigDecimal.ZERO) >= 0;
        } catch (Exception e) {
            return false;
        }
    }
}

package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 订单金额口径值对象
 *
 * <p>pay = total - discount + shipping</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class OrderAmountBreakdown implements Verifiable {
    /**
     * 商品总额 (未含运费/折扣)
     */
    @NotNull
    private Money totalAmount;
    /**
     * 折扣总额
     */
    @NotNull
    private Money discountAmount;
    /**
     * 运费
     */
    @NotNull
    private Money shippingAmount;
    /**
     * 应付金额 (= total - discount + shipping)
     */
    @NotNull
    private Money payAmount;

    /**
     * 由总额/折扣/运费推导应付金额, 并返回金额口径对象
     *
     * @param totalAmount    商品总额
     * @param discountAmount 折扣总额
     * @param shippingAmount 运费
     * @return 金额口径对象
     */
    public static OrderAmountBreakdown of(@NotNull Money totalAmount, @NotNull Money discountAmount, @NotNull Money shippingAmount) {
        requireNotNull(totalAmount, "totalAmount 不能为空");
        requireNotNull(discountAmount, "discountAmount 不能为空");
        requireNotNull(shippingAmount, "shippingAmount 不能为空");
        totalAmount.ensureSameCurrency(discountAmount);
        totalAmount.ensureSameCurrency(shippingAmount);
        Money pay = totalAmount.subtract(discountAmount).add(shippingAmount);
        return OrderAmountBreakdown.builder()
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .shippingAmount(shippingAmount)
                .payAmount(pay)
                .build();
    }

    /**
     * 校验金额口径与不变式
     *
     * <ul>
     *     <li>所有金额必填且币种一致</li>
     *     <li>折扣金额不得大于总额</li>
     *     <li>应付金额不得为负</li>
     *     <li>payAmount 必须等于 total - discount + shipping</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(totalAmount, "totalAmount 不能为空");
        requireNotNull(discountAmount, "discountAmount 不能为空");
        requireNotNull(shippingAmount, "shippingAmount 不能为空");
        requireNotNull(payAmount, "payAmount 不能为空");
        totalAmount.ensureSameCurrency(discountAmount);
        totalAmount.ensureSameCurrency(shippingAmount);
        totalAmount.ensureSameCurrency(payAmount);
        require(discountAmount.getAmount().compareTo(totalAmount.getAmount()) <= 0, "折扣金额不能大于总额");
        require(payAmount.getAmount().compareTo(BigDecimal.ZERO) >= 0, "应付金额不能为负数");
        Money expected = totalAmount.subtract(discountAmount).add(shippingAmount);
        require(expected.getAmount().compareTo(payAmount.getAmount()) == 0, "金额口径不一致");
    }
}

package shopping.international.domain.model.vo.payment;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.NoGenerator;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 退款单号值对象 (对应表 payment_refund.refund_no)
 *
 * <p>约定为 26 位字符串 (ULID/雪花等), 对外可见</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class RefundNo implements Verifiable {

    /**
     * 退款单号原始值
     */
    private String value;

    /**
     * 构造函数 (请使用 {@link #of(String)})
     *
     * @param value 退款单号
     */
    private RefundNo(String value) {
        this.value = value;
    }

    /**
     * 创建退款单号值对象
     *
     * @param value 退款单号字符串
     * @return {@link RefundNo}
     */
    public static @NotNull RefundNo of(@NotNull String value) {
        RefundNo no = new RefundNo(value);
        no.validate();
        return no;
    }

    /**
     * 生成一个新的退款单号
     *
     * <p>生成策略与 {@code orders.OrderNo#generate()} 一致: 48bit 时间戳 + 80bit 随机数, 编码为 26 位 Crockford Base32</p>
     *
     * @return 新生成的 {@link RefundNo}
     */
    public static @NotNull RefundNo generate() {
        return RefundNo.of(NoGenerator.generate());
    }

    /**
     * 校验退款单号合法性
     */
    @Override
    public void validate() {
        value = normalizeNotNullField(value, "refundNo 不能为空",
                s -> s.length() == 26, "refundNo 必须为 26 位");
    }
}

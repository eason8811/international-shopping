package shopping.international.domain.model.vo.payment;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 支付网关外部单号值对象 (如 PayPal Order ID) 
 *
 * <p>对应表字段: payment_order.external_id / orders.payment_external_id</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class PaymentExternalId implements Verifiable {

    /**
     * 外部单号原始值
     */
    private String value;

    /**
     * 构造函数 (请使用 {@link #of(String)}) 
     *
     * @param value 外部单号
     */
    private PaymentExternalId(String value) {
        this.value = value;
    }

    /**
     * 创建外部单号值对象
     *
     * @param value 外部单号字符串
     * @return {@link PaymentExternalId}
     */
    public static @NotNull PaymentExternalId of(@NotNull String value) {
        PaymentExternalId vo = new PaymentExternalId(value);
        vo.validate();
        return vo;
    }

    /**
     * 校验外部单号合法性
     */
    @Override
    public void validate() {
        value = normalizeNotNullField(value, "externalId 不能为空",
                s -> s.length() <= 128, "externalId 长度不能超过 128 个字符");
    }
}


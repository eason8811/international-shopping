package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧补发单元数据更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipPatchRequest implements Verifiable {
    /**
     * 币种代码
     */
    @Nullable
    private String currency;
    /**
     * 货品成本 (分)
     */
    @Nullable
    private String itemsCost;
    /**
     * 运费成本 (分)
     */
    @Nullable
    private String shippingCost;
    /**
     * 备注信息
     */
    @Nullable
    private String note;

    /**
     * 对补发单元数据更新参数进行校验与规范化
     */
    @Override
    public void validate() {
        currency = normalizeNullableField(currency, "currency 不能为空",
                value -> CURRENCY_PATTERN.matcher(value.strip().toUpperCase()).matches(),
                "currency 需为 3 位字母代码");
        if (currency != null)
            currency = currency.toUpperCase();

        if (itemsCost != null) {
            try {
                BigDecimal itemsCostNumber = new BigDecimal(itemsCost);
                require(itemsCostNumber.compareTo(BigDecimal.ZERO) >= 0, "itemsCost 必须大于等于 0");
            } catch (Exception e) {
                throw new IllegalParamException("itemsCost 格式不合法");
            }
        }
        if (shippingCost != null) {
            try {
                BigDecimal shippingCostNumber = new BigDecimal(shippingCost);
                require(shippingCostNumber.compareTo(BigDecimal.ZERO) >= 0, "shippingCost 必须大于等于 0");
            } catch (Exception e) {
                throw new IllegalParamException("shippingCost 格式不合法");
            }
        }

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        require(currency != null || itemsCost != null || shippingCost != null || note != null,
                "至少需要提供一个可更新字段");
    }
}

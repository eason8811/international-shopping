package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.types.utils.Verifiable;

import java.util.List;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧补发单创建请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipCreateRequest implements Verifiable {
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 补发原因编码
     */
    @Nullable
    private ReshipReasonCode reasonCode;
    /**
     * 币种代码
     */
    @Nullable
    private String currency;
    /**
     * 备注信息
     */
    @Nullable
    private String note;
    /**
     * 补发明细列表
     */
    @Nullable
    private List<AdminReshipCreateItemRequest> items;

    /**
     * 对补发单创建参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "orderId 不能为空");
        require(orderId >= 1, "orderId 必须大于等于 1");

        requireNotNull(reasonCode, "reasonCode 不能为空");

        if (currency != null)
            currency = normalizeNotNullField(currency, "currency 不能为空",
                    value -> CURRENCY_PATTERN.matcher(value.strip().toUpperCase(Locale.ROOT)).matches(),
                    "currency 需为 3 位字母代码").toUpperCase(Locale.ROOT);

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        requireNotNull(items, "items 不能为空");
        require(!items.isEmpty(), "items 不能为空数组");

        items = normalizeFieldList(items, i -> {
            requireNotNull(i, "items 元素不能为空");
            i.validate();
        });
    }
}

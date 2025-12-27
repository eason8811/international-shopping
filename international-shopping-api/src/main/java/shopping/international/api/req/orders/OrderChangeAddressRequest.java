package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 修改订单收货地址请求体 (OrderChangeAddressRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangeAddressRequest implements Verifiable {
    /**
     * 新的用户收货地址 ID
     */
    private Long addressId;
    /**
     * 修改原因备注 (可选, 最大长度 255)
     */
    @Nullable
    private String note;

    /**
     * 校验并规范化字段
     */
    @Override
    public void validate() {
        requireNotNull(addressId, "addressId 不能为空");
        require(addressId >= 1, "addressId 必须大于等于 1");
        note = normalizeNullableField(note, "note 不能为空", s -> s.length() <= 255, "note 长度不能超过 255 个字符");
    }
}


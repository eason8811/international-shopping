package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderSource;
import shopping.international.types.utils.Verifiable;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 创建订单请求体 (OrderCreateRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest implements Verifiable {
    /**
     * 创建来源
     */
    private OrderSource source;
    /**
     * 购买条目列表 (source=DIRECT 时必填)
     */
    @Nullable
    private List<OrderPreviewItemInputRequest> items;
    /**
     * 收货地址 ID
     */
    private Long addressId;
    /**
     * 结算币种 (3 位字母代码, 如 USD)
     */
    private String currency;
    /**
     * 折扣码 (可选, 最小长度 6, 最大长度 32)
     */
    @Nullable
    private String discountCode;
    /**
     * 买家备注 (可选, 最大长度 500)
     */
    @Nullable
    private String buyerRemark;
    /**
     * 语言代码 (可选, 如 en-US)
     */
    @Nullable
    private String locale;

    /**
     * 校验并规范化字段
     */
    @Override
    public void validate() {
        requireNotNull(source, "source 不能为空");
        requireNotNull(addressId, "addressId 不能为空");
        require(addressId >= 1, "addressId 必须大于等于 1");

        currency = normalizeCurrency(currency);
        requireNotNull(currency, "currency 不能为空");

        discountCode = normalizeNullableField(discountCode, "discountCode 不能为空",
                s -> s.length() >= 6 && s.length() <= 32, "discountCode 长度需在 6~32 之间");
        buyerRemark = normalizeNullableField(buyerRemark, "buyerRemark 不能为空",
                s -> s.length() <= 500, "buyerRemark 长度不能超过 500 个字符");
        locale = normalizeLocale(locale);

        if (source == OrderSource.DIRECT) {
            requireNotNull(items, "source=DIRECT 时 items 不能为空");
            require(!items.isEmpty(), "source=DIRECT 时 items 不能为空");
            items = normalizeFieldList(items);
        } else if (source == OrderSource.CART) {
            require(items == null || items.isEmpty(), "source=CART 时不需要 items");
            items = List.of();
        }
    }
}


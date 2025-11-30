package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 价格维护请求 ProductPriceUpsertRequest
 */
@Data
public class ProductPriceUpsertRequest {
    /**
     * 币种代码, 需符合 ISO 4217 格式
     */
    private String currency;
    /**
     * 标价
     */
    private BigDecimal listPrice;
    /**
     * 促销价
     */
    @Nullable
    private BigDecimal salePrice;
    /**
     * 价格是否生效
     */
    @Nullable
    private Boolean isActive;

    /**
     * 校验并规范化价格字段
     *
     * @throws IllegalParamException 当币种或价格不合法时抛出 IllegalParamException
     */
    public void validate() {
        requireNotBlank(currency, "价格币种不能为空");
        currency = currency.strip().toUpperCase();
        if (!Pattern.matches("^[A-Z]{3}$", currency))
            throw new IllegalParamException("价格币种格式不合法, 需要 3 位大写字母");

        if (listPrice == null)
            throw new IllegalParamException("标价不能为空");
        if (listPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalParamException("标价必须大于 0");

        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalParamException("促销价必须大于 0");
        if (salePrice != null && salePrice.compareTo(listPrice) > 0)
            throw new IllegalParamException("促销价不能高于标价");

        if (isActive == null)
            isActive = true;
    }
}

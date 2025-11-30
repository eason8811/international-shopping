package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 价格维护命令 ProductPriceUpsertCommand
 *
 * <p>用于表示 SKU 在单一币种下的标价与促销价, 便于领域层一次性校验</p>
 */
@Getter
@ToString
public class ProductPriceUpsertCommand {
    /**
     * 币种代码
     */
    private final String currency;
    /**
     * 标价
     */
    private final BigDecimal listPrice;
    /**
     * 促销价
     */
    @Nullable
    private final BigDecimal salePrice;
    /**
     * 是否生效
     */
    private final boolean active;

    private ProductPriceUpsertCommand(String currency, BigDecimal listPrice, @Nullable BigDecimal salePrice, boolean active) {
        this.currency = currency;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.active = active;
    }

    /**
     * 构建价格维护命令
     *
     * @param currency  币种代码
     * @param listPrice 标价
     * @param salePrice 促销价
     * @param active    是否生效
     * @return 价格维护命令
     * @throws IllegalParamException 当币种或价格不合法时抛出 IllegalParamException
     */
    public static ProductPriceUpsertCommand of(String currency,
                                               BigDecimal listPrice,
                                               @Nullable BigDecimal salePrice,
                                               boolean active) {
        requireNotBlank(currency, "价格币种不能为空");
        String normalizedCurrency = currency.strip().toUpperCase();
        if (!Pattern.matches("^[A-Z]{3}$", normalizedCurrency))
            throw new IllegalParamException("价格币种格式不合法");
        if (listPrice == null || listPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalParamException("标价必须大于 0");
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalParamException("促销价必须大于 0");
        if (salePrice != null && salePrice.compareTo(listPrice) > 0)
            throw new IllegalParamException("促销价不能高于标价");
        return new ProductPriceUpsertCommand(normalizedCurrency, listPrice, salePrice, active);
    }
}

package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * SKU 多币种定价值对象, 对应表 {@code product_price}.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "currency")
public class ProductPrice implements Verifiable {
    /**
     * 货币代码 (ISO 4217)
     */
    private final String currency;
    /**
     * 标价
     */
    private final BigDecimal listPrice;
    /**
     * 促销价, 可空
     */
    private final BigDecimal salePrice;
    /**
     * 是否可售用价
     */
    private final boolean active;

    /**
     * 构造函数
     *
     * @param currency 货币
     * @param listPrice 标价
     * @param salePrice 促销价
     * @param active 是否可售
     */
    private ProductPrice(String currency, BigDecimal listPrice, BigDecimal salePrice, boolean active) {
        this.currency = currency;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.active = active;
    }

    /**
     * 创建定价值对象
     *
     * @param currency  货币代码, 必填
     * @param listPrice 标价, 必须大于 0
     * @param salePrice 促销价, 可空且不得大于标价
     * @param active    是否可售
     * @return 规范化后的 {@link ProductPrice}
     */
    public static ProductPrice of(String currency, BigDecimal listPrice, BigDecimal salePrice, boolean active) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        requireNotNull(listPrice, "标价不能为空");
        require(listPrice.compareTo(BigDecimal.ZERO) > 0, "标价必须大于 0");
        if (salePrice != null) {
            require(salePrice.compareTo(BigDecimal.ZERO) > 0, "促销价必须大于 0");
            require(salePrice.compareTo(listPrice) <= 0, "促销价不能高于标价");
        }
        return new ProductPrice(normalizedCurrency, listPrice, salePrice, active);
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotNull(currency, "currency 不能为空");
        requireNotNull(listPrice, "标价不能为空");
    }
}

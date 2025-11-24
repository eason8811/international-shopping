package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 价格 (单币种)
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"currency", "listPrice", "salePrice"})
public class ProductPrice {
    /**
     * 币种
     */
    private final String currency;
    /**
     * 标价
     */
    private final BigDecimal listPrice;
    /**
     * 促销价
     */
    private final BigDecimal salePrice;
    /**
     * 是否可用
     */
    private final boolean active;

    private ProductPrice(String currency, BigDecimal listPrice, BigDecimal salePrice, boolean active) {
        this.currency = currency;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.active = active;
    }

    /**
     * 构建价格 VO
     *
     * @param currency  币种
     * @param listPrice 标价
     * @param salePrice 促销价
     * @param isActive  是否可用
     * @return 价格 VO
     */
    public static ProductPrice of(@NotNull String currency, @NotNull BigDecimal listPrice, BigDecimal salePrice, Boolean isActive) {
        requireNotBlank(currency, "价格币种不能为空");
        return new ProductPrice(currency, listPrice, salePrice, Boolean.TRUE.equals(isActive));
    }

    /**
     * 获取用于比较的实际价格 (优先促销价)
     *
     * @return 优先促销价，否则标价
     */
    public BigDecimal effectivePrice() {
        return salePrice != null ? salePrice : listPrice;
    }
}

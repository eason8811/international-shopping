package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品价格区间
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"currency", "listPriceMin", "listPriceMax", "salePriceMin", "salePriceMax"})
public class ProductPriceRange {
    /**
     * 币种
     */
    private final String currency;
    /**
     * 标价最小值
     */
    private final BigDecimal listPriceMin;
    /**
     * 标价最大值
     */
    private final BigDecimal listPriceMax;
    /**
     * 促销价最小值
     */
    private final BigDecimal salePriceMin;
    /**
     * 促销价最大值
     */
    private final BigDecimal salePriceMax;

    private ProductPriceRange(String currency, BigDecimal listPriceMin, BigDecimal listPriceMax,
                              BigDecimal salePriceMin, BigDecimal salePriceMax) {
        this.currency = currency;
        this.listPriceMin = listPriceMin;
        this.listPriceMax = listPriceMax;
        this.salePriceMin = salePriceMin;
        this.salePriceMax = salePriceMax;
    }

    /**
     * 构建价格区间
     *
     * @param currency     币种
     * @param listPriceMin 标价最小
     * @param listPriceMax 标价最大
     * @param salePriceMin 促销价最小
     * @param salePriceMax 促销价最大
     * @return 区间对象
     */
    public static ProductPriceRange of(@NotNull String currency,
                                       @NotNull BigDecimal listPriceMin,
                                       @NotNull BigDecimal listPriceMax,
                                       BigDecimal salePriceMin,
                                       BigDecimal salePriceMax) {
        requireNotBlank(currency, "价格区间币种不能为空");
        return new ProductPriceRange(currency, listPriceMin, listPriceMax, salePriceMin, salePriceMax);
    }
}

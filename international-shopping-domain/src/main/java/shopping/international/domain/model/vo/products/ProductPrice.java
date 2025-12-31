package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

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
     * 标价（最小货币单位）
     */
    private final long listPrice;
    /**
     * 促销价（最小货币单位）, 可空
     */
    @Nullable
    private final Long salePrice;
    /**
     * 是否可售用价
     */
    private final boolean active;

    /**
     * 构造函数
     *
     * @param currency  货币
     * @param listPrice 标价
     * @param salePrice 促销价
     * @param active    是否可售
     */
    private ProductPrice(String currency, long listPrice, @Nullable Long salePrice, boolean active) {
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
    public static ProductPrice of(String currency, long listPrice, @Nullable Long salePrice, boolean active) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        require(listPrice > 0, "标价必须大于 0");
        if (salePrice != null) {
            require(salePrice > 0, "促销价必须大于 0");
            require(salePrice <= listPrice, "促销价不能高于标价");
        }
        return new ProductPrice(normalizedCurrency, listPrice, salePrice, active);
    }

    /**
     * 有效价 (促销价优先)
     */
    public long effectivePrice() {
        return salePrice != null ? salePrice : listPrice;
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotNull(currency, "currency 不能为空");
        require(listPrice > 0, "标价必须大于 0");
        if (salePrice != null) {
            require(salePrice > 0, "促销价必须大于 0");
            require(salePrice <= listPrice, "促销价不能高于标价");
        }
        requireNotNull(active, "active 不能为空");
    }
}

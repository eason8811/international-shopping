package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductPriceSource;
import shopping.international.types.enums.FxRateProvider;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
     * 价格来源
     */
    private final ProductPriceSource source;
    /**
     * FX 派生基准币种 (通常 USD), source=FX_AUTO 时有效
     */
    @Nullable
    private final String derivedFrom;
    /**
     * FX 派生汇率 (1 derived_from = fx_rate currency)
     */
    @Nullable
    private final BigDecimal fxRate;
    /**
     * FX 汇率时间点/采样时间
     */
    @Nullable
    private final LocalDateTime fxAsOf;
    /**
     * FX 数据源
     */
    @Nullable
    private final FxRateProvider fxProvider;
    /**
     * 派生计算时间
     */
    @Nullable
    private final LocalDateTime computedAt;
    /**
     * 算法版本
     */
    private final int algoVer;
    /**
     * 加价/手续费 (bps)
     */
    private final int markupBps;

    /**
     * 构造函数
     *
     * @param currency  货币
     * @param listPrice 标价
     * @param salePrice 促销价
     * @param active    是否可售
     */
    private ProductPrice(String currency,
                         long listPrice,
                         @Nullable Long salePrice,
                         boolean active,
                         ProductPriceSource source,
                         @Nullable String derivedFrom,
                         @Nullable BigDecimal fxRate,
                         @Nullable LocalDateTime fxAsOf,
                         @Nullable FxRateProvider fxProvider,
                         @Nullable LocalDateTime computedAt,
                         int algoVer,
                         int markupBps) {
        this.currency = currency;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.active = active;
        this.source = source;
        this.derivedFrom = derivedFrom;
        this.fxRate = fxRate;
        this.fxAsOf = fxAsOf;
        this.fxProvider = fxProvider;
        this.computedAt = computedAt;
        this.algoVer = algoVer;
        this.markupBps = markupBps;
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
        return new ProductPrice(normalizedCurrency, listPrice, salePrice, active,
                ProductPriceSource.MANUAL,
                null, null, null, null, null,
                1, 0);
    }

    /**
     * 根据给定的参数创建一个通过自动汇率转换得到的产品价格对象。
     *
     * @param currency    货币代码, 必填
     * @param listPrice   标价, 必须大于 0
     * @param salePrice   促销价, 可为空且不得大于标价
     * @param active      是否可售
     * @param derivedFrom 汇率来源货币代码, 必填
     * @param fxRate      汇率, 必须大于 0
     * @param fxAsOf      汇率生效时间, 必填
     * @param fxProvider  汇率提供者, 必填
     * @param computedAt  计算时间, 必填
     * @param algoVer     算法版本号, 必须大于 0
     * @param markupBps   加成基点, 不得为负数
     * @return 规范化后的 {@link ProductPrice} 对象
     */
    public static @NotNull ProductPrice fxAuto(@NotNull String currency,
                                               long listPrice,
                                               @Nullable Long salePrice,
                                               boolean active,
                                               @NotNull String derivedFrom,
                                               @NotNull BigDecimal fxRate,
                                               @NotNull LocalDateTime fxAsOf,
                                               @NotNull FxRateProvider fxProvider,
                                               @NotNull LocalDateTime computedAt,
                                               int algoVer,
                                               int markupBps) {
        String normalizedCurrency = normalizeCurrency(currency);
        String normalizedDerivedFrom = normalizeCurrency(derivedFrom);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        requireNotNull(normalizedDerivedFrom, "derivedFrom 不能为空");
        require(!normalizedCurrency.equals(normalizedDerivedFrom), "currency 不能与 derivedFrom 相同");
        require(listPrice > 0, "标价必须大于 0");
        if (salePrice != null) {
            require(salePrice > 0, "促销价必须大于 0");
            require(salePrice <= listPrice, "促销价不能高于标价");
        }
        requireNotNull(fxRate, "fxRate 不能为空");
        require(fxRate.compareTo(BigDecimal.ZERO) > 0, "fxRate 必须大于 0");
        requireNotNull(fxAsOf, "fxAsOf 不能为空");
        requireNotNull(fxProvider, "fxProvider 不能为空");
        requireNotNull(computedAt, "computedAt 不能为空");
        require(algoVer > 0, "algoVer 必须大于 0");
        require(markupBps >= 0, "markupBps 不能为负数");
        return new ProductPrice(normalizedCurrency, listPrice, salePrice, active,
                ProductPriceSource.FX_AUTO,
                normalizedDerivedFrom,
                fxRate,
                fxAsOf,
                fxProvider,
                computedAt,
                algoVer,
                markupBps);
    }

    /**
     * 从给定参数重新构建 <code>ProductPrice</code> 对象并返回, 此方法会创建一个新的 <code>ProductPrice</code> 实例,
     * 并通过调用 <code>validate()</code> 方法来确保其有效性
     *
     * @param currency     货币代码, 必填
     * @param listPrice    标价, 必须大于 0
     * @param salePrice    促销价, 可为空且不得大于标价
     * @param active       是否可售
     * @param source       价格来源, 必填
     * @param derivedFrom  汇率来源货币代码, 可为空
     * @param fxRate       汇率, 可为空
     * @param fxAsOf       汇率生效时间, 可为空
     * @param fxProvider   汇率提供者, 可为空
     * @param computedAt   计算时间, 可为空
     * @param algoVer      算法版本号, 必须大于 0
     * @param markupBps    加成基点, 不得为负数
     * @return 规范化后的 <code>ProductPrice</code> 对象
     */
    public static @NotNull ProductPrice reconstitute(@NotNull String currency,
                                                     long listPrice,
                                                     @Nullable Long salePrice,
                                                     boolean active,
                                                     @NotNull ProductPriceSource source,
                                                     @Nullable String derivedFrom,
                                                     @Nullable BigDecimal fxRate,
                                                     @Nullable LocalDateTime fxAsOf,
                                                     @Nullable FxRateProvider fxProvider,
                                                     @Nullable LocalDateTime computedAt,
                                                     int algoVer,
                                                     int markupBps) {
        ProductPrice vo = new ProductPrice(currency, listPrice, salePrice, active,
                source, derivedFrom, fxRate, fxAsOf, fxProvider, computedAt, algoVer, markupBps);
        vo.validate();
        return vo;
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
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        require(listPrice > 0, "标价必须大于 0");
        if (salePrice != null) {
            require(salePrice > 0, "促销价必须大于 0");
            require(salePrice <= listPrice, "促销价不能高于标价");
        }
        requireNotNull(active, "active 不能为空");
        requireNotNull(source, "source 不能为空");
        require(algoVer > 0, "algoVer 必须大于 0");
        require(markupBps >= 0, "markupBps 不能为负数");

        if (source == ProductPriceSource.FX_AUTO) {
            String normalizedDerivedFrom = normalizeCurrency(derivedFrom);
            requireNotNull(normalizedDerivedFrom, "derivedFrom 不能为空");
            require(!normalizedCurrency.equalsIgnoreCase(normalizedDerivedFrom), "currency 不能与 derivedFrom 相同");
            requireNotNull(fxRate, "fxRate 不能为空");
            require(fxRate.compareTo(BigDecimal.ZERO) > 0, "fxRate 必须大于 0");
            requireNotNull(fxAsOf, "fxAsOf 不能为空");
            requireNotNull(fxProvider, "fxProvider 不能为空");
            requireNotNull(computedAt, "computedAt 不能为空");
        }
    }
}

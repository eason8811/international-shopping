package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.*;
import shopping.international.domain.model.vo.common.FxRateLatest;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.common.IFxRateService;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * SKU 领域服务实现
 *
 * <p>负责协调 SKU 聚合的创建、更新、价格与规格绑定维护以及库存调整, 并同步商品聚合的默认 SKU 与库存总数。</p>
 */
@Service
@RequiredArgsConstructor
public class SkuService implements ISkuService {

    /**
     * 全站默认基础货币
     */
    private static final String DEFAULT_BASE_CURRENCY = "USD";
    /**
     * 默认算法版本
     */
    private static final int DEFAULT_ALGO_VER = 1;
    /**
     * 默认 markup
     */
    private static final int DEFAULT_MARKUP_BPS = 0;
    /**
     * 允许的最大延迟时间
     */
    private static final Duration FX_MAX_AGE = Duration.ofHours(8);
    /**
     * 时钟
     */
    private final Clock clock = Clock.systemUTC();

    /**
     * SKU 仓储
     */
    private final ISkuRepository skuRepository;
    /**
     * 商品仓储
     */
    private final IProductRepository productRepository;
    /**
     * 币种仓储 (用于 enabled 币种集合)
     */
    private final ICurrencyRepository currencyRepository;
    /**
     * 货币配置服务
     */
    private final ICurrencyConfigService currencyConfigService;
    /**
     * 汇率服务
     */
    private final IFxRateService fxRateService;

    /**
     * 列出商品下的 SKU
     *
     * @param productId 商品 ID
     * @param status    状态过滤, 为空表示不过滤
     * @return SKU 聚合列表
     */
    @Override
    public @NotNull List<Sku> list(@NotNull Long productId, @Nullable SkuStatus status) {
        ensureProduct(productId);
        return skuRepository.listByProductId(productId, status);
    }

    /**
     * 创建 SKU
     *
     * @param productId 所属商品 ID
     * @param skuCode   SKU 编码
     * @param stock     库存
     * @param weight    重量
     * @param status    状态
     * @param isDefault 是否默认
     * @param barcode   条码
     * @param prices    价格列表
     * @param specs     规格绑定
     * @param images    图库
     * @return 新建的聚合
     */
    @Override
    public @NotNull Sku create(@NotNull Long productId, @Nullable String skuCode, @NotNull Integer stock,
                               @Nullable BigDecimal weight, @NotNull SkuStatus status, boolean isDefault,
                               @Nullable String barcode, @NotNull List<ProductPrice> prices,
                               @NotNull List<SkuSpecRelation> specs, @NotNull List<ProductImage> images) {
        Product product = ensureProduct(productId);
        if (product.getStatus() == ProductStatus.ON_SALE && status == SkuStatus.ENABLED)
            throw new ConflictException("已上架的商品, 无法创建状态为 '启用' 的 SKU");
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        if (product.getSkuType() == SkuType.SINGLE && !skuList.isEmpty())
            throw new ConflictException("单规格商品, 已存在 SKU, 无法再添加 SKU");
        ensureSkuSpecRelationValidate(product, skuList, null, specs);
        ensureHasBaseCurrencyPrice(prices, DEFAULT_BASE_CURRENCY);
        List<ProductPrice> fullPrices = deriveMissingFxAutoPrices(prices, List.of(), DEFAULT_BASE_CURRENCY);
        Sku sku = Sku.create(productId, skuCode, stock, weight, status, isDefault, barcode, fullPrices, specs, images);
        Sku saved = skuRepository.save(sku);
        if (isDefault)
            skuRepository.markDefault(productId, saved.getId());
        refreshProductStock(productId);
        return saved;
    }

    /**
     * 增量更新 SKU 基础字段
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param skuCode   新编码, 可空
     * @param stock     新库存, 可空
     * @param weight    新重量, 可空
     * @param status    新状态, 可空
     * @param isDefault 默认标记, 可空
     * @param barcode   条码, 可空
     * @param images    新图库, 可空表示不改
     * @return 更新后的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Sku updateBasic(@NotNull Long productId, @NotNull Long skuId, @Nullable String skuCode,
                                    @Nullable Integer stock, @Nullable BigDecimal weight, @Nullable SkuStatus status,
                                    @Nullable Boolean isDefault, @Nullable String barcode, @Nullable List<ProductImage> images) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        if (product.getStatus() == ProductStatus.ON_SALE && sku.getStatus() == SkuStatus.ENABLED && (status == null || status == SkuStatus.ENABLED))
            throw new ConflictException("已上架的 SKU, 且目标状态为 '启用' 无法修改信息");
        sku.updateBasic(skuCode, weight, status, isDefault, barcode);
        ensureSkuSpecRelationValidate(product, skuList, skuId, sku.getSpecs());
        if (stock != null)
            sku.adjustStock(StockAdjustMode.SET, stock);
        if (images != null)
            sku.replaceImages(images);

        Sku updated = skuRepository.updateBasic(sku, images != null);
        if (stock != null)
            refreshProductStock(productId);

        boolean hasEnabledSkuAfter = skuRepository.existsByProductIdAndStatus(productId, SkuStatus.ENABLED);
        boolean defaultChanged = product.onSkuUpdated(sku, hasEnabledSkuAfter);

        if (defaultChanged)
            skuRepository.markDefault(productId, product.getDefaultSkuId());
        return updated;
    }

    /**
     * 增量 upsert 规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specs     规格绑定列表
     * @return 受影响的规格 ID
     */
    @Override
    public @NotNull List<Long> upsertSpecs(@NotNull Long productId, @NotNull Long skuId, @NotNull List<SkuSpecRelation> specs) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        if (product.getStatus() == ProductStatus.ON_SALE && sku.getStatus() == SkuStatus.ENABLED)
            throw new ConflictException("已上架的 SKU, 无法修改规格绑定");
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        if (specs.isEmpty())
            return List.of();

        sku.patchSpecSelection(specs);
        List<SkuSpecRelation> newSpecRelationList = sku.getSpecs();
        ensureSkuSpecRelationValidate(product, skuList, skuId, newSpecRelationList);

        return skuRepository.upsertSpecs(skuId, sku.getSpecs());
    }

    /**
     * 解除规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteSpec(@NotNull Long productId, @NotNull Long skuId, @NotNull Long specId) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        if (product.getStatus() == ProductStatus.ON_SALE && sku.getStatus() == SkuStatus.ENABLED)
            throw new ConflictException("已上架的 SKU, 无法删除规格绑定");
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        sku.removeSpecSelection(specId);
        if (sku.getStatus() == SkuStatus.ENABLED)
            ensureSkuSpecRelationValidate(product, skuList, skuId, sku.getSpecs());
        return skuRepository.deleteSpec(skuId, specId);
    }

    /**
     * 增量 upsert 价格
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param prices    价格列表
     * @return 受影响的币种
     */
    @Override
    public @NotNull List<String> upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPrice> prices) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        if (product.getStatus() == ProductStatus.ON_SALE && sku.getStatus() == SkuStatus.ENABLED)
            throw new ConflictException("已上架的 SKU, 无法修改价格");
        if (prices.isEmpty())
            return List.of();

        ensureHasBaseCurrencyPrice(prices, DEFAULT_BASE_CURRENCY);
        List<ProductPrice> fullPrices = deriveMissingFxAutoPrices(prices, sku.getPrices(), DEFAULT_BASE_CURRENCY);
        sku.patchPrice(fullPrices);
        skuRepository.upsertPrices(skuId, sku.getPrices());
        return fullPrices.stream().map(ProductPrice::getCurrency).distinct().toList();
    }

    /**
     * 重新计算指定 SKU 的外汇自动价格或缺失币种的价格
     *
     * <p>不会覆盖已有 MANUAL 币种；基于 USD 基准价 + latest 汇率派生其余币种</p>
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @return 受影响的币种列表
     */
    @Override
    public @NotNull List<String> recomputeFxPrices(@NotNull Long productId, @NotNull Long skuId) {
        ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        ProductPrice usd = sku.getPrices().stream()
                .filter(p -> DEFAULT_BASE_CURRENCY.equalsIgnoreCase(p.getCurrency()))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("SKU 缺少全站默认币种 " + DEFAULT_BASE_CURRENCY + " 定价"));

        // 重算只依赖 USD 基准价, 并确保 USD 为 MANUAL (清理可能存在的 FX 元数据)
        ProductPrice usdManual = ProductPrice.of(DEFAULT_BASE_CURRENCY, usd.getListPrice(), usd.getSalePrice(), usd.isActive());

        List<ProductPrice> fullPrices = deriveMissingFxAutoPrices(List.of(usdManual), sku.getPrices(), DEFAULT_BASE_CURRENCY);
        sku.patchPrice(fullPrices);
        skuRepository.upsertPrices(skuId, sku.getPrices());
        return fullPrices.stream().map(ProductPrice::getCurrency).distinct().toList();
    }

    /**
     * 重新计算指定商品下所有 SKU 的外汇自动价格或缺失币种的价格
     *
     * <p>此方法不会覆盖已设定为 MANUAL 模式的币种价格；它基于 USD 基准价和最新汇率派生其余币种的价格</p>
     *
     * @param productId 商品 ID
     * @return 受影响的 SKU 数量, 表示有多少个 SKU 的价格被重新计算了
     */
    @Override
    public int recomputeFxPricesByProduct(@NotNull Long productId) {
        ensureProduct(productId);
        List<Sku> skus = skuRepository.listByProductId(productId, null);
        if (skus.isEmpty())
            return 0;
        int processed = 0;
        for (Sku sku : skus) {
            if (sku == null || sku.getId() == null)
                continue;
            try {
                recomputeFxPrices(productId, sku.getId());
                processed++;
            } catch (Exception ignore) {
                // 跳过缺失 USD 定价等不满足派生条件的 SKU
            }
        }
        return processed;
    }

    /**
     * 全量重算 FX_AUTO / 缺失币种价格
     *
     * @param batchSize 商品分页大小
     * @return 处理的 SKU 数量
     */
    @Override
    public int recomputeFxPricesAll(int batchSize) {
        int size = Math.max(1, Math.min(batchSize, 500));
        int offset = 0;
        int processedSkus = 0;
        while (true) {
            List<Product> products = productRepository.list(null, null, null, null, null, false, offset, size);
            if (products.isEmpty())
                break;
            for (Product p : products) {
                if (p == null || p.getId() == null)
                    continue;
                processedSkus += recomputeFxPricesByProduct(p.getId());
            }
            offset += products.size();
            if (products.size() < size)
                break;
        }
        return processedSkus;
    }

    /**
     * 将指定 SKU 的特定币种价格模式切换为 MANUAL 模式
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param currency  要切换到 MANUAL 模式的币种代码
     * @return 受影响的币种列表, 包含了成功切换为 MANUAL 模式的币种
     */
    @Override
    public @NotNull List<String> switchPriceToManual(@NotNull Long productId, @NotNull Long skuId, @NotNull String currency) {
        ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        ProductPrice existed = sku.getPrices().stream()
                .filter(p -> p != null && p.getCurrency() != null && p.getCurrency().equalsIgnoreCase(currency))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("SKU 未配置该币种价格: " + currency));
        ProductPrice manual = ProductPrice.of(existed.getCurrency(), existed.getListPrice(), existed.getSalePrice(), existed.isActive());
        sku.patchPrice(List.of(manual));
        skuRepository.upsertPrices(skuId, sku.getPrices());
        return List.of(manual.getCurrency());
    }

    /**
     * 将指定 SKU 的特定币种价格模式切换为 FX_AUTO 模式
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param currency  要切换到 FX_AUTO 模式的币种代码
     * @return 受影响的币种列表, 包含了成功切换为 FX_AUTO 模式的币种
     */
    @Override
    public @NotNull List<String> switchPriceToFxAuto(@NotNull Long productId, @NotNull Long skuId, @NotNull String currency) {
        if (DEFAULT_BASE_CURRENCY.equalsIgnoreCase(currency))
            throw new IllegalParamException("无法将全站默认币种 " + DEFAULT_BASE_CURRENCY + " 设为 FX_AUTO 模式, 该币种必须手动设置");
        ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        ProductPrice defaultCurrencyPrice = sku.getPrices().stream()
                .filter(p -> p != null && p.getCurrency() != null && p.getCurrency().equalsIgnoreCase(DEFAULT_BASE_CURRENCY))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("SKU 未配置全站默认币种价格: " + DEFAULT_BASE_CURRENCY));
        ProductPrice existed = sku.getPrices().stream()
                .filter(p -> p != null && p.getCurrency() != null && p.getCurrency().equalsIgnoreCase(currency))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("SKU 未配置该币种价格: " + currency));
        FxRateLatest fxRateLatest = fxRateService.getLatest(DEFAULT_BASE_CURRENCY, currency);
        requireNotNull(fxRateLatest, "汇率 '" + DEFAULT_BASE_CURRENCY + "' -> '" + currency + "' 不存在或已过期, 无法自动计算价格");

        CurrencyConfig existedQuoteCfg = currencyConfigService.get(currency);
        CurrencyConfig defaultBaseCfg = currencyConfigService.get(DEFAULT_BASE_CURRENCY);
        long listMinor = convertMinor(defaultBaseCfg, existedQuoteCfg, defaultCurrencyPrice.getListPrice(), fxRateLatest.rate(), DEFAULT_MARKUP_BPS);
        Long saleMinor = null;
        if (defaultCurrencyPrice.getSalePrice() != null) {
            long tmp = convertMinor(defaultBaseCfg, existedQuoteCfg, defaultCurrencyPrice.getSalePrice(), fxRateLatest.rate(), DEFAULT_MARKUP_BPS);
            if (tmp > 0)
                saleMinor = tmp;
        }
        if (saleMinor != null && saleMinor > listMinor)
            saleMinor = listMinor;

        ProductPrice fxAutoPrice = ProductPrice.fxAuto(
                existed.getCurrency(),
                listMinor,
                saleMinor,
                existed.isActive(),
                DEFAULT_BASE_CURRENCY,
                fxRateLatest.rate(),
                fxRateLatest.asOf(),
                fxRateLatest.provider(),
                LocalDateTime.now(clock),
                DEFAULT_ALGO_VER,
                DEFAULT_MARKUP_BPS);
        sku.patchPrice(List.of(fxAutoPrice));
        skuRepository.upsertPrices(skuId, sku.getPrices());
        return List.of(fxAutoPrice.getCurrency());
    }

    /**
     * 确保给定的价格列表中至少包含一个以全站默认币种计价的价格
     *
     * @param prices       价格列表, 必须不为空
     * @param baseCurrency 全站默认币种代码, 必须不为空
     * @throws IllegalArgumentException 如果价格列表中没有任何一个价格是以 {@code baseCurrency} 为币种的
     */
    private static void ensureHasBaseCurrencyPrice(@NotNull List<ProductPrice> prices, @NotNull String baseCurrency) {
        require(prices.stream().filter(Objects::nonNull).anyMatch(p -> baseCurrency.equalsIgnoreCase(p.getCurrency())),
                "价格必须包含全站默认币种 " + baseCurrency);
    }

    /**
     * 写入时派生价格并落库:
     * <ul>
     *      <li>未传入的币种: 若该币种当前不是 MANUAL，则使用 base(USD) + latest 汇率派生</li>
     *      <li>传入的非 base 币种: 视为人工调整 (MANUAL)，不派生该币种</li>
     * </ul>
     * <p>当汇率缺失/过期(>8h)时，不生成派生价格，读取侧应回退 USD</p>
     *
     * @param requested    请求中的产品价格列表, 包含可能需要推导出新价格的条目
     * @param existing     已存在的产品价格列表, 用于参考哪些价格已经设定且为手动调整
     * @param baseCurrency 基础货币代码, 作为汇率换算的基础
     * @return 返回一个完整的包含所有原始请求价格及推导出的新价格的列表
     * @throws IllegalParamException 如果提供的价格列表中不包含全站默认币种, 则抛出异常
     */
    private @NotNull List<ProductPrice> deriveMissingFxAutoPrices(@NotNull List<ProductPrice> requested,
                                                                  @NotNull List<ProductPrice> existing,
                                                                  @NotNull String baseCurrency) {
        // 获取 currency -> Price 映射
        Map<String, ProductPrice> priceByCurrencyMap = requested.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(p -> p.getCurrency().toUpperCase(), Function.identity(), (a, b) -> b));

        ProductPrice basePrice = priceByCurrencyMap.get(baseCurrency);
        if (basePrice == null)
            throw new IllegalParamException("价格必须包含全站默认币种 " + baseCurrency);

        Map<String, ProductPrice> existingByCurrency = existing.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(p -> p.getCurrency().toUpperCase(), Function.identity(), (a, b) -> b));

        Set<String> quoteCodes = currencyRepository.listEnabledCodes().stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(String::toUpperCase)
                .filter(c -> !c.isBlank())
                .filter(c -> !baseCurrency.equalsIgnoreCase(c))
                .collect(Collectors.toSet());

        if (quoteCodes.isEmpty())
            return requested;

        Map<String, FxRateLatest> latestMap = fxRateService.getLatestByQuotes(baseCurrency, quoteCodes);

        CurrencyConfig baseCfg = currencyConfigService.get(baseCurrency);
        LocalDateTime computedAt = LocalDateTime.now(clock);

        List<ProductPrice> derived = new ArrayList<>();
        for (String quote : quoteCodes) {
            if (quote == null || quote.isBlank())
                continue;
            // 传入视为人工调整
            if (priceByCurrencyMap.containsKey(quote))
                continue;
            // 已有且为 MANUAL 则保持不变
            ProductPrice existed = existingByCurrency.get(quote);
            if (existed != null && existed.getSource() == ProductPriceSource.MANUAL)
                continue;

            FxRateLatest latest = latestMap.get(quote);
            if (latest == null || !latest.isFresh(clock, FX_MAX_AGE))
                continue;

            CurrencyConfig quoteCfg = currencyConfigService.get(quote);
            long listMinor = convertMinor(baseCfg, quoteCfg, basePrice.getListPrice(), latest.rate(), DEFAULT_MARKUP_BPS);
            if (listMinor <= 0)
                continue;

            Long saleMinor = null;
            if (basePrice.getSalePrice() != null) {
                long tmp = convertMinor(baseCfg, quoteCfg, basePrice.getSalePrice(), latest.rate(), DEFAULT_MARKUP_BPS);
                if (tmp > 0)
                    saleMinor = tmp;
            }
            if (saleMinor != null && saleMinor > listMinor)
                saleMinor = listMinor;

            derived.add(ProductPrice.fxAuto(
                    quote,
                    listMinor,
                    saleMinor,
                    basePrice.isActive(),
                    baseCurrency,
                    latest.rate(),
                    latest.asOf(),
                    latest.provider(),
                    computedAt,
                    DEFAULT_ALGO_VER,
                    DEFAULT_MARKUP_BPS
            ));
        }

        if (derived.isEmpty())
            return requested;

        List<ProductPrice> full = new ArrayList<>(requested);
        full.addAll(derived);
        return full;
    }

    /**
     * 将基础货币的最小单位金额转换为目标货币的最小单位金额, 并根据给定的加成基点进行调整
     * baseMinor(USD) -> quoteMinor
     * <p>markup_bps 在换算前应用，最终舍入由 quoteCfg.roundingMode + minorUnit 决定</p>
     *
     * @param baseCfg   基础货币配置信息
     * @param quoteCfg  目标货币配置信息
     * @param baseMinor 基础货币的最小单位金额
     * @param rate      汇率, 用于从基础货币转换到目标货币
     * @param markupBps 加成基点, 用于计算额外的成本或利润, 以基点为单位(1基点=0.0001)
     * @return 转换后并经过加成调整的目标货币的最小单位金额
     */
    private static long convertMinor(@NotNull CurrencyConfig baseCfg,
                                     @NotNull CurrencyConfig quoteCfg,
                                     long baseMinor,
                                     @NotNull java.math.BigDecimal rate,
                                     int markupBps) {
        require(baseMinor >= 0, "金额不能为负数");
        require(markupBps >= 0, "markup_bps 不能为负数");
        BigDecimal baseMajor = baseCfg.toMajor(baseMinor);
        BigDecimal marked = markupBps == 0
                ? baseMajor
                : baseMajor.multiply(BigDecimal.valueOf(10000L + markupBps))
                .divide(BigDecimal.valueOf(10000L), 18, RoundingMode.HALF_UP);
        BigDecimal quoteMajor = marked.multiply(rate);
        return quoteCfg.toMinorRounded(quoteMajor);
    }

    /**
     * 调整库存
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param mode      调整模式
     * @param quantity  数量
     * @return 调整后的库存
     */
    @Override
    public int adjustStock(@NotNull Long productId, @NotNull Long skuId,
                           @NotNull StockAdjustMode mode, int quantity) {
        Sku sku = ensureSku(productId, skuId);
        sku.adjustStock(mode, quantity);
        int stock = skuRepository.updateStock(skuId, sku.getStock());
        refreshProductStock(productId);
        return stock;
    }

    /**
     * 删除指定商品下的 SKU
     *
     * @param productId 所属商品 ID
     * @param skuId     要删除的 SKU ID
     * @return 如果删除成功返回 <code>true</code>, 否则返回 <code>false</code>
     */
    @Override
    public boolean delete(Long productId, Long skuId) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        if (product.getStatus() == ProductStatus.ON_SALE && sku.getStatus() == SkuStatus.ENABLED)
            throw new ConflictException("已上架的 SKU, 无法删除");
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        Map<Long, SkuStatus> statusBySkuIdMap = skuList.stream().collect(Collectors.toMap(Sku::getId, Sku::getStatus));
        sku.updateBasic(null, null, SkuStatus.DISABLED, null, null);
        statusBySkuIdMap.put(skuId, SkuStatus.DISABLED);
        boolean hasEnabledSkuAfter = statusBySkuIdMap.values().stream().anyMatch(status -> status == SkuStatus.ENABLED);
        boolean defaultChanged = product.onSkuUpdated(sku, hasEnabledSkuAfter);
        if (defaultChanged)
            skuRepository.markDefault(productId, product.getDefaultSkuId());

        boolean delete = skuRepository.delete(productId, skuId);
        refreshProductStock(productId);
        return delete;
    }

    /**
     * 校验商品是否存在且未删除
     *
     * @param productId 商品 ID
     * @return 商品聚合
     */
    private Product ensureProduct(@NotNull Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalParamException("商品不存在"));
        if (product.getStatus() == ProductStatus.DELETED)
            throw new IllegalParamException("商品已删除");
        return product;
    }

    /**
     * 校验 SKU 是否存在且归属指定商品
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @return SKU 聚合
     */
    private Sku ensureSku(@NotNull Long productId, @NotNull Long skuId) {
        return skuRepository.findById(productId, skuId)
                .orElseThrow(() -> new IllegalParamException("SKU 不存在"));
    }

    /**
     * 确保新的 SKU 选择的规格值组合不与同 SPU 下的其他 SKU 重复
     *
     * @param product             产品
     * @param skuList             产品下的 SKU 列表
     * @param excludeSkuId        SKU ID, 可空 (不为空则排除这一 SKU)
     * @param newSpecRelationList 新规格绑定列表
     */
    private void ensureSkuSpecRelationValidate(@NotNull Product product, @NotNull List<Sku> skuList, @Nullable Long excludeSkuId, @NotNull List<SkuSpecRelation> newSpecRelationList) {
        Map<Long, List<ProductSpecValue>> specValueIdBySpecIdMap = product.getSpecs().stream()
                .map(ProductSpec::getValues)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(ProductSpecValue::getSpecId));
        for (SkuSpecRelation skuSpecRelation : newSpecRelationList) {
            require(specValueIdBySpecIdMap.containsKey(skuSpecRelation.getSpecId()), "规格 ID '" + skuSpecRelation.getSpecId() + "' 不存在");
            require(
                    specValueIdBySpecIdMap.get(skuSpecRelation.getSpecId()).stream()
                            .map(ProductSpecValue::getId)
                            .toList()
                            .contains(skuSpecRelation.getValueId()),
                    "规格值 ID '" + skuSpecRelation.getValueId() + "' 不存在"
            );
        }
        // 获取本 SPU 下所有已存在的 SKU 选择的规格值组合
        List<List<Long>> specValueGroupList = new ArrayList<>();
        for (Sku s : skuList) {
            if (excludeSkuId != null && excludeSkuId.equals(s.getId()))
                continue;
            List<Long> valueList = s.getSpecs().stream()
                    .map(SkuSpecRelation::getValueId)
                    .toList();
            specValueGroupList.add(valueList);
        }
        // 新的 SKU 选择的规格值组合
        List<Long> newSpecRelationValueIdList = newSpecRelationList.stream()
                .map(SkuSpecRelation::getValueId)
                .toList();
        // SPU 下必选的规格 ID 列表
        List<ProductSpec> requiredSpecList = product.getSpecs().stream()
                .filter(ProductSpec::isRequired)
                .toList();
        // 获取 Spec ID -> List< Spec Value > 映射
        Map<Long, ProductSpecValue> specValueByValueIdMap = product.getSpecs().stream()
                .map(ProductSpec::getValues)
                .flatMap(List::stream)
                .collect(Collectors.toMap(ProductSpecValue::getId, Function.identity()));
        // 如果 SPU 必选规格数量大于 SKU 选择规格数量, 则找出第一个缺失的必填规格名称, 抛出异常
        if (requiredSpecList.size() > newSpecRelationValueIdList.size())
            requiredSpecList.stream()
                    .filter(spec ->
                            !newSpecRelationList.stream().map(SkuSpecRelation::getSpecId).toList().contains(spec.getId())
                    )
                    .findFirst()
                    .ifPresent(spec -> {
                        throw new ConflictException("规格 '" + spec.getSpecName() + "' 必填");
                    });
        // 遍历有已存在的 SKU 选择的规格值组合, 如果有与新的选择的规格值组合相同的, 则获取这些组合的名称, 然后抛出异常
        for (List<Long> specValueIdList : specValueGroupList)
            if (specValueIdList.equals(newSpecRelationValueIdList)) {
                String existingSpecValueMsg = specValueIdList.stream()
                        .map(specValueByValueIdMap::get)
                        .map(ProductSpecValue::getValueName)
                        .collect(Collectors.joining(", "));
                throw new ConflictException("规格组合: [ " + existingSpecValueMsg + " ] 已被占用");
            }
    }

    /**
     * 刷新商品聚合库存
     *
     * @param productId 商品 ID
     */
    private void refreshProductStock(@NotNull Long productId) {
        int total = skuRepository.sumStockByProduct(productId);
        productRepository.updateStockTotal(productId, total);
    }
}

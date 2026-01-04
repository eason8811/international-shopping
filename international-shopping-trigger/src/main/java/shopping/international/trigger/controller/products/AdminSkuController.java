package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.*;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端 SKU 接口
 *
 * <p>覆盖 SKU 列表、创建、基础字段更新、规格绑定、价格维护与库存调整。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products/{product_id}/skus")
public class AdminSkuController {

    /**
     * SKU 领域服务
     */
    private final ISkuService skuService;
    /**
     * 货币配置服务（用于最小货币单位换算）
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 查询商品下所有 SKU
     *
     * @param productId 商品 ID
     * @return SKU 列表
     */
    @GetMapping
    public ResponseEntity<Result<List<ProductSkuRespond>>> list(@PathVariable("product_id") Long productId) {
        List<Sku> skus = skuService.list(productId, null);
        List<ProductSkuRespond> responds = skus.stream()
                .map(this::toSkuRespond)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Result.ok(responds));
    }

    /**
     * 创建 SKU
     *
     * @param productId 商品 ID
     * @param req       创建请求
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Result<ProductSkuRespond>> create(@PathVariable("product_id") Long productId,
                                                            @RequestBody ProductSkuCreateRequest req) {
        req.validate();
        List<ProductPrice> prices = buildPrices(req.getPrice(), true);
        List<SkuSpecRelation> specs = buildSpecs(req.getSpecs(), true);
        List<ProductImage> images = buildImages(req.getImages(), true);
        Sku saved = skuService.create(productId, req.getSkuCode(), req.getStock(), req.getWeight(),
                req.getStatus(), req.getIsDefault() != null && req.getIsDefault(),
                req.getBarcode(), prices, specs, images);
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(toSkuRespond(saved)));
    }

    /**
     * 更新 SKU 基础信息
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param req       更新请求
     * @return 更新结果
     */
    @PatchMapping("/{sku_id}")
    public ResponseEntity<Result<SkuOperationRespond>> updateBasic(@PathVariable("product_id") Long productId,
                                                                   @PathVariable("sku_id") Long skuId,
                                                                   @RequestBody ProductSkuUpdateRequest req) {
        req.validate();
        List<ProductImage> images = req.getImages() == null ? null : buildImages(req.getImages(), false);
        Sku updated = skuService.updateBasic(productId, skuId, req.getSkuCode(), req.getStock(),
                req.getWeight(), req.getStatus(), req.getIsDefault(), req.getBarcode(), images);
        SkuOperationRespond respond = SkuOperationRespond.builder()
                .productId(productId)
                .skuId(updated.getId())
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 更新规格绑定
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param payload   规格绑定请求
     * @return 受影响规格
     */
    @PatchMapping("/{sku_id}/specs")
    public ResponseEntity<Result<SkuSpecOperationRespond>> upsertSpecs(@PathVariable("product_id") Long productId,
                                                                       @PathVariable("sku_id") Long skuId,
                                                                       @RequestBody List<ProductSkuSpecUpsertRequest> payload) {
        List<SkuSpecRelation> specs = buildSpecs(payload, false);
        List<Long> specIds = specs.isEmpty() ? List.of() : skuService.upsertSpecs(productId, skuId, specs);
        SkuSpecOperationRespond respond = SkuSpecOperationRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .specIds(specIds)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 删除规格绑定
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param specId    规格 ID
     * @return 删除结果
     */
    @DeleteMapping("/{sku_id}/specs/{spec_id}")
    public ResponseEntity<Result<Map<String, Boolean>>> deleteSpec(@PathVariable("product_id") Long productId,
                                                                   @PathVariable("sku_id") Long skuId,
                                                                   @PathVariable("spec_id") Long specId) {
        boolean deleted = skuService.deleteSpec(productId, skuId, specId);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("deleted", deleted)));
    }

    /**
     * 更新价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param payload   价格请求
     * @return 更新结果
     */
    @PatchMapping("/{sku_id}/price")
    public ResponseEntity<Result<SkuPriceUpdateRespond>> upsertPrice(@PathVariable("product_id") Long productId,
                                                                     @PathVariable("sku_id") Long skuId,
                                                                     @RequestBody List<ProductPriceUpsertRequest> payload) {
        List<ProductPrice> prices = buildPrices(payload, false);
        List<String> currencies = prices.isEmpty() ? List.of() : skuService.upsertPrices(productId, skuId, prices);
        SkuPriceUpdateRespond respond = SkuPriceUpdateRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .currencies(currencies)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 重新计算指定商品SKU的 FX_AUTO / 缺失币种价格信息
     *
     * @param productId 商品ID
     * @param skuId     SKU ID
     * @return 返回一个包含更新后价格信息的响应实体, 其中包括商品ID, SKU ID以及更新后的货币列表
     */
    @PostMapping("/{sku_id}/price/recompute")
    public ResponseEntity<Result<SkuPriceUpdateRespond>> recomputePrice(@PathVariable("product_id") Long productId,
                                                                        @PathVariable("sku_id") Long skuId) {
        List<String> currencies = skuService.recomputeFxPrices(productId, skuId);
        SkuPriceUpdateRespond respond = SkuPriceUpdateRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .currencies(currencies)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 将指定 SKU 的价格模式切换为 MANUAL 模式 (保留金额, 清空 FX 元数据)
     *
     * @param productId 产品 ID, 用于定位具体的产品
     * @param skuId     SKU ID, 用于定位具体的 SKU
     * @param currency  货币代码, 指定要切换到 MANUAL 模式的货币
     * @return 返回一个包含更新结果的 ResponseEntity 对象. 结果中包含了操作后的响应信息, 包括受影响的产品 ID, SKU ID 以及受影响的货币列表
     */
    @PatchMapping("/{sku_id}/price/{currency}/manual")
    public ResponseEntity<Result<SkuPriceUpdateRespond>> switchPriceToManual(@PathVariable("product_id") Long productId,
                                                                             @PathVariable("sku_id") Long skuId,
                                                                             @PathVariable String currency) {
        List<String> currencies = skuService.switchPriceToManual(productId, skuId, currency);
        SkuPriceUpdateRespond respond = SkuPriceUpdateRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .currencies(currencies)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 将指定 SKU 的价格模式切换为 FX_AUTO 模式
     *
     * @param productId 产品 ID, 用于定位具体的产品
     * @param skuId     SKU ID, 用于定位具体的 SKU
     * @param currency  货币代码, 指定要切换到 FX_AUTO 模式的货币
     * @return 返回一个包含更新结果的 ResponseEntity 对象. 结果中包含了操作后的响应信息, 包括受影响的产品 ID, SKU ID 以及受影响的货币列表
     */
    @PatchMapping("/{sku_id}/price/{currency}/fx-auto")
    public ResponseEntity<Result<SkuPriceUpdateRespond>> switchPriceToFxAuto(@PathVariable("product_id") Long productId,
                                                                             @PathVariable("sku_id") Long skuId,
                                                                             @PathVariable String currency) {
        List<String> currencies = skuService.switchPriceToFxAuto(productId, skuId, currency);
        SkuPriceUpdateRespond respond = SkuPriceUpdateRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .currencies(currencies)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 调整库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param req       库存请求
     * @return 库存结果
     */
    @PatchMapping("/{sku_id}/stock")
    public ResponseEntity<Result<SkuStockAdjustRespond>> adjustStock(@PathVariable("product_id") Long productId,
                                                                     @PathVariable("sku_id") Long skuId,
                                                                     @RequestBody StockAdjustRequest req) {
        req.validate();
        int stock = skuService.adjustStock(productId, skuId, req.getMode(), req.getQuantity());
        SkuStockAdjustRespond respond = SkuStockAdjustRespond.builder()
                .productId(productId)
                .skuId(skuId)
                .stock(stock)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 通过产品ID和SKU ID删除特定的SKU
     *
     * @param productId 产品ID, 用于指定要从哪个产品中删除SKU
     * @param skuId     SKU ID, 指定要删除的具体SKU
     * @return 删除结果
     */
    @DeleteMapping("/{sku_id}")
    public ResponseEntity<Result<Map<String, Boolean>>> delete(@PathVariable("product_id") Long productId,
                                                               @PathVariable("sku_id") Long skuId) {
        boolean deleted = skuService.delete(productId, skuId);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("deleted", deleted)));
    }

    /**
     * 将 SKU 聚合转换为响应
     *
     * @param sku 聚合
     * @return 响应体
     */
    private ProductSkuRespond toSkuRespond(@NotNull Sku sku) {
        Map<String, CurrencyConfig> currencyConfigCache = new HashMap<>();
        List<ProductSkuRespond.ProductPriceRespond> prices = sku.getPrices().stream()
                .map(price -> ProductSkuRespond.ProductPriceRespond.builder()
                        .currency(price.getCurrency())
                        .listPrice(currencyConfigCache.computeIfAbsent(price.getCurrency(), currencyConfigService::get)
                                .toMajor(price.getListPrice()))
                        .salePrice(currencyConfigCache.computeIfAbsent(price.getCurrency(), currencyConfigService::get)
                                .toMajorNullable(price.getSalePrice()))
                        .isActive(price.isActive())
                        .build())
                .toList();
        List<ProductSkuRespond.ProductSkuSpecRespond> specs = sku.getSpecs().stream()
                .map(spec -> ProductSkuRespond.ProductSkuSpecRespond.builder()
                        .specId(spec.getSpecId())
                        .specCode(spec.getSpecCode())
                        .specName(spec.getSpecName())
                        .valueId(spec.getValueId())
                        .valueCode(spec.getValueCode())
                        .valueName(spec.getValueName())
                        .build())
                .toList();
        List<ProductImageRespond> images = sku.getImages().stream()
                .map(image -> ProductImageRespond.builder()
                        .url(image.getUrl())
                        .isMain(image.isMain())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();
        return ProductSkuRespond.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .stock(sku.getStock())
                .weight(sku.getWeight())
                .status(sku.getStatus())
                .isDefault(sku.isDefaultSku())
                .barcode(sku.getBarcode())
                .prices(prices)
                .specs(specs)
                .images(images)
                .build();
    }

    /**
     * 构建价格值对象
     *
     * @param payloads 请求列表
     * @param create   是否用于创建
     * @return 价格列表
     */
    private List<ProductPrice> buildPrices(@Nullable List<ProductPriceUpsertRequest> payloads, boolean create) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .filter(Objects::nonNull)
                .peek(req -> {
                    if (create)
                        req.createValidate();
                    else
                        req.updateValidate();
                })
                .map(req -> {
                    CurrencyConfig currencyConfig = currencyConfigService.get(req.getCurrency());
                    long listPriceMinor = currencyConfig.toMinorRounded(req.getListPrice());
                    Long salePriceMinor = currencyConfig.toMinorRoundedNullable(req.getSalePrice());
                    return ProductPrice.of(
                            req.getCurrency(),
                            listPriceMinor,
                            salePriceMinor,
                            req.getIsActive() == null || req.getIsActive()
                    );
                })
                .toList();
    }

    /**
     * 构建规格绑定值对象
     *
     * @param payloads 请求列表
     * @param create   是否用于创建
     * @return 规格绑定列表
     */
    private List<SkuSpecRelation> buildSpecs(@Nullable List<ProductSkuSpecUpsertRequest> payloads, boolean create) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .filter(Objects::nonNull)
                .peek(req -> {
                    if (create)
                        req.createValidate();
                    else
                        req.updateValidate();
                })
                .map(req ->
                        SkuSpecRelation.of(
                                req.getSpecId(),
                                req.getSpecCode(),
                                req.getSpecName(),
                                req.getValueId(),
                                req.getValueCode(),
                                req.getValueName()
                        )
                )
                .toList();
    }

    /**
     * 构建图片值对象
     *
     * @param payloads 请求列表
     * @param create   是否用于创建
     * @return 图片列表
     */
    private List<ProductImage> buildImages(@Nullable List<ProductImagePayload> payloads, boolean create) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .filter(Objects::nonNull)
                .peek(req -> {
                    if (create)
                        req.createValidate();
                    else
                        req.updateValidate();
                })
                .map(req ->
                        ProductImage.of(
                                req.getUrl(),
                                req.getIsMain() != null && req.getIsMain(),
                                req.getSortOrder() == null ? 0 : req.getSortOrder()
                        )
                )
                .toList();
    }
}

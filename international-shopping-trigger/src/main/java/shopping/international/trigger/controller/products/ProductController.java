package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.ProductImageRespond;
import shopping.international.api.resp.products.ProductSkuRespond;
import shopping.international.api.resp.products.PublicProductDetailRespond;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.constant.SecurityConstants;

import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户侧商品详情接口
 *
 * <p>按 slug 返回包含 SKU、规格与本地化信息的商品详情。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/products")
public class ProductController {

    /**
     * 商品查询领域服务
     */
    private final IProductQueryService productQueryService;

    /**
     * 获取商品详情
     *
     * @param slug     商品 slug
     * @param locale   语言代码
     * @param currency 价格币种
     * @return 商品详情
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Result<PublicProductDetailRespond>> detail(@PathVariable("slug") String slug,
                                                                     @RequestParam(value = "locale", required = false) @Nullable String locale,
                                                                     @RequestParam(value = "currency", required = false) @Nullable String currency) {
        requireNotBlank(slug, "商品 slug 不能为空");
        requireNotBlank(locale, "本地化 locale 不能为空");
        requireNotBlank(currency, "货币 currency 不能为空");
        slug = slug.strip();
        locale = normalizeLocale(locale);
        currency = normalizeCurrency(currency);
        IProductQueryService.ProductDetail detail = productQueryService.getPublicDetail(slug, locale, currency);
        PublicProductDetailRespond respond = toPublicRespond(detail, locale);
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 将领域读模型转换为用户侧响应
     *
     * @param detail 读模型
     * @param locale 语言
     * @return 响应体
     */
    private PublicProductDetailRespond toPublicRespond(@NotNull IProductQueryService.ProductDetail detail, @NotNull String locale) {
        Product product = detail.product();
        ProductI18n productI18n = findProductI18n(product, locale);

        String title = getI18nOrDefault(productI18n, ProductI18n::getTitle, product.getTitle());
        String subtitle = getI18nOrDefault(productI18n, ProductI18n::getSubtitle, product.getSubtitle());
        String description = getI18nOrDefault(productI18n, ProductI18n::getDescription, product.getDescription());
        String slug = getI18nOrDefault(productI18n, ProductI18n::getSlug, product.getSlug());
        List<String> tags = productI18n == null ? product.getTags() : Optional.ofNullable(productI18n.getTags()).orElse(product.getTags());

        Map<Long, String> specIdNameMap = buildSpecNameMap(product.getSpecs(), locale);
        Map<Long, String> valueIdNameMap = buildSpecValueNameMap(product.getSpecs(), locale);

        List<ProductImageRespond> gallery = product.getGallery().stream()
                .map(img -> ProductImageRespond.builder()
                        .url(img.getUrl())
                        .isMain(img.isMain())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();

        List<PublicProductDetailRespond.PublicSpecRespond> specs = buildSpecResponds(product.getSpecs(), locale);
        List<ProductSkuRespond> skus = detail.skus().stream()
                .map(sku -> toSkuRespond(sku, specIdNameMap, valueIdNameMap))
                .collect(Collectors.toList());

        return PublicProductDetailRespond.builder()
                .id(product.getId())
                .slug(slug)
                .title(title)
                .subtitle(subtitle)
                .description(description)
                .categoryId(product.getCategoryId())
                .categorySlug(detail.categorySlug())
                .brand(product.getBrand())
                .coverImageUrl(product.getCoverImageUrl())
                .stockTotal(product.getStockTotal())
                .saleCount(product.getSaleCount())
                .skuType(product.getSkuType())
                .status(product.getStatus())
                .tags(tags)
                .defaultSkuId(product.getDefaultSkuId())
                .gallery(gallery)
                .specs(specs)
                .skus(skus)
                .build();
    }

    /**
     * 构建用户侧规格响应
     *
     * @param specs  规格列表
     * @param locale 语言
     * @return 响应列表
     */
    private List<PublicProductDetailRespond.PublicSpecRespond> buildSpecResponds(@Nullable List<ProductSpec> specs, @NotNull String locale) {
        if (specs == null)
            return List.of();
        return specs.stream()
                .map(spec -> {
                    ProductSpecI18n specI18n = findSpecI18n(spec, locale);
                    String specName = getI18nOrDefault(specI18n, ProductSpecI18n::getSpecName, spec.getSpecName());
                    List<PublicProductDetailRespond.PublicSpecValueRespond> values = buildSpecValues(spec.getValues(), locale);
                    return (PublicProductDetailRespond.PublicSpecRespond) PublicProductDetailRespond.PublicSpecRespond.builder()
                            .specId(spec.getId())
                            .specCode(spec.getSpecCode())
                            .specName(specName)
                            .specType(spec.getSpecType())
                            .isRequired(spec.isRequired())
                            .values(values)
                            .build();
                })
                .toList();
    }

    /**
     * 构建用户侧规格值响应
     *
     * @param values 规格值列表
     * @param locale 语言
     * @return 响应列表
     */
    private List<PublicProductDetailRespond.PublicSpecValueRespond> buildSpecValues(@Nullable List<ProductSpecValue> values, @Nullable String locale) {
        if (values == null)
            return List.of();
        return values.stream()
                .map(value -> {
                    ProductSpecValueI18n i18n = findSpecValueI18n(value, locale);
                    String valueName = getI18nOrDefault(i18n, ProductSpecValueI18n::getValueName, value.getValueName());
                    return (PublicProductDetailRespond.PublicSpecValueRespond) PublicProductDetailRespond.PublicSpecValueRespond.builder()
                            .valueId(value.getId())
                            .valueCode(value.getValueCode())
                            .valueName(valueName)
                            .attributes(value.getAttributes())
                            .build();
                })
                .toList();
    }

    /**
     * 将 SKU 转换为响应, 并覆盖规格名称
     *
     * @param sku          SKU 聚合
     * @param specNameMap  规格名称映射
     * @param valueNameMap 规格值名称映射
     * @return 响应
     */
    private ProductSkuRespond toSkuRespond(@NotNull Sku sku, @NotNull Map<Long, String> specNameMap, @NotNull Map<Long, String> valueNameMap) {
        List<ProductSkuRespond.ProductPriceRespond> prices = sku.getPrices().stream()
                .map(price -> ProductSkuRespond.ProductPriceRespond.builder()
                        .currency(price.getCurrency())
                        .listPrice(price.getListPrice())
                        .salePrice(price.getSalePrice())
                        .isActive(price.isActive())
                        .build())
                .toList();
        List<ProductSkuRespond.ProductSkuSpecRespond> specs = sku.getSpecs().stream()
                .map(spec -> ProductSkuRespond.ProductSkuSpecRespond.builder()
                        .specId(spec.getSpecId())
                        .specCode(spec.getSpecCode())
                        .specName(specNameMap.getOrDefault(spec.getSpecId(), spec.getSpecName()))
                        .valueId(spec.getValueId())
                        .valueCode(spec.getValueCode())
                        .valueName(valueNameMap.getOrDefault(spec.getValueId(), spec.getValueName()))
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
                .price(prices)
                .specs(specs)
                .images(images)
                .build();
    }

    /**
     * 查找商品多语言
     *
     * @param product 商品聚合
     * @param locale  语言
     * @return 多语言对象
     */
    @Nullable
    private ProductI18n findProductI18n(@NotNull Product product, @NotNull String locale) {
        return product.getI18nList().stream()
                .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找规格多语言
     *
     * @param spec   规格
     * @param locale 语言
     * @return 多语言对象
     */
    @Nullable
    private ProductSpecI18n findSpecI18n(@NotNull ProductSpec spec, @NotNull String locale) {
        return spec.getI18nList().stream()
                .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找规格值多语言
     *
     * @param value  规格值
     * @param locale 语言
     * @return 多语言对象
     */
    @Nullable
    private ProductSpecValueI18n findSpecValueI18n(@NotNull ProductSpecValue value, @Nullable String locale) {
        if (locale == null)
            return null;
        return value.getI18nList().stream()
                .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 构建规格名称映射
     *
     * @param specs  规格列表
     * @param locale 语言
     * @return Spec ID -> 本地化 Spec 名称
     */
    private Map<Long, String> buildSpecNameMap(@Nullable List<ProductSpec> specs, @NotNull String locale) {
        if (specs == null || specs.isEmpty())
            return Collections.emptyMap();
        return specs.stream()
                .collect(Collectors.toMap(
                        ProductSpec::getId,
                        spec -> {
                            ProductSpecI18n i18n = findSpecI18n(spec, locale);
                            return getI18nOrDefault(i18n, ProductSpecI18n::getSpecName, spec.getSpecName());
                        }
                ));
    }

    /**
     * 构建规格值名称映射
     *
     * @param specs  规格列表
     * @param locale 语言
     * @return 传入的 Spec 列表中包括的 Spec Value ID -> 本地化 Spec Value 名称
     */
    private Map<Long, String> buildSpecValueNameMap(@Nullable List<ProductSpec> specs, @NotNull String locale) {
        if (specs == null || specs.isEmpty())
            return Collections.emptyMap();
        Map<Long, String> map = new HashMap<>();
        for (ProductSpec spec : specs) {
            for (ProductSpecValue value : spec.getValues()) {
                ProductSpecValueI18n i18n = findSpecValueI18n(value, locale);
                String name = getI18nOrDefault(i18n, ProductSpecValueI18n::getValueName, value.getValueName());
                map.put(value.getId(), name);
            }
        }
        return map;
    }
}

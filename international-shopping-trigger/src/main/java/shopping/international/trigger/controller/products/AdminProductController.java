package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.ProductI18nUpsertRequest;
import shopping.international.api.req.products.ProductImagePayload;
import shopping.international.api.req.products.ProductSpuBasicUpsertRequest;
import shopping.international.api.req.products.ProductStatusUpdateRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.*;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.products.IProductService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;

import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 管理端商品接口
 *
 * <p>覆盖商品分页、详情以及基础信息、状态、多语言与图库维护。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products")
public class AdminProductController {

    /**
     * 商品领域服务
     */
    private final IProductService productService;

    /**
     * 分页查询商品
     *
     * @param page           页码
     * @param size           页大小
     * @param status         状态过滤
     * @param skuType        SKU 类型过滤
     * @param categoryId     分类过滤
     * @param keyword        关键词
     * @param tag            标签
     * @param includeDeleted 是否包含删除
     * @return 商品列表
     */
    @GetMapping
    public ResponseEntity<Result<List<ProductSpuRespond>>> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                                                @RequestParam(name = "size", defaultValue = "20") int size,
                                                                @RequestParam(name = "status", required = false) ProductStatus status,
                                                                @RequestParam(name = "sku_type", required = false) SkuType skuType,
                                                                @RequestParam(name = "category_id", required = false) Long categoryId,
                                                                @RequestParam(name = "keyword", required = false) String keyword,
                                                                @RequestParam(name = "tag", required = false) String tag,
                                                                @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted) {
        PageQuery pageQuery = PageQuery.of(page, size, 100);
        if (keyword != null)
            requireNotBlank(keyword, "keyword 不能为空");
        if (tag != null)
            requireNotBlank(tag, "tag 不能为空");
        PageResult<Product> pageResult = productService.page(pageQuery, status, skuType, categoryId, keyword, tag, includeDeleted);
        List<ProductSpuRespond> data = pageResult.items().stream()
                .map(this::toSpuRespond)
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(pageQuery.page())
                .size(pageQuery.size())
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 创建商品基础信息
     *
     * @param req 请求体
     * @return 商品详情
     */
    @PostMapping
    public ResponseEntity<Result<AdminProductDetailRespond>> create(@RequestBody ProductSpuBasicUpsertRequest req) {
        req.createValidate();
        List<String> tags = req.getTags() == null ? List.of() : req.getTags();
        Product product = productService.createBasic(req.getSlug(), req.getTitle(), req.getSubtitle(),
                req.getDescription(), req.getCategoryId(), req.getBrand(), req.getCoverImageUrl(),
                req.getSkuType(), req.getStatus(), tags);
        AdminProductDetailRespond respond = toDetailRespond(productService.detail(product.getId()));
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus()).body(Result.created(respond));
    }

    /**
     * 获取商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    @GetMapping("/{product_id}")
    public ResponseEntity<Result<AdminProductDetailRespond>> detail(@PathVariable("product_id") Long productId) {
        AdminProductDetailRespond respond = toDetailRespond(productService.detail(productId));
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 更新商品基础信息
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 更新结果
     */
    @PatchMapping("/{product_id}")
    public ResponseEntity<Result<Map<String, Long>>> updateBasic(@PathVariable("product_id") Long productId,
                                                                 @RequestBody ProductSpuBasicUpsertRequest req) {
        req.updateValidate();
        productService.updateBasic(productId, req.getSlug(), req.getTitle(), req.getSubtitle(), req.getDescription(),
                req.getCategoryId(), req.getBrand(), req.getCoverImageUrl(), req.getSkuType(), req.getStatus(), req.getTags());
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("product_id", productId)));
    }

    /**
     * 更新商品状态
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 状态结果
     */
    @PatchMapping("/{product_id}/status")
    public ResponseEntity<Result<Map<String, ProductStatus>>> updateStatus(@PathVariable("product_id") Long productId,
                                                                           @RequestBody ProductStatusUpdateRequest req) {
        req.validate();
        ProductStatus status = productService.changeStatus(productId, req.getStatus());
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("status", status)));
    }

    /**
     * 新增商品多语言
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 语言代码
     */
    @PostMapping("/{product_id}/i18n")
    public ResponseEntity<Result<Map<String, String>>> addI18n(@PathVariable("product_id") Long productId,
                                                               @RequestBody ProductI18nUpsertRequest req) {
        req.createValidate();
        ProductI18n i18n = ProductI18n.of(req.getLocale(), req.getTitle(), req.getSubtitle(), req.getDescription(),
                req.getSlug(), req.getTags());
        productService.addI18n(productId, i18n);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("locale", i18n.getLocale())));
    }

    /**
     * 更新商品多语言
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 语言代码
     */
    @PatchMapping("/{product_id}/i18n")
    public ResponseEntity<Result<Map<String, String>>> updateI18n(@PathVariable("product_id") Long productId,
                                                                  @RequestBody ProductI18nUpsertRequest req) {
        req.updateValidate();
        ProductI18n updated = productService.updateI18n(productId, req.getLocale(), req.getTitle(), req.getSubtitle(),
                req.getDescription(), req.getSlug(), req.getTags());
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("locale", updated.getLocale())));
    }

    /**
     * 删除指定产品在特定语言环境下的国际化信息
     *
     * @param productId 产品的唯一标识符
     * @param locale    指定的语言环境代码, 如 en_US, zh_CN 等
     * @return 包含成功删除的语言环境代码的响应实体
     */
    @DeleteMapping("/{product_id}/i18n/{locale}")
    public ResponseEntity<Result<Map<String, String>>> deleteI18n(@PathVariable("product_id") Long productId,
                                                                  @PathVariable String locale) {
        locale = normalizeLocale(locale);
        requireNotBlank(locale, "locale 不能为空");
        String deletedI18n = productService.deleteI18n(productId, locale);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("locale", deletedI18n)));
    }

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param payload   图库
     * @return 更新结果
     */
    @PutMapping("/{product_id}/gallery")
    public ResponseEntity<Result<Map<String, Object>>> replaceGallery(@PathVariable("product_id") Long productId,
                                                                      @RequestBody List<ProductImagePayload> payload) {
        List<ProductImage> images = buildImages(payload);
        int count = productService.replaceGallery(productId, images);
        Map<String, Object> data = new HashMap<>();
        data.put("product_id", productId);
        data.put("image_count", count);
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 构建列表响应
     *
     * @param product 商品聚合
     * @return 列表响应
     */
    private ProductSpuRespond toSpuRespond(@NotNull Product product) {
        List<ProductImageRespond> gallery = product.getGallery().stream()
                .map(img -> ProductImageRespond.builder()
                        .url(img.getUrl())
                        .isMain(img.isMain())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();
        return ProductSpuRespond.builder()
                .id(product.getId())
                .slug(product.getSlug())
                .title(product.getTitle())
                .subtitle(product.getSubtitle())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categorySlug(null)
                .brand(product.getBrand())
                .coverImageUrl(product.getCoverImageUrl())
                .stockTotal(product.getStockTotal())
                .saleCount(product.getSaleCount())
                .skuType(product.getSkuType())
                .status(product.getStatus())
                .tags(product.getTags())
                .gallery(gallery)
                .likedAt(null)
                .build();
    }

    /**
     * 构建详情响应
     *
     * @param detail 商品详情读模型
     * @return 详情响应
     */
    private AdminProductDetailRespond toDetailRespond(@NotNull IProductService.ProductDetail detail) {
        Product product = detail.product();
        List<ProductImageRespond> gallery = product.getGallery().stream()
                .map(image -> ProductImageRespond.builder()
                        .url(image.getUrl())
                        .isMain(image.isMain())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();
        List<ProductSkuRespond> skus = detail.skus().stream()
                .map(this::toSkuRespond)
                .toList();
        List<AdminSpecRespond> specs = product.getSpecs().stream()
                .map(this::toSpecRespond)
                .toList();
        List<AdminProductDetailRespond.ProductI18nRespond> i18nList = product.getI18nList().stream()
                .map(i18n -> AdminProductDetailRespond.ProductI18nRespond.builder()
                        .locale(i18n.getLocale())
                        .title(i18n.getTitle())
                        .subtitle(i18n.getSubtitle())
                        .description(i18n.getDescription())
                        .slug(i18n.getSlug())
                        .tags(i18n.getTags())
                        .build())
                .toList();
        return AdminProductDetailRespond.builder()
                .id(product.getId())
                .slug(product.getSlug())
                .title(product.getTitle())
                .subtitle(product.getSubtitle())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categorySlug(detail.categorySlug())
                .brand(product.getBrand())
                .coverImageUrl(product.getCoverImageUrl())
                .stockTotal(product.getStockTotal())
                .saleCount(product.getSaleCount())
                .skuType(product.getSkuType())
                .status(product.getStatus())
                .tags(product.getTags())
                .defaultSkuId(product.getDefaultSkuId())
                .gallery(gallery)
                .specs(specs)
                .skus(skus)
                .i18nList(i18nList)
                .build();
    }

    /**
     * 将 SKU 聚合转换为响应
     *
     * @param sku SKU 聚合
     * @return 响应体
     */
    private ProductSkuRespond toSkuRespond(@NotNull Sku sku) {
        List<ProductSkuRespond.ProductPriceRespond> prices = sku.getPrices().stream()
                .map(this::toPriceRespond)
                .toList();
        List<ProductSkuRespond.ProductSkuSpecRespond> specs = sku.getSpecs().stream()
                .map(this::toSkuSpecRespond)
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
     * 构建 SKU 规格响应
     *
     * @param relation SKU 规格绑定
     * @return 响应体
     */
    private ProductSkuRespond.ProductSkuSpecRespond toSkuSpecRespond(@NotNull SkuSpecRelation relation) {
        return ProductSkuRespond.ProductSkuSpecRespond.builder()
                .specId(relation.getSpecId())
                .specCode(relation.getSpecCode())
                .specName(relation.getSpecName())
                .valueId(relation.getValueId())
                .valueCode(relation.getValueCode())
                .valueName(relation.getValueName())
                .build();
    }

    /**
     * 构建价格响应
     *
     * @param price 价格值对象
     * @return 响应体
     */
    private ProductSkuRespond.ProductPriceRespond toPriceRespond(@NotNull ProductPrice price) {
        return ProductSkuRespond.ProductPriceRespond.builder()
                .currency(price.getCurrency())
                .listPrice(price.getListPrice())
                .salePrice(price.getSalePrice())
                .isActive(price.isActive())
                .build();
    }

    /**
     * 构建规格响应
     *
     * @param spec 规格实体
     * @return 响应体
     */
    private AdminSpecRespond toSpecRespond(@NotNull ProductSpec spec) {
        List<AdminSpecValueRespond> values = spec.getValues().stream()
                .map(this::toSpecValueRespond)
                .toList();
        List<AdminSpecRespond.SpecI18nPayloadRespond> i18n = spec.getI18nList().stream()
                .map(item -> AdminSpecRespond.SpecI18nPayloadRespond.builder()
                        .locale(item.getLocale())
                        .specName(item.getSpecName())
                        .build())
                .toList();
        return AdminSpecRespond.builder()
                .specId(spec.getId())
                .specCode(spec.getSpecCode())
                .specName(spec.getSpecName())
                .specType(spec.getSpecType())
                .isRequired(spec.isRequired())
                .sortOrder(spec.getSortOrder())
                .enabled(spec.isEnabled())
                .values(values)
                .i18nList(i18n)
                .build();
    }

    /**
     * 构建规格值响应
     *
     * @param value 规格值实体
     * @return 响应体
     */
    private AdminSpecValueRespond toSpecValueRespond(@NotNull ProductSpecValue value) {
        List<AdminSpecValueRespond.SpecValueI18nPayloadRespond> i18n = value.getI18nList().stream()
                .map(item -> AdminSpecValueRespond.SpecValueI18nPayloadRespond.builder()
                        .locale(item.getLocale())
                        .valueName(item.getValueName())
                        .build())
                .toList();
        return AdminSpecValueRespond.builder()
                .valueId(value.getId())
                .valueCode(value.getValueCode())
                .valueName(value.getValueName())
                .attributes(value.getAttributes())
                .sortOrder(value.getSortOrder())
                .enabled(value.isEnabled())
                .i18nList(i18n)
                .build();
    }

    /**
     * 将图库请求转换为值对象
     *
     * @param payloads 请求列表
     * @return 图库值对象
     */
    private List<ProductImage> buildImages(@Nullable List<ProductImagePayload> payloads) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .filter(Objects::nonNull)
                .peek(ProductImagePayload::createValidate)
                .map(req -> ProductImage.of(
                        req.getUrl(),
                        req.getIsMain() != null && req.getIsMain(),
                        req.getSortOrder() == null ? 0 : req.getSortOrder()
                ))
                .toList();
    }
}

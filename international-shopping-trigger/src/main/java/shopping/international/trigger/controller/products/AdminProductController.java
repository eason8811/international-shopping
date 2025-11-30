package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.ProductDetailRespond;
import shopping.international.api.resp.products.ProductRespond;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductAdminService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端商品接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products")
public class AdminProductController {
    /**
     * 商品管理服务
     */
    private final IProductAdminService productAdminService;

    /**
     * 分页搜索商品
     *
     * @param page           页码, 默认为 1
     * @param size           每页数量, 默认为 20
     * @param status         状态过滤
     * @param skuType        SKU 类型过滤
     * @param categoryId     分类 ID 过滤
     * @param keyword        关键词
     * @param tag            标签
     * @param includeDeleted 是否包含已删除
     * @return 商品列表
     */
    @GetMapping
    public ResponseEntity<Result<List<ProductRespond>>> page(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             @RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "sku_type", required = false) String skuType,
                                                             @RequestParam(value = "category_id", required = false) Long categoryId,
                                                             @RequestParam(value = "keyword", required = false) String keyword,
                                                             @RequestParam(value = "tag", required = false) String tag,
                                                             @RequestParam(value = "include_deleted", defaultValue = "false") boolean includeDeleted) {
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 20 : Math.min(size, 100);
        ProductStatus filterStatus = parseStatus(status);
        SkuType filterSkuType = parseSkuType(skuType);
        IProductAdminService.PageResult<ProductSummary> pageResult = productAdminService.page(page, size, filterStatus, filterSkuType, categoryId, keyword, tag, includeDeleted);
        List<ProductRespond> data = pageResult.items().stream()
                .map(ProductRespond::from)
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(page)
                .size(size)
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 创建商品
     *
     * @param request 商品保存请求
     * @return 详情
     */
    @PostMapping
    public ResponseEntity<Result<ProductDetailRespond>> create(@RequestBody ProductSaveRequest request) {
        request.validate();
        ProductDetail detail = productAdminService.create(toCommand(request));
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 获取商品详情
     *
     * @param productId 商品 ID
     * @return 详情
     */
    @GetMapping("/{product_id}")
    public ResponseEntity<Result<ProductDetailRespond>> detail(@PathVariable("product_id") Long productId) {
        ProductDetail detail = productAdminService.detail(productId);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 更新商品基础信息
     *
     * @param productId 商品 ID
     * @param request   商品保存请求
     * @return 详情
     */
    @PatchMapping("/{product_id}")
    public ResponseEntity<Result<ProductDetailRespond>> update(@PathVariable("product_id") Long productId,
                                                               @RequestBody ProductSaveRequest request) {
        request.validate();
        ProductDetail detail = productAdminService.update(productId, toCommand(request));
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 更新商品状态
     *
     * @param productId 商品 ID
     * @param request   状态请求
     * @return 详情
     */
    @PatchMapping("/{product_id}/status")
    public ResponseEntity<Result<ProductDetailRespond>> updateStatus(@PathVariable("product_id") Long productId,
                                                                     @RequestBody ProductStatusUpdateRequest request) {
        request.validate();
        ProductDetail detail = productAdminService.updateStatus(productId, request.getStatus());
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 批量维护商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     * @return 详情
     */
    @PatchMapping("/{product_id}/i18n")
    public ResponseEntity<Result<ProductDetailRespond>> upsertI18n(@PathVariable("product_id") Long productId,
                                                                   @RequestBody List<ProductI18nUpsertRequest> payloads) {
        List<ProductI18nUpsertRequest> safePayloads = payloads == null ? List.of() : payloads;
        List<ProductI18n> i18nList = new ArrayList<>();
        for (ProductI18nUpsertRequest payload : safePayloads) {
            if (payload == null)
                continue;
            payload.validate();
            i18nList.add(ProductI18n.of(payload.getLocale(), payload.getTitle(), payload.getSubtitle(),
                    payload.getDescription(), payload.getSlug(), payload.getTags()));
        }
        ProductDetail detail = productAdminService.upsertI18n(productId, i18nList);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param payloads  图库列表
     * @return 详情
     */
    @PatchMapping("/{product_id}/gallery")
    public ResponseEntity<Result<ProductDetailRespond>> replaceGallery(@PathVariable("product_id") Long productId,
                                                                       @RequestBody List<ProductImagePayload> payloads) {
        List<ProductImagePayload> safePayloads = payloads == null ? List.of() : payloads;
        List<ProductImage> gallery = new ArrayList<>();
        for (ProductImagePayload payload : safePayloads) {
            if (payload == null)
                continue;
            payload.validate();
            gallery.add(ProductImage.of(payload.getUrl(), Boolean.TRUE.equals(payload.getIsMain()), payload.getSortOrder()));
        }
        ProductDetail detail = productAdminService.replaceGallery(productId, gallery);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 批量维护商品规格
     *
     * @param productId 商品 ID
     * @param payloads  规格列表
     * @return 商品详情
     */
    @PatchMapping("/{product_id}/specs")
    public ResponseEntity<Result<ProductDetailRespond>> upsertSpecs(@PathVariable("product_id") Long productId,
                                                                    @RequestBody List<ProductSpecUpsertRequest> payloads) {
        List<ProductSpecUpsertRequest> safePayloads = payloads == null ? List.of() : payloads;
        List<ProductSpecUpsertCommand> commands = new ArrayList<>();
        for (ProductSpecUpsertRequest payload : safePayloads) {
            if (payload == null)
                continue;
            payload.validate();
            commands.add(toSpecCommand(payload));
        }
        ProductDetail detail = productAdminService.upsertSpecs(productId, commands);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 批量创建 SKU
     *
     * @param productId 商品 ID
     * @param request   SKU 请求
     * @return 商品详情
     */
    @PostMapping("/{product_id}/skus")
    public ResponseEntity<Result<ProductDetailRespond>> createSkus(@PathVariable("product_id") Long productId,
                                                                   @RequestBody ProductSkuUpsertRequest request) {
        request.validate();
        ProductDetail detail = productAdminService.createSkus(productId, toSkuCommand(request));
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 批量更新 SKU
     *
     * @param productId 商品 ID
     * @param request   SKU 请求
     * @return 商品详情
     */
    @PatchMapping("/{product_id}/skus")
    public ResponseEntity<Result<ProductDetailRespond>> updateSkus(@PathVariable("product_id") Long productId,
                                                                   @RequestBody ProductSkuUpsertRequest request) {
        request.validate();
        ProductDetail detail = productAdminService.updateSkus(productId, toSkuCommand(request));
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, null)));
    }

    /**
     * 更新 SKU 价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param request   价格请求
     * @return SKU
     */
    @PatchMapping("/{product_id}/skus/{sku_id}/price")
    public ResponseEntity<Result<ProductDetailRespond.SkuRespond>> updateSkuPrice(@PathVariable("product_id") Long productId,
                                                                                  @PathVariable("sku_id") Long skuId,
                                                                                  @RequestBody ProductPriceUpsertRequest request) {
        request.validate();
        ProductPriceUpsertCommand command = ProductPriceUpsertCommand.of(request.getCurrency(), request.getListPrice(),
                request.getSalePrice(), Boolean.TRUE.equals(request.getIsActive()));
        ProductSku sku = productAdminService.updateSkuPrice(productId, skuId, command);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.SkuRespond.from(sku)));
    }

    /**
     * 调整 SKU 库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param request   库存请求
     * @return SKU
     */
    @PatchMapping("/{product_id}/skus/{sku_id}/stock")
    public ResponseEntity<Result<ProductDetailRespond.SkuRespond>> adjustSkuStock(@PathVariable("product_id") Long productId,
                                                                                  @PathVariable("sku_id") Long skuId,
                                                                                  @RequestBody StockAdjustRequest request) {
        request.validate();
        StockAdjustCommand command = StockAdjustCommand.of(request.getMode(), request.getQuantity(), request.getReason());
        ProductSku sku = productAdminService.adjustSkuStock(productId, skuId, command);
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.SkuRespond.from(sku)));
    }

    /**
     * 将请求转换为保存命令
     *
     * @param request 商品保存请求
     * @return 保存命令
     */
    private ProductSaveCommand toCommand(ProductSaveRequest request) {
        return new ProductSaveCommand(request.getSlug(), request.getTitle(), request.getSubtitle(),
                request.getDescription(), request.getCategoryId(), request.getBrand(), request.getCoverImageUrl(),
                request.getSkuType(), request.getStatus(), request.getTags());
    }

    /**
     * 将规格请求转换为命令
     *
     * @param request 规格请求
     * @return 规格命令
     */
    private ProductSpecUpsertCommand toSpecCommand(ProductSpecUpsertRequest request) {
        List<ProductSpecI18n> i18nList = toSpecI18nList(request.getI18nList());
        List<ProductSpecValueUpsertCommand> values = new ArrayList<>();
        List<ProductSpecValueUpsertRequest> valuePayloads = request.getValues() == null ? List.of() : request.getValues();
        int sort = 0;
        for (ProductSpecValueUpsertRequest valuePayload : valuePayloads) {
            if (valuePayload == null)
                continue;
            valuePayload.validate();
            values.add(ProductSpecValueUpsertCommand.of(valuePayload.getValueId(), request.getSpecId(),
                    valuePayload.getValueCode(), valuePayload.getValueName(), valuePayload.getAttributes(),
                    Boolean.TRUE.equals(valuePayload.getIsEnabled()), sort++, toSpecValueI18nList(valuePayload.getI18nList())));
        }
        return ProductSpecUpsertCommand.of(request.getSpecId(), request.getSpecCode(), request.getSpecName(),
                request.getSpecType(), Boolean.TRUE.equals(request.getIsRequired()), true, 0, i18nList, values);
    }

    private List<ProductSpecI18n> toSpecI18nList(List<ProductSpecI18nPayload> payloads) {
        if (payloads == null)
            return List.of();
        List<ProductSpecI18n> i18nList = new ArrayList<>();
        for (ProductSpecI18nPayload payload : payloads) {
            if (payload == null)
                continue;
            payload.validate();
            i18nList.add(ProductSpecI18n.of(payload.getLocale(), payload.getSpecName()));
        }
        return i18nList;
    }

    private List<ProductSpecValueI18n> toSpecValueI18nList(List<ProductSpecValueI18nPayload> payloads) {
        if (payloads == null)
            return List.of();
        List<ProductSpecValueI18n> i18nList = new ArrayList<>();
        for (ProductSpecValueI18nPayload payload : payloads) {
            if (payload == null)
                continue;
            payload.validate();
            i18nList.add(ProductSpecValueI18n.of(payload.getLocale(), payload.getValueName()));
        }
        return i18nList;
    }

    /**
     * 将 SKU 请求转换为命令
     *
     * @param request SKU 请求
     * @return SKU 命令
     */
    private ProductSkuUpsertCommand toSkuCommand(ProductSkuUpsertRequest request) {
        List<ProductSkuUpsertItemCommand> itemList = new ArrayList<>();
        for (ProductSkuUpsertRequestItem item : request.getSkus()) {
            if (item == null)
                continue;
            item.validate();
            ProductPriceUpsertCommand price = null;
            if (item.getPrice() != null) {
                ProductPriceUpsertRequest priceReq = item.getPrice();
                price = ProductPriceUpsertCommand.of(priceReq.getCurrency(), priceReq.getListPrice(),
                        priceReq.getSalePrice(), Boolean.TRUE.equals(priceReq.getIsActive()));
            }
            List<ProductSkuSpecUpsertCommand> specs = mapSkuSpecs(item.getSpecs());
            List<ProductImage> images = mapSkuImages(item.getImages());
            itemList.add(ProductSkuUpsertItemCommand.of(item.getId(), item.getSkuCode(), item.getStock(), item.getWeight(),
                    item.getStatus(), Boolean.TRUE.equals(item.getIsDefault()), item.getBarcode(), price, specs, images));
        }
        return ProductSkuUpsertCommand.of(itemList);
    }

    private List<ProductSkuSpecUpsertCommand> mapSkuSpecs(List<ProductSkuSpecUpsertRequest> specs) {
        if (specs == null)
            return List.of();
        List<ProductSkuSpecUpsertCommand> commands = new ArrayList<>();
        for (ProductSkuSpecUpsertRequest spec : specs) {
            if (spec == null)
                continue;
            commands.add(ProductSkuSpecUpsertCommand.of(spec.getSpecId(), spec.getSpecCode(),
                    spec.getValueId(), spec.getValueCode()));
        }
        return commands;
    }

    private List<ProductImage> mapSkuImages(List<ProductImagePayload> payloads) {
        if (payloads == null)
            return List.of();
        List<ProductImage> images = new ArrayList<>();
        for (ProductImagePayload payload : payloads) {
            if (payload == null)
                continue;
            payload.validate();
            images.add(ProductImage.of(payload.getUrl(), Boolean.TRUE.equals(payload.getIsMain()), payload.getSortOrder()));
        }
        return images;
    }

    /**
     * 解析状态入参
     *
     * @param raw 状态字符串
     * @return 枚举
     */
    private ProductStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return ProductStatus.from(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalParamException("非法的商品状态");
        }
    }

    /**
     * 解析 SKU 类型
     *
     * @param raw 类型字符串
     * @return 枚举
     */
    private SkuType parseSkuType(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return SkuType.from(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalParamException("非法的 SKU 类型");
        }
    }
}

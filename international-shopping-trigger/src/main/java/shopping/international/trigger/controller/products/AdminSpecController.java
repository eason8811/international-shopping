package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.ProductSpecI18nPayload;
import shopping.international.api.req.products.ProductSpecUpsertRequest;
import shopping.international.api.req.products.ProductSpecValueI18nPayload;
import shopping.international.api.req.products.ProductSpecValueUpsertRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.*;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.service.products.IProductSpecService;
import shopping.international.types.constant.SecurityConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端规格维护接口
 *
 * <p>覆盖规格与规格值的查询、新增、更新与删除。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products/{product_id}/specs")
public class AdminSpecController {

    /**
     * 规格领域服务
     */
    private final IProductSpecService productSpecService;

    /**
     * 获取商品规格列表
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    @GetMapping
    public ResponseEntity<Result<List<AdminSpecRespond>>> list(@PathVariable("product_id") Long productId) {
        List<ProductSpec> specs = productSpecService.list(productId);
        List<AdminSpecRespond> responds = specs.stream()
                .map(this::toSpecRespond)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Result.ok(responds));
    }

    /**
     * 新增规格
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 规格 ID
     */
    @PostMapping
    public ResponseEntity<Result<Map<String, Long>>> create(@PathVariable("product_id") Long productId,
                                                               @RequestBody ProductSpecUpsertRequest req) {
        req.createValidate();
        ProductSpec spec = ProductSpec.create(productId, req.getSpecCode(), req.getSpecName(), req.getSpecType(),
                Boolean.TRUE.equals(req.getIsRequired()), 0, true, buildSpecI18n(req.getI18nList()), List.of());
        Long specId = productSpecService.create(productId, spec);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("spec_id", specId)));
    }

    /**
     * 增量更新规格
     *
     * @param productId 商品 ID
     * @param req       请求体
     * @return 规格 ID
     */
    @PatchMapping
    public ResponseEntity<Result<Map<String, Long>>> update(@PathVariable("product_id") Long productId,
                                                               @RequestBody ProductSpecUpsertRequest req) {
        req.updateValidate();
        List<ProductSpecI18n> i18nList = req.getI18nList() == null ? null : buildSpecI18n(req.getI18nList());
        boolean patchI18n = i18nList != null;
        Long specId = productSpecService.update(productId, req.getSpecId(), req.getSpecName(), req.getSpecType(),
                req.getIsRequired(), null, null, i18nList, patchI18n);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("spec_id", specId)));
    }

    /**
     * 删除规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 删除结果
     */
    @DeleteMapping("/{spec_id}")
    public ResponseEntity<Result<SpecDeleteRespond>> delete(@PathVariable("product_id") Long productId,
                                                            @PathVariable("spec_id") Long specId) {
        boolean deleted = productSpecService.delete(productId, specId);
        SpecDeleteRespond respond = SpecDeleteRespond.builder()
                .productId(productId)
                .specId(specId)
                .deleted(deleted)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 获取规格值列表
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @GetMapping("/{spec_id}/values")
    public ResponseEntity<Result<List<AdminSpecValueRespond>>> listValues(@PathVariable("product_id") Long productId,
                                                                          @PathVariable("spec_id") Long specId) {
        List<ProductSpecValue> values = productSpecService.listValues(productId, specId);
        List<AdminSpecValueRespond> responds = values.stream()
                .map(this::toSpecValueRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(responds));
    }

    /**
     * 新增规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param req       请求体
     * @return 规格值 ID
     */
    @PostMapping("/{spec_id}/values")
    public ResponseEntity<Result<Map<String, Long>>> createValue(@PathVariable("product_id") Long productId,
                                                                 @PathVariable("spec_id") Long specId,
                                                                 @RequestBody ProductSpecValueUpsertRequest req) {
        req.createValidate();
        ProductSpecValue value = ProductSpecValue.create(productId, specId, req.getValueCode(), req.getValueName(),
                req.getAttributes() == null ? Collections.emptyMap() : req.getAttributes(), 0,
                Boolean.TRUE.equals(req.getIsEnabled()), buildSpecValueI18n(req.getI18nList()));
        Long valueId = productSpecService.createValue(productId, specId, value);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("value_id", valueId)));
    }

    /**
     * 更新规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param req       请求体
     * @return 规格值 ID
     */
    @PatchMapping("/{spec_id}/values")
    public ResponseEntity<Result<Map<String, Long>>> updateValue(@PathVariable("product_id") Long productId,
                                                                 @PathVariable("spec_id") Long specId,
                                                                 @RequestBody ProductSpecValueUpsertRequest req) {
        req.updateValidate();
        List<ProductSpecValueI18n> i18nList = req.getI18nList() == null ? null : buildSpecValueI18n(req.getI18nList());
        boolean patchI18n = i18nList != null;
        Long valueId = productSpecService.updateValue(productId, specId, req.getValueId(), req.getValueCode(),
                req.getValueName(), req.getAttributes(), null, req.getIsEnabled(), i18nList, patchI18n);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("value_id", valueId)));
    }

    /**
     * 删除规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param valueId   规格值 ID
     * @return 删除结果
     */
    @DeleteMapping("/{spec_id}/values/{value_id}")
    public ResponseEntity<Result<SpecValueDeleteRespond>> deleteValue(@PathVariable("product_id") Long productId,
                                                                      @PathVariable("spec_id") Long specId,
                                                                      @PathVariable("value_id") Long valueId) {
        boolean deleted = productSpecService.deleteValue(productId, specId, valueId);
        SpecValueDeleteRespond respond = SpecValueDeleteRespond.builder()
                .productId(productId)
                .specId(specId)
                .valueId(valueId)
                .deleted(deleted)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
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
                .i18nList(i18n)
                .build();
    }

    /**
     * 将请求多语言转换为值对象
     *
     * @param payloads 请求列表
     * @return 值对象列表
     */
    private List<ProductSpecI18n> buildSpecI18n(@Nullable List<ProductSpecI18nPayload> payloads) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .peek(ProductSpecI18nPayload::validate)
                .map(item -> ProductSpecI18n.of(item.getLocale(), item.getSpecName()))
                .toList();
    }

    /**
     * 将请求多语言转换为值对象
     *
     * @param payloads 请求列表
     * @return 值对象列表
     */
    private List<ProductSpecValueI18n> buildSpecValueI18n(@Nullable List<ProductSpecValueI18nPayload> payloads) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        return payloads.stream()
                .peek(ProductSpecValueI18nPayload::validate)
                .map(item -> ProductSpecValueI18n.of(item.getLocale(), item.getValueName()))
                .toList();
    }
}

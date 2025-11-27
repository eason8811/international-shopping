package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.CategoryI18nPayload;
import shopping.international.api.req.products.CategoryUpsertRequest;
import shopping.international.api.req.products.ToggleEnableRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.CategoryNodeRespond;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;
import shopping.international.domain.model.vo.products.CategoryUpsertCommand;
import shopping.international.domain.service.products.ICategoryAdminService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;

import java.util.List;
import java.util.Objects;

/**
 * 管理端分类接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products/categories")
public class AdminCategoryController {

    /**
     * 分类管理服务
     */
    private final ICategoryAdminService categoryAdminService;

    /**
     * 分页查询分类
     */
    @GetMapping
    public ResponseEntity<Result<List<CategoryNodeRespond>>> page(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(value = "parent_id", required = false) Long parentId,
                                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                                  @RequestParam(value = "is_enabled", required = false) Boolean isEnabled) {
        int pageNo = page <= 0 ? 1 : page;
        int pageSize = size <= 0 ? 20 : Math.min(size, 100);
        boolean filterByParent = parentId != null;
        Long normalizedParentId = parentId;
        if (filterByParent && normalizedParentId != null && normalizedParentId <= 0)
            normalizedParentId = null;

        ICategoryAdminService.PageResult<CategoryNode> pageResult = categoryAdminService.page(pageNo, pageSize, filterByParent, normalizedParentId, keyword, isEnabled);
        List<CategoryNodeRespond> data = pageResult.items().stream()
                .map(CategoryNodeRespond::from)
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(pageNo)
                .size(pageSize)
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ResponseEntity<Result<CategoryNodeRespond>> create(@RequestBody CategoryUpsertRequest request) {
        request.validate();
        CategoryNode node = categoryAdminService.create(toCommand(request));
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(CategoryNodeRespond.from(node)));
    }

    /**
     * 分类详情
     */
    @GetMapping("/{category_id}")
    public ResponseEntity<Result<CategoryNodeRespond>> detail(@PathVariable("category_id") Long categoryId) {
        CategoryNode node = categoryAdminService.detail(categoryId);
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * 更新分类
     */
    @PatchMapping("/{category_id}")
    public ResponseEntity<Result<CategoryNodeRespond>> update(@PathVariable("category_id") Long categoryId,
                                                              @RequestBody CategoryUpsertRequest request) {
        request.validate();
        CategoryNode node = categoryAdminService.update(categoryId, toCommand(request));
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * upsert 分类多语言
     */
    @PatchMapping("/{category_id}/i18n")
    public ResponseEntity<Result<CategoryNodeRespond>> upsertI18n(@PathVariable("category_id") Long categoryId,
                                                                  @RequestBody List<CategoryI18nPayload> payloads) {
        List<CategoryI18nPayload> safePayloads = payloads == null ? List.of() : payloads;
        safePayloads.forEach(payload -> {
            if (payload != null)
                payload.validate();
        });
        CategoryNode node = categoryAdminService.upsertI18n(categoryId, mapI18nPayloads(safePayloads));
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * 启用或禁用分类
     */
    @PatchMapping("/{category_id}/enable")
    public ResponseEntity<Result<CategoryNodeRespond>> toggleEnable(@PathVariable("category_id") Long categoryId,
                                                                    @RequestBody ToggleEnableRequest request) {
        request.validate();
        CategoryNode node = categoryAdminService.toggleEnable(categoryId, Boolean.TRUE.equals(request.getIsEnabled()));
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * 将请求转换为命令
     */
    private CategoryUpsertCommand toCommand(CategoryUpsertRequest request) {
        List<CategoryI18n> i18nList = mapI18nPayloads(request.getI18n());
        return new CategoryUpsertCommand(request.getName(), request.getSlug(), request.getParentId(), request.getSortOrder(),
                request.getBrand(), request.getIsEnabled(), i18nList);
    }

    /**
     * 映射 i18n 请求
     */
    private List<CategoryI18n> mapI18nPayloads(List<CategoryI18nPayload> payloads) {
        if (payloads == null)
            return null;
        return payloads.stream()
                .filter(Objects::nonNull)
                .map(item -> CategoryI18n.of(item.getLocale(), item.getName(), item.getSlug(), item.getBrand()))
                .toList();
    }
}

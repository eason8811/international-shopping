package shopping.international.trigger.controller.products;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.CategoryI18nPayload;
import shopping.international.api.req.products.CategoryUpsertRequest;
import shopping.international.api.req.products.ToggleEnableRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.AdminCategoryNodeRespond;
import shopping.international.api.resp.products.CategoryOperationRespond;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.service.products.ICategoryService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端分类接口
 *
 * <p>覆盖分类的分页查询, 增删改以及 i18n, 启用状态管理</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/products/categories")
public class AdminCategoryController {

    /**
     * 分类领域服务
     */
    private final ICategoryService categoryService;

    /**
     * 分页列出分类
     *
     * @param page      页码, 默认 1
     * @param size      页大小, 默认 20
     * @param parentId  父分类过滤, 可空
     * @param keyword   关键词, 可空
     * @param isEnabled 启用过滤, 可空
     * @param request   HTTP 请求, 用于判断是否携带 parent_id
     * @return 分类列表及分页信息
     */
    @GetMapping
    public ResponseEntity<Result<List<AdminCategoryNodeRespond>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "parent_id", required = false) @Nullable Long parentId,
            @RequestParam(value = "keyword", required = false) @Nullable String keyword,
            @RequestParam(value = "is_enabled", required = false) @Nullable Boolean isEnabled,
            HttpServletRequest request) {
        boolean parentSpecified = request.getParameterMap().containsKey("parent_id");
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 20 : size;
        ICategoryService.PageResult result = categoryService.list(safePage, safeSize, parentSpecified, parentId, keyword, isEnabled);
        List<AdminCategoryNodeRespond> nodes = result.items().stream()
                .map(this::toAdminNode)
                .collect(Collectors.toList());
        Result.Meta meta = Result.Meta.builder()
                .page(safePage)
                .size(safeSize)
                .total(result.total())
                .build();
        return ResponseEntity.ok(Result.ok(nodes, meta));
    }

    /**
     * 创建分类
     *
     * @param req 创建请求
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Result<AdminCategoryNodeRespond>> create(@RequestBody CategoryUpsertRequest req) {
        req.createValidate();
        List<CategoryI18n> i18nList = toCreateI18n(req.getI18n());
        Category created = categoryService.create(req.getName(), req.getSlug(), req.getParentId(),
                req.getSortOrder(), req.getIsEnabled(), i18nList);
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(toAdminNode(created)));
    }

    /**
     * 获取分类详情
     *
     * @param categoryId 分类 ID
     * @return 分类详情
     */
    @GetMapping("/{category_id}")
    public ResponseEntity<Result<AdminCategoryNodeRespond>> detail(@PathVariable("category_id") Long categoryId) {
        Category category = categoryService.get(categoryId);
        return ResponseEntity.ok(Result.ok(toAdminNode(category)));
    }

    /**
     * 更新分类
     *
     * @param categoryId 分类 ID
     * @param req        更新请求
     * @return 更新后的分类
     */
    @PatchMapping("/{category_id}")
    public ResponseEntity<Result<AdminCategoryNodeRespond>> update(@PathVariable("category_id") Long categoryId,
                                                                   @RequestBody CategoryUpsertRequest req) {
        req.updateValidate();
        List<ICategoryService.CategoryI18nPatch> patches = toPatchI18n(req.getI18n());
        Category updated = categoryService.update(categoryId, req.getName(), req.getSlug(), req.getParentId(),
                req.getSortOrder(), req.getIsEnabled(), patches);
        return ResponseEntity.ok(Result.ok(toAdminNode(updated)));
    }

    /**
     * 删除分类
     *
     * @param categoryId 分类 ID
     * @return 删除结果
     */
    @DeleteMapping("/{category_id}")
    public ResponseEntity<Result<CategoryOperationRespond>> delete(@PathVariable("category_id") Long categoryId) {
        categoryService.delete(categoryId);
        CategoryOperationRespond respond = CategoryOperationRespond.builder()
                .categoryId(categoryId)
                .deleted(true)
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 新增分类多语言
     *
     * @param categoryId 分类 ID
     * @param payload    多语言请求体
     * @return 变更后的 locale
     */
    @PostMapping("/{category_id}/i18n")
    public ResponseEntity<Result<Map<String, String>>> addI18n(@PathVariable("category_id") Long categoryId,
                                                               @RequestBody CategoryI18nPayload payload) {
        payload.createValidate();
        CategoryI18n i18n = CategoryI18n.of(payload.getLocale(), payload.getName(), payload.getSlug(), payload.getBrand());
        CategoryI18n saved = categoryService.addI18n(categoryId, i18n);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("locale", saved.getLocale())));
    }

    /**
     * 增量更新多语言
     *
     * @param categoryId 分类 ID
     * @param payload    多语言请求体
     * @return 变更后的 locale
     */
    @PatchMapping("/{category_id}/i18n")
    public ResponseEntity<Result<Map<String, String>>> updateI18n(@PathVariable("category_id") Long categoryId,
                                                                  @RequestBody CategoryI18nPayload payload) {
        payload.updateValidate();
        ICategoryService.CategoryI18nPatch patch = new ICategoryService.CategoryI18nPatch(
                payload.getLocale(), payload.getName(), payload.getSlug(), payload.getBrand());
        CategoryI18n updated = categoryService.updateI18n(categoryId, patch);
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("locale", updated.getLocale())));
    }

    /**
     * 切换启用状态
     *
     * @param categoryId 分类 ID
     * @param req        启用请求体
     * @return 新状态
     */
    @PatchMapping("/{category_id}/enable")
    public ResponseEntity<Result<Map<String, Boolean>>> toggle(@PathVariable("category_id") Long categoryId,
                                                               @RequestBody ToggleEnableRequest req) {
        req.validate();
        Category updated = categoryService.toggleStatus(categoryId, req.getIsEnabled());
        boolean enabled = updated.getStatus() == CategoryStatus.ENABLED;
        return ResponseEntity.ok(Result.ok(Collections.singletonMap("is_enabled", enabled)));
    }

    /**
     * 将聚合转为管理端节点
     *
     * @param category 聚合
     * @return 响应节点
     */
    private AdminCategoryNodeRespond toAdminNode(@NotNull Category category) {
        List<AdminCategoryNodeRespond.CategoryI18nPayloadRespond> i18nResponds = category.getI18nList() == null
                ? Collections.emptyList()
                : category.getI18nList().stream()
                .map(i18n -> AdminCategoryNodeRespond.CategoryI18nPayloadRespond.builder()
                        .locale(i18n.getLocale())
                        .name(i18n.getName())
                        .slug(i18n.getSlug())
                        .brand(i18n.getBrand())
                        .build())
                .collect(Collectors.toList());
        return AdminCategoryNodeRespond.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .level(category.getLevel())
                .path(category.getPath())
                .sortOrder(category.getSortOrder())
                .brand(category.getBrand())
                .children(new ArrayList<>())
                .isEnabled(category.getStatus() == CategoryStatus.ENABLED)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .i18nList(i18nResponds)
                .build();
    }

    /**
     * 将创建请求中的 i18n 转为值对象列表
     *
     * @param payloads 请求体多语言列表
     * @return 值对象列表
     */
    private List<CategoryI18n> toCreateI18n(@Nullable List<CategoryI18nPayload> payloads) {
        if (payloads == null)
            return null;
        return payloads.stream()
                .map(item -> CategoryI18n.of(item.getLocale(), item.getName(), item.getSlug(), item.getBrand()))
                .collect(Collectors.toList());
    }

    /**
     * 将更新请求中的 i18n 转为增量列表
     *
     * @param payloads 请求体多语言列表
     * @return 增量指令列表
     */
    private List<ICategoryService.CategoryI18nPatch> toPatchI18n(@Nullable List<CategoryI18nPayload> payloads) {
        if (payloads == null)
            return null;
        return payloads.stream()
                .map(item -> new ICategoryService.CategoryI18nPatch(item.getLocale(), item.getName(), item.getSlug(), item.getBrand()))
                .collect(Collectors.toList());
    }
}

package shopping.international.trigger.controller.products;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
     *
     * @param page      页码, 默认值为 1
     * @param size      每页显示的条目数, 默认值为 20, 最大不超过 100
     * @param parentId  父级分类 ID, 可选参数
     * @param keyword   查询关键词, 可选参数
     * @param isEnabled 是否启用, 可选参数
     * @return 包含分页结果的 ResponseEntity, 结果中包含分类节点列表和元数据信息
     */
    @GetMapping
    public ResponseEntity<Result<List<CategoryNodeRespond>>> page(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(value = "parent_id", required = false) String parentId,
                                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                                  @RequestParam(value = "is_enabled", required = false) Boolean isEnabled,
                                                                  HttpServletRequest request) {
        int pageNo = page <= 0 ? 1 : page;
        int pageSize = size <= 0 ? 20 : Math.min(size, 100);
        // 不带 parent_id 不过滤, 带了但为空表示筛选 parent_id IS NULL
        boolean filterByParent = request.getParameterMap().containsKey("parent_id");
        Long normalizedParentId = StringUtils.hasText(parentId) ? Long.valueOf(parentId) : null;
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
     * 创建分类, 并返回创建成功的分类节点信息
     *
     * @param request 包含分类创建所需信息的请求体, 如分类名称, slug, 父级分类 ID (可选), 排序号, 品牌, 是否启用状态以及多语言信息等
     * @return 包含创建成功分类节点信息的 ResponseEntity, 如果分类创建成功, 则返回 201 Created 状态码及分类节点响应对象
     */
    @PostMapping
    public ResponseEntity<Result<CategoryNodeRespond>> create(@RequestBody CategoryUpsertRequest request) {
        request.validate();
        CategoryNode node = categoryAdminService.create(toCommand(request));
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(CategoryNodeRespond.from(node)));
    }

    /**
     * 获取指定 ID 的分类详情
     *
     * @param categoryId 分类的唯一标识符, 用于定位具体的分类节点
     * @return 包含分类节点详细信息的 ResponseEntity, 如果成功获取到数据, 则返回 200 OK 状态码及分类节点响应对象
     */
    @GetMapping("/{category_id}")
    public ResponseEntity<Result<CategoryNodeRespond>> detail(@PathVariable("category_id") Long categoryId) {
        CategoryNode node = categoryAdminService.detail(categoryId);
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * 更新指定 ID 的分类信息
     *
     * @param categoryId 分类的唯一标识符, 用于定位具体的分类节点
     * @param request    包含更新分类所需信息的请求体, 如分类名称, slug, 父级分类 ID (可选), 排序号, 品牌, 是否启用状态以及多语言信息等
     * @return 包含更新后分类节点详细信息的 ResponseEntity, 如果成功更新数据, 则返回 200 OK 状态码及分类节点响应对象
     */
    @PatchMapping("/{category_id}")
    public ResponseEntity<Result<CategoryNodeRespond>> update(@PathVariable("category_id") Long categoryId,
                                                              @RequestBody CategoryUpsertRequest request) {
        request.validate();
        CategoryNode node = categoryAdminService.update(categoryId, toCommand(request));
        return ResponseEntity.ok(Result.ok(CategoryNodeRespond.from(node)));
    }

    /**
     * 更新或插入指定分类的多语言信息, 此方法接收一个分类 ID 和一组多语言信息负载, 并更新或插入这些信息到对应的分类节点中
     *
     * @param categoryId 分类的唯一标识符, 用于定位具体的分类节点
     * @param payloads   包含要更新或插入的多语言信息列表, 每个元素代表一种语言下的分类名称、slug 和品牌等信息
     * @return 包含操作结果的 ResponseEntity, 如果成功执行了更新或插入操作, 则返回 200 OK 状态码及更新后的分类节点响应对象
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
     * 将 {@link CategoryUpsertRequest} 请求对象转换为 {@link CategoryUpsertCommand} 命令对象
     *
     * @param request 包含分类创建或更新所需信息的请求体, 如分类名称, slug, 父级分类 ID (可选), 排序号, 品牌, 是否启用状态以及多语言信息等
     * @return 一个基于请求数据构建的 {@link CategoryUpsertCommand} 对象, 用于执行实际的业务逻辑操作
     */
    private CategoryUpsertCommand toCommand(CategoryUpsertRequest request) {
        List<CategoryI18n> i18nList = mapI18nPayloads(request.getI18n());
        return new CategoryUpsertCommand(request.getName(), request.getSlug(), request.getParentId(), request.getSortOrder(),
                request.getBrand(), request.getIsEnabled(), i18nList);
    }

    /**
     * 将 <code>CategoryI18nPayload</code> 对象列表映射为 <code>CategoryI18n</code> 对象列表
     *
     * @param payloads 一个包含多个 <code>CategoryI18nPayload</code> 对象的列表, 每个对象代表一种语言下的分类信息, 包括名称、slug 和品牌等
     * @return 返回一个新的 <code>List<CategoryI18n></code>, 其中每个元素都是从对应的 <code>CategoryI18nPayload</code> 转换而来的 <code>CategoryI18n</code> 对象。如果输入参数为 null, 则返回 null
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

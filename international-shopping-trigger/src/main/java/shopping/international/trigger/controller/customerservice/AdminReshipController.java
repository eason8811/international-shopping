package shopping.international.trigger.controller.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.req.customerservice.AdminReshipBindShipmentsRequest;
import shopping.international.api.req.customerservice.AdminReshipCreateRequest;
import shopping.international.api.req.customerservice.AdminReshipPatchRequest;
import shopping.international.api.req.customerservice.AdminReshipStatusTransitionRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.ReshipDetailRespond;
import shopping.international.api.resp.customerservice.ReshipShipmentRespond;
import shopping.international.api.resp.customerservice.ReshipSummaryRespond;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminReshipPageCriteria;
import shopping.international.domain.model.vo.customerservice.ReshipCreateItemCommand;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.customerservice.IAdminReshipService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static shopping.international.trigger.controller.customerservice.support.CustomerServiceControllerSupport.parseDateTime;
import static shopping.international.trigger.controller.customerservice.support.CustomerServiceControllerSupport.parseEnumIgnoreBlank;
import static shopping.international.trigger.controller.customerservice.support.CustomerServiceControllerSupport.requireCurrentUserId;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧补发单控制器, 提供补发单创建, 检索, 详情, 更新, 状态推进, 物流绑定能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin")
public class AdminReshipController {

    /**
     * 管理侧补发单领域服务
     */
    private final IAdminReshipService adminReshipService;
    /**
     * 币种配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 管理侧基于工单创建补发单
     *
     * @param ticketId       工单 ID
     * @param idempotencyKey 幂等键
     * @param request        创建请求
     * @return 补发单详情
     */
    @PostMapping("/tickets/{ticket_id}/reships")
    public ResponseEntity<Result<ReshipDetailRespond>> createReshipByTicket(@PathVariable("ticket_id") Long ticketId,
                                                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                            @RequestBody AdminReshipCreateRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        List<ReshipCreateItemCommand> itemCommands = request.getItems().stream()
                .map(item -> new ReshipCreateItemCommand(
                                item.getOrderItemId(),
                                item.getSkuId(),
                                item.getQuantity()
                        )
                )
                .toList();
        Long actorUserId = requireCurrentUserId();
        AfterSalesReship detail = adminReshipService.createReshipByTicket(
                actorUserId,
                ticketId,
                request.getOrderId(),
                request.getReasonCode(),
                request.getCurrency(),
                request.getNote(),
                itemCommands,
                normalizedIdempotencyKey
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(AdminReshipRespondAssembler.toDetailRespond(detail)));
    }

    /**
     * 管理侧分页检索补发单
     *
     * @param page        页码
     * @param size        每页条数
     * @param reshipNo    补发单号
     * @param orderId     订单 ID
     * @param ticketId    工单 ID
     * @param status      补发状态
     * @param reasonCode  补发原因编码
     * @param createdFrom 创建时间起始
     * @param createdTo   创建时间结束
     * @return 补发单摘要分页结果
     */
    @GetMapping("/reships")
    public ResponseEntity<Result<List<ReshipSummaryRespond>>> pageReships(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                          @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                          @RequestParam(value = "reship_no", required = false) String reshipNo,
                                                                          @RequestParam(value = "order_id", required = false) Long orderId,
                                                                          @RequestParam(value = "ticket_id", required = false) Long ticketId,
                                                                          @RequestParam(value = "status", required = false) String status,
                                                                          @RequestParam(value = "reason_code", required = false) String reasonCode,
                                                                          @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                          @RequestParam(value = "created_to", required = false) String createdTo) {
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 20 : size;
        require(normalizedPage >= 1, "page 必须大于等于 1");
        require(normalizedSize >= 1 && normalizedSize <= 200, "size 必须在 1 到 200 之间");

        String normalizedReshipNo = normalizeNullableField(
                reshipNo,
                "reship_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "reship_no 长度需在 10 到 32 之间"
        );
        if (orderId != null)
            require(orderId >= 1, "order_id 必须大于等于 1");
        if (ticketId != null)
            require(ticketId >= 1, "ticket_id 必须大于等于 1");

        ReshipStatus statusEnum = parseEnumIgnoreBlank(status, ReshipStatus.class, "status");
        ReshipReasonCode reasonCodeEnum = parseEnumIgnoreBlank(reasonCode, ReshipReasonCode.class, "reason_code");
        LocalDateTime createdFromTime = parseDateTime(createdFrom);
        LocalDateTime createdToTime = parseDateTime(createdTo);
        if (createdFromTime != null && createdToTime != null)
            require(!createdFromTime.isAfter(createdToTime), "created_from 不能晚于 created_to");

        AdminReshipPageCriteria criteria = AdminReshipPageCriteria.builder()
                .reshipNo(normalizedReshipNo)
                .orderId(orderId)
                .ticketId(ticketId)
                .status(statusEnum)
                .reasonCode(reasonCodeEnum)
                .createdFrom(createdFromTime)
                .createdTo(createdToTime)
                .build();
        PageQuery pageQuery = PageQuery.of(normalizedPage, normalizedSize, 200);
        PageResult<AfterSalesReship> pageResult = adminReshipService.pageReships(criteria, pageQuery);

        List<ReshipSummaryRespond> data = pageResult.items().stream()
                .map(AdminReshipRespondAssembler::toSummaryRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageResult.total())
                        .build()
        ));
    }

    /**
     * 管理侧查询补发单详情
     *
     * @param reshipId 补发单 ID
     * @return 补发单详情
     */
    @GetMapping("/reships/{reship_id}")
    public ResponseEntity<Result<ReshipDetailRespond>> getReshipDetail(@PathVariable("reship_id") Long reshipId) {
        require(reshipId != null && reshipId >= 1, "reship_id 必须大于等于 1");
        AfterSalesReship detail = adminReshipService.getReshipDetail(reshipId);
        return ResponseEntity.ok(Result.ok(AdminReshipRespondAssembler.toDetailRespond(detail)));
    }

    /**
     * 管理侧更新补发单元数据
     *
     * @param reshipId       补发单 ID
     * @param idempotencyKey 幂等键
     * @param request        更新请求
     * @return 更新后的补发单详情
     */
    @PatchMapping("/reships/{reship_id}")
    public ResponseEntity<Result<ReshipDetailRespond>> patchReship(@PathVariable("reship_id") Long reshipId,
                                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                   @RequestBody AdminReshipPatchRequest request) {
        require(reshipId != null && reshipId >= 1, "reship_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        String amountCurrency = request.getCurrency();
        if ((request.getItemsCost() != null || request.getShippingCost() != null)
                && (amountCurrency == null || amountCurrency.isBlank()))
            amountCurrency = adminReshipService.getReshipDetail(reshipId).getCurrency();

        Long itemsCostMinor = null;
        Long shippingCostMinor = null;
        if (amountCurrency != null && !amountCurrency.isBlank()) {
            CurrencyConfig currencyConfig = currencyConfigService.get(amountCurrency);
            itemsCostMinor = toMinorOrNull(currencyConfig, request.getItemsCost(), "itemsCost");
            shippingCostMinor = toMinorOrNull(currencyConfig, request.getShippingCost(), "shippingCost");
        }

        Long actorUserId = requireCurrentUserId();
        AfterSalesReship detail = adminReshipService.patchReship(
                actorUserId,
                reshipId,
                request.getCurrency(),
                itemsCostMinor,
                shippingCostMinor,
                request.getNote(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminReshipRespondAssembler.toDetailRespond(detail)));
    }

    /**
     * 管理侧推进补发单状态
     *
     * @param reshipId       补发单 ID
     * @param idempotencyKey 幂等键
     * @param request        状态推进请求
     * @return 更新后的补发单详情
     */
    @PostMapping("/reships/{reship_id}/status")
    public ResponseEntity<Result<ReshipDetailRespond>> transitionReshipStatus(@PathVariable("reship_id") Long reshipId,
                                                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                              @RequestBody AdminReshipStatusTransitionRequest request) {
        require(reshipId != null && reshipId >= 1, "reship_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        AfterSalesReship detail = adminReshipService.transitionReshipStatus(
                actorUserId,
                reshipId,
                request.getToStatus(),
                request.getNote(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminReshipRespondAssembler.toDetailRespond(detail)));
    }

    /**
     * 管理侧查询补发单关联物流单
     *
     * @param reshipId 补发单 ID
     * @return 关联物流单列表
     */
    @GetMapping("/reships/{reship_id}/shipments")
    public ResponseEntity<Result<List<ReshipShipmentRespond>>> listReshipShipments(@PathVariable("reship_id") Long reshipId) {
        require(reshipId != null && reshipId >= 1, "reship_id 必须大于等于 1");
        List<ReshipShipment> shipmentList = adminReshipService.listReshipShipments(reshipId);
        List<ReshipShipmentRespond> data = shipmentList.stream()
                .map(AdminReshipRespondAssembler::toShipmentRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 管理侧绑定补发单和物流单
     *
     * @param reshipId       补发单 ID
     * @param idempotencyKey 幂等键
     * @param request        绑定请求
     * @return 绑定后的关联物流单列表
     */
    @PostMapping("/reships/{reship_id}/shipments")
    public ResponseEntity<Result<List<ReshipShipmentRespond>>> bindReshipShipments(@PathVariable("reship_id") Long reshipId,
                                                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                   @RequestBody AdminReshipBindShipmentsRequest request) {
        require(reshipId != null && reshipId >= 1, "reship_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        List<ReshipShipment> shipmentList = adminReshipService.bindReshipShipments(
                actorUserId,
                reshipId,
                request.getShipmentIds(),
                normalizedIdempotencyKey
        );
        List<ReshipShipmentRespond> data = shipmentList.stream()
                .map(AdminReshipRespondAssembler::toShipmentRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 将金额字符串转换为 Minor 形式, 空值返回 null
     *
     * @param currencyConfig 币种配置
     * @param raw            原始金额字符串
     * @param fieldName      字段名
     * @return Minor 形式金额
     */
    private @Nullable Long toMinorOrNull(@NotNull CurrencyConfig currencyConfig,
                                         @Nullable String raw,
                                         @NotNull String fieldName) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return currencyConfig.toMinorRounded(new BigDecimal(raw.strip()));
        } catch (IllegalParamException exception) {
            throw new IllegalParamException(fieldName + " 字段" + exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new IllegalParamException(fieldName + " 数值不合法");
        }
    }

}

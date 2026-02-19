package shopping.international.trigger.controller.shipping;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.shipping.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.shipping.AdminDispatchShipmentsRespond;
import shopping.international.api.resp.shipping.ShipmentDetailRespond;
import shopping.international.api.resp.shipping.ShipmentStatusLogRespond;
import shopping.international.api.resp.shipping.ShipmentSummaryRespond;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.*;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.shipping.IAdminShipmentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧物流控制器, 提供物流单管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin")
public class AdminShipmentController {

    /**
     * 管理侧物流领域服务
     */
    private final IAdminShipmentService adminShipmentService;
    /**
     * 币种配置服务
     */
    private final ICurrencyConfigService currencyService;

    /**
     * 管理侧分页查询物流单
     *
     * @param shipmentNo    物流单号
     * @param orderNo       订单号
     * @param orderId       订单主键
     * @param carrierCode   承运商编码
     * @param trackingNo    追踪号
     * @param extExternalId 外部物流单号
     * @param statusIn      状态筛选
     * @param updatedFrom   更新时间起点
     * @param updatedTo     更新时间终点
     * @param createdFrom   创建时间起点
     * @param createdTo     创建时间终点
     * @param page          页码
     * @param size          每页条数
     * @param sort          排序字段
     * @return 分页结果
     */
    @GetMapping("/shipments")
    public ResponseEntity<Result<List<ShipmentSummaryRespond>>> pageShipments(@RequestParam(value = "shipment_no", required = false) String shipmentNo,
                                                                              @RequestParam(value = "order_no", required = false) String orderNo,
                                                                              @RequestParam(value = "order_id", required = false) Long orderId,
                                                                              @RequestParam(value = "carrier_code", required = false) String carrierCode,
                                                                              @RequestParam(value = "tracking_no", required = false) String trackingNo,
                                                                              @RequestParam(value = "ext_external_id", required = false) String extExternalId,
                                                                              @RequestParam(value = "status_in", required = false) List<String> statusIn,
                                                                              @RequestParam(value = "updated_from", required = false) String updatedFrom,
                                                                              @RequestParam(value = "updated_to", required = false) String updatedTo,
                                                                              @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                              @RequestParam(value = "created_to", required = false) String createdTo,
                                                                              @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                              @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                              @RequestParam(value = "sort", required = false, defaultValue = "updated_at,desc") String sort) {
        AdminShipmentPageRequest request = new AdminShipmentPageRequest(
                shipmentNo,
                orderNo,
                orderId,
                carrierCode,
                trackingNo,
                extExternalId,
                statusIn,
                parseDateTime(updatedFrom),
                parseDateTime(updatedTo),
                parseDateTime(createdFrom),
                parseDateTime(createdTo),
                page,
                size,
                sort
        );
        request.validate();

        ShipmentPageCriteria criteria = ShipmentPageCriteria.builder()
                .shipmentNo(request.getShipmentNo())
                .orderNo(request.getOrderNo())
                .orderId(request.getOrderId())
                .carrierCode(request.getCarrierCode())
                .trackingNo(request.getTrackingNo())
                .extExternalId(request.getExtExternalId())
                .statusIn(request.getStatusIn() == null ? null : request.getStatusIn().stream().map(ShipmentStatus::valueOf).toList())
                .updatedFrom(request.getUpdatedFrom())
                .updatedTo(request.getUpdatedTo())
                .createdFrom(request.getCreatedFrom())
                .createdTo(request.getCreatedTo())
                .sortField(extractSortField(request.getSort()))
                .sortDirection(extractSortDirection(request.getSort()))
                .build();

        PageQuery pageQuery = PageQuery.of(request.getPage(), request.getSize(), 200);
        PageResult<ShipmentSummaryView> result = adminShipmentService.pageShipments(criteria, pageQuery);
        List<ShipmentSummaryRespond> data = result.items().stream()
                .map(ShipmentRespondAssembler::toSummaryRespond)
                .toList();

        return ResponseEntity.ok(
                Result.ok(
                        data,
                        Result.Meta.builder()
                                .page(pageQuery.page())
                                .size(pageQuery.size())
                                .total(result.total())
                                .build()
                )
        );
    }

    /**
     * 管理侧查询物流单详情
     *
     * @param shipmentId 物流单主键
     * @return 物流单详情
     */
    @GetMapping("/shipments/{shipment_id}")
    public ResponseEntity<Result<ShipmentDetailRespond>> getShipmentDetail(@PathVariable("shipment_id") Long shipmentId) {
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId >= 1, "shipmentId 必须大于等于 1");

        Shipment shipment = adminShipmentService.getShipmentDetail(shipmentId);
        return ResponseEntity.ok(Result.ok(ShipmentRespondAssembler.toShipmentDetailRespond(shipment)));
    }

    /**
     * 管理侧回填物流面单信息
     *
     * @param shipmentId     物流单主键
     * @param idempotencyKey 幂等键
     * @param request        请求体
     * @return 回填后的物流单详情
     */
    @PostMapping("/shipments/{shipment_id}/label")
    public ResponseEntity<Result<ShipmentDetailRespond>> fillShipmentLabel(@PathVariable("shipment_id") Long shipmentId,
                                                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                           @RequestBody AdminFillShipmentLabelRequest request) {
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId >= 1, "shipmentId 必须大于等于 1");
        request.validate();

        String normalizedIdempotencyKey = normalizeNotNullField(idempotencyKey, "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符");

        ShipmentDimension dimension = ShipmentDimension.ofText(
                request.getWeightKg(),
                request.getLengthCm(),
                request.getWidthCm(),
                request.getHeightCm()
        );
        if (!dimension.hasAnyValue())
            dimension = null;

        CurrencyConfig currencyConfig = currencyService.get(request.getCurrency());
        ShipmentLabel label = ShipmentLabel.of(
                request.getCarrierCode(),
                request.getCarrierName(),
                request.getServiceCode(),
                request.getTrackingNo(),
                request.getExtExternalId(),
                request.getLabelUrl(),
                dimension,
                currencyConfig.toMinorRounded(new BigDecimal(request.getDeclaredValue())),
                request.getCurrency()
        );

        Long actorUserId = requireCurrentUserId();
        String sourceRef = "admin:shipment:label:" + shipmentId + ":" + normalizedIdempotencyKey;
        Shipment shipment = adminShipmentService.fillShipmentLabel(
                shipmentId,
                label,
                request.getShipFromAddressId(),
                normalizedIdempotencyKey,
                sourceRef,
                actorUserId,
                "管理侧回填面单"
        );

        return ResponseEntity.ok(Result.ok(ShipmentRespondAssembler.toShipmentDetailRespond(shipment)));
    }

    /**
     * 管理侧批量发货
     *
     * @param idempotencyKey 幂等键
     * @param request        请求体
     * @return 发货结果
     */
    @PostMapping("/shipments/dispatch")
    public ResponseEntity<Result<AdminDispatchShipmentsRespond>> dispatchShipments(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                                   @RequestBody AdminDispatchShipmentsRequest request) {
        request.validate();
        String normalizedIdempotencyKey = normalizeNotNullField(idempotencyKey, "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符");

        Long actorUserId = requireCurrentUserId();
        String sourceRef = "admin:shipment:dispatch:" + normalizedIdempotencyKey;
        List<Shipment> shipments = adminShipmentService.dispatchShipments(
                request.getShipmentIds(),
                normalizedIdempotencyKey,
                sourceRef,
                request.getNote(),
                actorUserId
        );

        AdminDispatchShipmentsRespond respond = AdminDispatchShipmentsRespond.builder()
                .shipmentIds(shipments.stream().map(row -> row.getShipmentNo().getValue()).toList())
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 管理侧手工补建物流单
     *
     * @param idempotencyKey 幂等键
     * @param request        请求体
     * @return 创建后的物流单详情
     */
    @PostMapping("/shipments/manual-create")
    public ResponseEntity<Result<ShipmentDetailRespond>> manualCreateShipment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                              @RequestBody AdminManualCreateShipmentRequest request) {
        request.validate();
        String normalizedIdempotencyKey = normalizeNotNullField(idempotencyKey, "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符");

        CurrencyConfig currencyConfig = currencyService.get(request.getCurrency());
        Long actorUserId = requireCurrentUserId();
        ManualCreateShipmentCommand command = ManualCreateShipmentCommand.builder()
                .shipFromAddressId(request.getShipFromAddressId())
                .orderNo(OrderNo.of(request.getOrderNo()))
                .carrierCode(request.getCarrierCode())
                .carrierName(request.getCarrierName())
                .serviceCode(request.getServiceCode())
                .trackingNo(request.getTrackingNo())
                .extExternalId(request.getExtExternalId())
                .labelUrl(request.getLabelUrl())
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .declaredValue(currencyConfig.toMinorRounded(new BigDecimal(request.getDeclaredValue())))
                .currency(request.getCurrency())
                .note("管理侧手动创建物流单")
                .build();

        Shipment shipment = adminShipmentService.manualCreateShipment(command, normalizedIdempotencyKey, actorUserId);
        if (shipment.getId() == null)
            throw new IllegalParamException("物流单创建后缺少主键");

        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(ShipmentRespondAssembler.toShipmentDetailRespond(shipment)));
    }

    /**
     * 管理侧分页查询物流状态日志
     *
     * @param shipmentId    物流单主键
     * @param orderNo       订单号
     * @param fromStatus    起始状态
     * @param toStatus      目标状态
     * @param sourceType    来源类型
     * @param sourceRef     来源引用
     * @param carrierCode   承运商编码
     * @param trackingNo    追踪号
     * @param eventTimeFrom 事件时间起点
     * @param eventTimeTo   事件时间终点
     * @param createdFrom   创建时间起点
     * @param createdTo     创建时间终点
     * @param page          页码
     * @param size          每页条数
     * @param sort          排序
     * @return 状态日志分页结果
     */
    @GetMapping("/shipment-status-logs")
    public ResponseEntity<Result<List<ShipmentStatusLogRespond>>> pageStatusLogs(@RequestParam(value = "shipment_id", required = false) Long shipmentId,
                                                                                 @RequestParam(value = "order_no", required = false) String orderNo,
                                                                                 @RequestParam(value = "from_status", required = false) String fromStatus,
                                                                                 @RequestParam(value = "to_status", required = false) String toStatus,
                                                                                 @RequestParam(value = "source_type", required = false) String sourceType,
                                                                                 @RequestParam(value = "source_ref", required = false) String sourceRef,
                                                                                 @RequestParam(value = "carrier_code", required = false) String carrierCode,
                                                                                 @RequestParam(value = "tracking_no", required = false) String trackingNo,
                                                                                 @RequestParam(value = "event_time_from", required = false) String eventTimeFrom,
                                                                                 @RequestParam(value = "event_time_to", required = false) String eventTimeTo,
                                                                                 @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                                 @RequestParam(value = "created_to", required = false) String createdTo,
                                                                                 @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                                 @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                                 @RequestParam(value = "sort", required = false, defaultValue = "created_at,desc") String sort) {
        AdminShipmentStatusLogPageRequest request = new AdminShipmentStatusLogPageRequest(
                shipmentId,
                orderNo,
                fromStatus,
                toStatus,
                sourceType,
                sourceRef,
                carrierCode,
                trackingNo,
                parseDateTime(eventTimeFrom),
                parseDateTime(eventTimeTo),
                parseDateTime(createdFrom),
                parseDateTime(createdTo),
                page,
                size,
                sort
        );
        request.validate();

        ShipmentStatusLogPageCriteria criteria = ShipmentStatusLogPageCriteria.builder()
                .shipmentId(request.getShipmentId())
                .orderNo(request.getOrderNo())
                .fromStatus(request.getFromStatus() == null ? null : ShipmentStatus.valueOf(request.getFromStatus()))
                .toStatus(request.getToStatus() == null ? null : ShipmentStatus.valueOf(request.getToStatus()))
                .sourceType(request.getSourceType() == null ? null : ShipmentStatusEventSource.valueOf(request.getSourceType()))
                .sourceRef(request.getSourceRef())
                .carrierCode(request.getCarrierCode())
                .trackingNo(request.getTrackingNo())
                .eventTimeFrom(request.getEventTimeFrom())
                .eventTimeTo(request.getEventTimeTo())
                .createdFrom(request.getCreatedFrom())
                .createdTo(request.getCreatedTo())
                .sortField(extractSortField(request.getSort()))
                .sortDirection(extractSortDirection(request.getSort()))
                .build();

        PageQuery pageQuery = PageQuery.of(request.getPage(), request.getSize(), 200);
        PageResult<ShipmentStatusLog> result = adminShipmentService.pageStatusLogs(criteria, pageQuery);
        List<ShipmentStatusLogRespond> data = result.items().stream()
                .map(ShipmentRespondAssembler::toStatusLogRespond)
                .toList();

        return ResponseEntity.ok(
                Result.ok(
                        data,
                        Result.Meta.builder()
                                .page(pageQuery.page())
                                .size(pageQuery.size())
                                .total(result.total())
                                .build()
                )
        );
    }

    /**
     * 解析排序字段
     *
     * @param sort 排序表达式
     * @return 排序字段
     */
    private static @NotNull String extractSortField(@NotNull String sort) {
        String[] parts = sort.split(",");
        if (parts.length != 2)
            throw new IllegalParamException("sort 格式需为 field,asc|desc");
        return parts[0].strip();
    }

    /**
     * 解析排序方向
     *
     * @param sort 排序表达式
     * @return 排序方向
     */
    private static @NotNull String extractSortDirection(@NotNull String sort) {
        String[] parts = sort.split(",");
        if (parts.length != 2)
            throw new IllegalParamException("sort 格式需为 field,asc|desc");
        return parts[1].strip();
    }

    /**
     * 解析时间字符串, 支持 ISO_OFFSET_DATE_TIME 和 ISO_LOCAL_DATE_TIME
     *
     * @param value 时间字符串
     * @return 解析后的时间
     */
    private static @Nullable LocalDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        String text = value.strip();
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception exception) {
                throw new IllegalParamException("时间格式不合法: " + value);
            }
        }
    }

    /**
     * 从安全上下文读取当前用户主键
     *
     * @return 当前用户主键
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId)
            return userId;
        if (principal instanceof String userId)
            return Long.parseLong(userId);
        throw new AccountException("无法解析当前用户");
    }
}

package shopping.international.trigger.controller.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.customerservice.TicketCloseRequest;
import shopping.international.api.req.customerservice.TicketCreateRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.TicketCreateDataRespond;
import shopping.international.api.resp.customerservice.UserTicketDetailRespond;
import shopping.international.api.resp.customerservice.UserTicketSummaryRespond;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketCreateCommand;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.customerservice.IUserTicketService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 用户侧工单控制器, 提供工单列表, 创建, 详情, 关闭能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/tickets")
public class UserTicketController {

    /**
     * 用户侧工单领域服务
     */
    private final IUserTicketService userTicketService;
    /**
     * 币种配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 用户侧分页查询工单列表
     *
     * @param page        页码
     * @param size        每页条数
     * @param status      工单状态筛选
     * @param issueType   问题类型筛选
     * @param orderNo     订单号筛选
     * @param shipmentNo  物流单号筛选
     * @param createdFrom 创建时间起始
     * @param createdTo   创建时间结束
     * @return 工单摘要分页结果
     */
    @GetMapping
    public ResponseEntity<Result<List<UserTicketSummaryRespond>>> listMyTickets(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                                @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                                @RequestParam(value = "status", required = false) String status,
                                                                                @RequestParam(value = "issue_type", required = false) String issueType,
                                                                                @RequestParam(value = "order_no", required = false) String orderNo,
                                                                                @RequestParam(value = "shipment_no", required = false) String shipmentNo,
                                                                                @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                                @RequestParam(value = "created_to", required = false) String createdTo) {
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 20 : size;
        require(normalizedPage >= 1, "page 必须大于等于 1");
        require(normalizedSize >= 1, "size 必须大于等于 1");

        TicketStatus statusEnum = parseTicketStatus(status);
        TicketIssueType issueTypeEnum = parseTicketIssueType(issueType);
        String normalizedOrderNo = normalizeNullableField(
                orderNo,
                "order_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "order_no 长度需在 10 到 32 之间"
        );
        String normalizedShipmentNo = normalizeNullableField(
                shipmentNo,
                "shipment_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "shipment_no 长度需在 10 到 32 之间"
        );
        LocalDateTime createdFromTime = parseDateTime(createdFrom);
        LocalDateTime createdToTime = parseDateTime(createdTo);
        if (createdFromTime != null && createdToTime != null)
            require(!createdFromTime.isAfter(createdToTime), "created_from 不能晚于 created_to");

        Long userId = requireCurrentUserId();
        PageQuery pageQuery = PageQuery.of(normalizedPage, normalizedSize, 200);
        PageResult<UserTicketSummaryView> pageResult = userTicketService.listMyTickets(
                userId,
                pageQuery,
                statusEnum,
                issueTypeEnum,
                normalizedOrderNo,
                normalizedShipmentNo,
                createdFromTime,
                createdToTime
        );

        List<UserTicketSummaryRespond> data = pageResult.items().stream()
                .map(item -> {
                    CurrencyConfig payCurrencyConfig = currencyConfigService.get(item.payCurrency());
                    return UserTicketRespondAssembler.toSummaryRespond(item, payCurrencyConfig);
                })
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
     * 用户侧创建工单
     *
     * @param idempotencyKey 幂等键
     * @param request        创建请求
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Result<TicketCreateDataRespond>> createMyTicket(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                          @RequestBody TicketCreateRequest request) {
        request.validate();
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        Long userId = requireCurrentUserId();
        TicketCreateCommand command = new TicketCreateCommand(
                request.getOrderId(),
                request.getOrderItemId(),
                request.getShipmentId(),
                request.getIssueType(),
                request.getTitle(),
                request.getDescription(),
                request.getAttachments(),
                request.getEvidence(),
                request.getRequestedRefundAmount(),
                request.getCurrency()
        );
        UserTicketCreateResult created = userTicketService.createMyTicket(userId, command, normalizedIdempotencyKey);
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(UserTicketRespondAssembler.toCreateDataRespond(created)));
    }

    /**
     * 用户侧查询工单详情
     *
     * @param ticketNo 工单编号
     * @return 工单详情
     */
    @GetMapping("/{ticket_no}")
    public ResponseEntity<Result<UserTicketDetailRespond>> getMyTicketDetail(@PathVariable("ticket_no") String ticketNo) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );

        Long userId = requireCurrentUserId();
        UserTicketDetailView detailView = userTicketService.getMyTicketDetail(userId, TicketNo.of(normalizedTicketNo));
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        UserTicketDetailRespond respond = UserTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig);
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 用户侧关闭工单
     *
     * @param ticketNo       工单编号
     * @param idempotencyKey 幂等键
     * @param request        关闭请求
     * @return 关闭后的工单详情
     */
    @PostMapping("/{ticket_no}/close")
    public ResponseEntity<Result<UserTicketDetailRespond>> closeMyTicket(@PathVariable("ticket_no") String ticketNo,
                                                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                         @RequestBody(required = false) TicketCloseRequest request) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        TicketCloseRequest closeRequest = request == null ? new TicketCloseRequest() : request;
        closeRequest.validate();

        Long userId = requireCurrentUserId();
        UserTicketDetailView detailView = userTicketService.closeMyTicket(
                userId,
                TicketNo.of(normalizedTicketNo),
                closeRequest.getNote(),
                normalizedIdempotencyKey
        );
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        UserTicketDetailRespond respond = UserTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig);
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 解析工单状态查询参数
     *
     * @param status 工单状态文本
     * @return 工单状态枚举
     */
    private @Nullable TicketStatus parseTicketStatus(@Nullable String status) {
        if (status == null || status.isBlank())
            return null;
        String normalized = status.strip().toUpperCase(Locale.ROOT);
        try {
            return TicketStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalParamException("status 不合法: " + status);
        }
    }

    /**
     * 解析问题类型查询参数
     *
     * @param issueType 问题类型文本
     * @return 问题类型枚举
     */
    private @Nullable TicketIssueType parseTicketIssueType(@Nullable String issueType) {
        if (issueType == null || issueType.isBlank())
            return null;
        String normalized = issueType.strip().toUpperCase(Locale.ROOT);
        try {
            return TicketIssueType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalParamException("issue_type 不合法: " + issueType);
        }
    }

    /**
     * 解析时间文本, 支持 ISO_OFFSET_DATE_TIME 和 yyyy-MM-dd HH:mm:ss
     *
     * @param value 时间文本
     * @return 本地时间
     */
    private @Nullable LocalDateTime parseDateTime(@Nullable String value) {
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
    private @NotNull Long requireCurrentUserId() {
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

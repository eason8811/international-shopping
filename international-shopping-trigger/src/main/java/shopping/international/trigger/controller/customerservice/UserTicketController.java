package shopping.international.trigger.controller.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.req.customerservice.TicketCloseRequest;
import shopping.international.api.req.customerservice.TicketCreateRequest;
import shopping.international.api.req.customerservice.TicketMessageCreateRequest;
import shopping.international.api.req.customerservice.TicketMessageRecallRequest;
import shopping.international.api.req.customerservice.TicketMessageUpdateRequest;
import shopping.international.api.req.customerservice.TicketReadRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.ShipmentSummaryRespond;
import shopping.international.api.resp.customerservice.TicketCreateDataRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.api.resp.customerservice.TicketStatusLogRespond;
import shopping.international.api.resp.customerservice.UserTicketDetailRespond;
import shopping.international.api.resp.customerservice.UserTicketSummaryRespond;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketCreateCommand;
import shopping.international.domain.model.vo.customerservice.TicketMessageNo;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketMessageView;
import shopping.international.domain.model.vo.customerservice.UserTicketReadUpdateView;
import shopping.international.domain.model.vo.customerservice.UserTicketShipmentSummaryView;
import shopping.international.domain.model.vo.customerservice.UserTicketStatusLogView;
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
 * 用户侧工单控制器, 提供工单列表, 创建, 详情, 关闭, 消息, 已读, 状态日志, 补发物流能力
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
     * 用户侧查询工单消息列表
     *
     * @param ticketNo 工单编号
     * @param beforeId 向前翻页锚点
     * @param size     返回条数
     * @param afterId  向后增量锚点
     * @param order    排序方向
     * @return 消息列表
     */
    @GetMapping("/{ticket_no}/messages")
    public ResponseEntity<Result<List<TicketMessageRespond>>> listMyTicketMessages(@PathVariable("ticket_no") String ticketNo,
                                                                                   @RequestParam(value = "before_id", required = false) Long beforeId,
                                                                                   @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                                   @RequestParam(value = "after_id", required = false) Long afterId,
                                                                                   @RequestParam(value = "order", required = false, defaultValue = "desc") String order) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );
        int normalizedSize = size == null ? 20 : size;
        require(normalizedSize >= 1 && normalizedSize <= 100, "size 必须在 1 到 100 之间");
        if (beforeId != null)
            require(beforeId >= 1, "before_id 必须大于等于 1");
        if (afterId != null)
            require(afterId >= 1, "after_id 必须大于等于 1");

        boolean ascOrder = parseMessageOrder(order);

        Long userId = requireCurrentUserId();
        List<UserTicketMessageView> messageViewList = userTicketService.listMyTicketMessages(
                userId,
                TicketNo.of(normalizedTicketNo),
                beforeId,
                afterId,
                ascOrder,
                normalizedSize
        );
        List<TicketMessageRespond> data = messageViewList.stream()
                .map(UserTicketRespondAssembler::toMessageRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 用户侧发送工单消息
     *
     * @param ticketNo       工单编号
     * @param idempotencyKey 幂等键
     * @param request        消息发送请求
     * @return 发送后的消息
     */
    @PostMapping("/{ticket_no}/messages")
    public ResponseEntity<Result<TicketMessageRespond>> createMyTicketMessage(@PathVariable("ticket_no") String ticketNo,
                                                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                              @RequestBody TicketMessageCreateRequest request) {
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
        request.validate();

        Long userId = requireCurrentUserId();
        UserTicketMessageView created = userTicketService.createMyTicketMessage(
                userId,
                TicketNo.of(normalizedTicketNo),
                request.getMessageType(),
                request.getContent(),
                request.getAttachments(),
                request.getClientMessageId(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(UserTicketRespondAssembler.toMessageRespond(created)));
    }

    /**
     * 用户侧编辑消息
     *
     * @param ticketNo       工单编号
     * @param messageNo      消息编号
     * @param idempotencyKey 幂等键
     * @param request        消息编辑请求
     * @return 编辑后的消息
     */
    @PatchMapping("/{ticket_no}/messages/{message_no}")
    public ResponseEntity<Result<TicketMessageRespond>> updateMyTicketMessage(@PathVariable("ticket_no") String ticketNo,
                                                                              @PathVariable("message_no") String messageNo,
                                                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                              @RequestBody TicketMessageUpdateRequest request) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );
        String normalizedMessageNo = normalizeNotNullField(
                messageNo,
                "message_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "message_no 长度需在 10 到 32 之间"
        );
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long userId = requireCurrentUserId();
        UserTicketMessageView updated = userTicketService.editMyTicketMessage(
                userId,
                TicketNo.of(normalizedTicketNo),
                TicketMessageNo.of(normalizedMessageNo),
                request.getContent(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(UserTicketRespondAssembler.toMessageRespond(updated)));
    }

    /**
     * 用户侧撤回消息
     *
     * @param ticketNo       工单编号
     * @param messageNo      消息编号
     * @param idempotencyKey 幂等键
     * @param request        撤回请求
     * @return 撤回后的消息
     */
    @PostMapping("/{ticket_no}/messages/{message_no}/recall")
    public ResponseEntity<Result<TicketMessageRespond>> recallMyTicketMessage(@PathVariable("ticket_no") String ticketNo,
                                                                              @PathVariable("message_no") String messageNo,
                                                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                              @RequestBody(required = false) TicketMessageRecallRequest request) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );
        String normalizedMessageNo = normalizeNotNullField(
                messageNo,
                "message_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "message_no 长度需在 10 到 32 之间"
        );
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        TicketMessageRecallRequest recallRequest = request == null ? new TicketMessageRecallRequest() : request;
        recallRequest.validate();

        Long userId = requireCurrentUserId();
        UserTicketMessageView recalled = userTicketService.recallMyTicketMessage(
                userId,
                TicketNo.of(normalizedTicketNo),
                TicketMessageNo.of(normalizedMessageNo),
                recallRequest.getReason(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(UserTicketRespondAssembler.toMessageRespond(recalled)));
    }

    /**
     * 用户侧标记工单消息已读
     *
     * @param ticketNo       工单编号
     * @param idempotencyKey 幂等键
     * @param request        已读请求
     * @return 已读位点事件数据
     */
    @PostMapping("/{ticket_no}/read")
    public ResponseEntity<Result<CsWsTicketReadUpdatedEventDataRespond>> markMyTicketRead(@PathVariable("ticket_no") String ticketNo,
                                                                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                          @RequestBody TicketReadRequest request) {
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
        request.validate();

        Long userId = requireCurrentUserId();
        UserTicketReadUpdateView updated = userTicketService.markMyTicketRead(
                userId,
                TicketNo.of(normalizedTicketNo),
                request.getLastReadMessageId(),
                normalizedIdempotencyKey
        );
        CsWsTicketReadUpdatedEventDataRespond respond = UserTicketRespondAssembler.toReadUpdatedEventDataRespond(updated);
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 用户侧查询工单状态流转日志
     *
     * @param ticketNo 工单编号
     * @param page     页码
     * @param size     每页条数
     * @return 状态日志分页结果
     */
    @GetMapping("/{ticket_no}/status-logs")
    public ResponseEntity<Result<List<TicketStatusLogRespond>>> listMyTicketStatusLogs(@PathVariable("ticket_no") String ticketNo,
                                                                                       @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                                       @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );

        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 20 : size;
        require(normalizedPage >= 1, "page 必须大于等于 1");
        require(normalizedSize >= 1 && normalizedSize <= 200, "size 必须在 1 到 200 之间");

        Long userId = requireCurrentUserId();
        PageQuery pageQuery = PageQuery.of(normalizedPage, normalizedSize, 200);
        PageResult<UserTicketStatusLogView> pageResult = userTicketService.listMyTicketStatusLogs(
                userId,
                TicketNo.of(normalizedTicketNo),
                pageQuery
        );
        List<TicketStatusLogRespond> data = pageResult.items().stream()
                .map(UserTicketRespondAssembler::toStatusLogRespond)
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
     * 用户侧查询工单下的补发单关联物流单列表
     *
     * @param ticketNo 工单编号
     * @return 物流摘要列表
     */
    @GetMapping("/{ticket_no}/reships")
    public ResponseEntity<Result<List<ShipmentSummaryRespond>>> listMyTicketReships(@PathVariable("ticket_no") String ticketNo) {
        String normalizedTicketNo = normalizeNotNullField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );

        Long userId = requireCurrentUserId();
        List<UserTicketShipmentSummaryView> shipmentViewList = userTicketService.listMyTicketReshipShipments(
                userId,
                TicketNo.of(normalizedTicketNo)
        );
        List<ShipmentSummaryRespond> data = shipmentViewList.stream()
                .map(UserTicketRespondAssembler::toShipmentSummaryRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
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
     * 解析消息排序参数
     *
     * @param order 排序方向
     * @return true 表示升序, false 表示降序
     */
    private boolean parseMessageOrder(@Nullable String order) {
        if (order == null || order.isBlank())
            return false;
        String normalized = order.strip().toLowerCase(Locale.ROOT);
        if (!"asc".equals(normalized) && !"desc".equals(normalized))
            throw new IllegalParamException("order 只支持 asc 或 desc");
        return "asc".equals(normalized);
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

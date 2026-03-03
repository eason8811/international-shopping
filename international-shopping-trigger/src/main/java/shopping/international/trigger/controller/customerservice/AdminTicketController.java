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
import shopping.international.api.req.customerservice.AdminTicketAssignRequest;
import shopping.international.api.req.customerservice.AdminTicketParticipantCreateRequest;
import shopping.international.api.req.customerservice.AdminTicketParticipantPatchRequest;
import shopping.international.api.req.customerservice.AdminTicketPatchRequest;
import shopping.international.api.req.customerservice.TicketMessageCreateRequest;
import shopping.international.api.req.customerservice.TicketMessageRecallRequest;
import shopping.international.api.req.customerservice.TicketMessageUpdateRequest;
import shopping.international.api.req.customerservice.TicketReadRequest;
import shopping.international.api.req.customerservice.TicketStatusTransitionRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.AdminTicketDetailRespond;
import shopping.international.api.resp.customerservice.AdminTicketSummaryRespond;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.TicketAssignmentLogRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.api.resp.customerservice.TicketParticipantRespond;
import shopping.international.api.resp.customerservice.TicketStatusLogRespond;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;
import shopping.international.domain.model.vo.customerservice.TicketReadUpdateView;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.customerservice.IAdminTicketService;
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
 * 管理侧工单控制器, 提供工单检索, 详情, 元数据更新, 指派, 状态流转能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/tickets")
public class AdminTicketController {

    /**
     * 管理侧工单领域服务
     */
    private final IAdminTicketService adminTicketService;
    /**
     * 币种配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 管理侧分页检索工单
     *
     * @param page              页码
     * @param size              每页条数
     * @param ticketNo          工单编号
     * @param userId            用户 ID
     * @param orderId           订单 ID
     * @param shipmentId        物流单 ID
     * @param issueType         问题类型
     * @param status            工单状态
     * @param priority          工单优先级
     * @param assignedToUserId  指派坐席用户 ID
     * @param claimExternalId   理赔外部编号
     * @param slaDueFrom        SLA 到期时间起始
     * @param slaDueTo          SLA 到期时间结束
     * @param createdFrom       创建时间起始
     * @param createdTo         创建时间结束
     * @return 工单摘要分页结果
     */
    @GetMapping
    public ResponseEntity<Result<List<AdminTicketSummaryRespond>>> pageTickets(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                               @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                                                               @RequestParam(value = "ticket_no", required = false) String ticketNo,
                                                                               @RequestParam(value = "user_id", required = false) Long userId,
                                                                               @RequestParam(value = "order_id", required = false) Long orderId,
                                                                               @RequestParam(value = "shipment_id", required = false) Long shipmentId,
                                                                               @RequestParam(value = "issue_type", required = false) String issueType,
                                                                               @RequestParam(value = "status", required = false) String status,
                                                                               @RequestParam(value = "priority", required = false) String priority,
                                                                               @RequestParam(value = "assigned_to_user_id", required = false) Long assignedToUserId,
                                                                               @RequestParam(value = "claim_external_id", required = false) String claimExternalId,
                                                                               @RequestParam(value = "sla_due_from", required = false) String slaDueFrom,
                                                                               @RequestParam(value = "sla_due_to", required = false) String slaDueTo,
                                                                               @RequestParam(value = "created_from", required = false) String createdFrom,
                                                                               @RequestParam(value = "created_to", required = false) String createdTo) {
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 20 : size;
        require(normalizedPage >= 1, "page 必须大于等于 1");
        require(normalizedSize >= 1 && normalizedSize <= 200, "size 必须在 1 到 200 之间");

        String normalizedTicketNo = normalizeNullableField(
                ticketNo,
                "ticket_no 不能为空",
                value -> value.length() >= 10 && value.length() <= 32,
                "ticket_no 长度需在 10 到 32 之间"
        );
        if (userId != null)
            require(userId >= 1, "user_id 必须大于等于 1");
        if (orderId != null)
            require(orderId >= 1, "order_id 必须大于等于 1");
        if (shipmentId != null)
            require(shipmentId >= 1, "shipment_id 必须大于等于 1");
        if (assignedToUserId != null)
            require(assignedToUserId >= 1, "assigned_to_user_id 必须大于等于 1");
        String normalizedClaimExternalId = normalizeNullableField(
                claimExternalId,
                "claim_external_id 不能为空",
                value -> value.length() <= 128,
                "claim_external_id 长度不能超过 128 个字符"
        );

        TicketIssueType issueTypeEnum = parseIssueType(issueType);
        TicketStatus statusEnum = parseStatus(status);
        TicketPriority priorityEnum = parsePriority(priority);
        LocalDateTime slaDueFromTime = parseDateTime(slaDueFrom);
        LocalDateTime slaDueToTime = parseDateTime(slaDueTo);
        LocalDateTime createdFromTime = parseDateTime(createdFrom);
        LocalDateTime createdToTime = parseDateTime(createdTo);
        if (slaDueFromTime != null && slaDueToTime != null)
            require(!slaDueFromTime.isAfter(slaDueToTime), "sla_due_from 不能晚于 sla_due_to");
        if (createdFromTime != null && createdToTime != null)
            require(!createdFromTime.isAfter(createdToTime), "created_from 不能晚于 created_to");

        AdminTicketPageCriteria criteria = AdminTicketPageCriteria.builder()
                .ticketNo(normalizedTicketNo)
                .userId(userId)
                .orderId(orderId)
                .shipmentId(shipmentId)
                .issueType(issueTypeEnum)
                .status(statusEnum)
                .priority(priorityEnum)
                .assignedToUserId(assignedToUserId)
                .claimExternalId(normalizedClaimExternalId)
                .slaDueFrom(slaDueFromTime)
                .slaDueTo(slaDueToTime)
                .createdFrom(createdFromTime)
                .createdTo(createdToTime)
                .build();
        PageQuery pageQuery = PageQuery.of(normalizedPage, normalizedSize, 200);
        PageResult<AdminTicketSummaryView> pageResult = adminTicketService.pageTickets(criteria, pageQuery);

        List<AdminTicketSummaryRespond> data = pageResult.items().stream()
                .map(item -> {
                    CurrencyConfig payCurrencyConfig = currencyConfigService.get(item.payCurrency());
                    return AdminTicketRespondAssembler.toSummaryRespond(item, payCurrencyConfig);
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
     * 管理侧查询工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情
     */
    @GetMapping("/{ticket_id}")
    public ResponseEntity<Result<AdminTicketDetailRespond>> getTicketDetail(@PathVariable("ticket_id") Long ticketId) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        AdminTicketDetailView detailView = adminTicketService.getTicketDetail(ticketId);
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig)));
    }

    /**
     * 管理侧更新工单元数据
     *
     * @param ticketId        工单 ID
     * @param idempotencyKey  幂等键
     * @param request         更新请求
     * @return 更新后的工单详情
     */
    @PatchMapping("/{ticket_id}")
    public ResponseEntity<Result<AdminTicketDetailRespond>> patchTicket(@PathVariable("ticket_id") Long ticketId,
                                                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                        @RequestBody AdminTicketPatchRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        AdminTicketDetailView detailView = adminTicketService.patchTicket(
                actorUserId,
                ticketId,
                request.getPriority(),
                request.getTags(),
                request.getRequestedRefundAmount(),
                request.getCurrency(),
                request.getClaimExternalId(),
                request.getSlaDueAt(),
                normalizedIdempotencyKey
        );
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig)));
    }

    /**
     * 管理侧指派或转派工单
     *
     * @param ticketId        工单 ID
     * @param idempotencyKey  幂等键
     * @param request         指派请求
     * @return 更新后的工单详情
     */
    @PostMapping("/{ticket_id}/assign")
    public ResponseEntity<Result<AdminTicketDetailRespond>> assignTicket(@PathVariable("ticket_id") Long ticketId,
                                                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                         @RequestBody AdminTicketAssignRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        AdminTicketDetailView detailView = adminTicketService.assignTicket(
                actorUserId,
                ticketId,
                request.getToAssigneeUserId(),
                request.getActionType(),
                request.getNote(),
                request.getSourceRef(),
                normalizedIdempotencyKey
        );
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig)));
    }

    /**
     * 管理侧更新工单状态
     *
     * @param ticketId        工单 ID
     * @param idempotencyKey  幂等键
     * @param request         状态流转请求
     * @return 更新后的工单详情
     */
    @PostMapping("/{ticket_id}/status")
    public ResponseEntity<Result<AdminTicketDetailRespond>> transitionTicketStatus(@PathVariable("ticket_id") Long ticketId,
                                                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                   @RequestBody TicketStatusTransitionRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        AdminTicketDetailView detailView = adminTicketService.transitionTicketStatus(
                actorUserId,
                ticketId,
                request.getToStatus(),
                request.getNote(),
                request.getSourceRef(),
                normalizedIdempotencyKey
        );
        CurrencyConfig payCurrencyConfig = currencyConfigService.get(detailView.payCurrency());
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView, payCurrencyConfig)));
    }

    /**
     * 管理侧查询工单消息列表
     *
     * @param ticketId 工单 ID
     * @param beforeId 向前翻页锚点
     * @param afterId  向后增量锚点
     * @param order    排序方向
     * @param size     返回条数
     * @return 消息列表
     */
    @GetMapping("/{ticket_id}/messages")
    public ResponseEntity<Result<List<TicketMessageRespond>>> listTicketMessages(@PathVariable("ticket_id") Long ticketId,
                                                                                 @RequestParam(value = "before_id", required = false) Long beforeId,
                                                                                 @RequestParam(value = "after_id", required = false) Long afterId,
                                                                                 @RequestParam(value = "order", required = false, defaultValue = "desc") String order,
                                                                                 @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        int normalizedSize = size == null ? 20 : size;
        require(normalizedSize >= 1 && normalizedSize <= 100, "size 必须在 1 到 100 之间");
        if (beforeId != null)
            require(beforeId >= 1, "before_id 必须大于等于 1");
        if (afterId != null)
            require(afterId >= 1, "after_id 必须大于等于 1");

        boolean ascOrder = parseMessageOrder(order);
        Long actorUserId = requireCurrentUserId();
        List<TicketMessageView> messageViewList = adminTicketService.listTicketMessages(
                actorUserId,
                ticketId,
                beforeId,
                afterId,
                ascOrder,
                normalizedSize
        );
        List<TicketMessageRespond> data = messageViewList.stream()
                .map(AdminTicketRespondAssembler::toMessageRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 管理侧发送工单消息
     *
     * @param ticketId       工单 ID
     * @param idempotencyKey 幂等键
     * @param request        消息发送请求
     * @return 发送后的消息
     */
    @PostMapping("/{ticket_id}/messages")
    public ResponseEntity<Result<TicketMessageRespond>> createTicketMessage(@PathVariable("ticket_id") Long ticketId,
                                                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                            @RequestBody TicketMessageCreateRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        TicketMessageView created = adminTicketService.createTicketMessage(
                actorUserId,
                ticketId,
                request.getMessageType(),
                request.getContent(),
                request.getAttachments(),
                request.getClientMessageId(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(AdminTicketRespondAssembler.toMessageRespond(created)));
    }

    /**
     * 管理侧编辑消息
     *
     * @param ticketId       工单 ID
     * @param messageId      消息 ID
     * @param idempotencyKey 幂等键
     * @param request        消息编辑请求
     * @return 编辑后的消息
     */
    @PatchMapping("/{ticket_id}/messages/{message_id}")
    public ResponseEntity<Result<TicketMessageRespond>> editTicketMessage(@PathVariable("ticket_id") Long ticketId,
                                                                          @PathVariable("message_id") Long messageId,
                                                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                          @RequestBody TicketMessageUpdateRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        require(messageId != null && messageId >= 1, "message_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        TicketMessageView updated = adminTicketService.editTicketMessage(
                actorUserId,
                ticketId,
                messageId,
                request.getContent(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toMessageRespond(updated)));
    }

    /**
     * 管理侧撤回消息
     *
     * @param ticketId       工单 ID
     * @param messageId      消息 ID
     * @param idempotencyKey 幂等键
     * @param request        撤回请求
     * @return 撤回后的消息
     */
    @PostMapping("/{ticket_id}/messages/{message_id}/recall")
    public ResponseEntity<Result<TicketMessageRespond>> recallTicketMessage(@PathVariable("ticket_id") Long ticketId,
                                                                            @PathVariable("message_id") Long messageId,
                                                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                            @RequestBody(required = false) TicketMessageRecallRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        require(messageId != null && messageId >= 1, "message_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        TicketMessageRecallRequest recallRequest = request == null ? new TicketMessageRecallRequest() : request;
        recallRequest.validate();

        Long actorUserId = requireCurrentUserId();
        TicketMessageView recalled = adminTicketService.recallTicketMessage(
                actorUserId,
                ticketId,
                messageId,
                recallRequest.getReason(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toMessageRespond(recalled)));
    }

    /**
     * 管理侧标记工单消息已读
     *
     * @param ticketId       工单 ID
     * @param idempotencyKey 幂等键
     * @param request        已读请求
     * @return 已读位点事件数据
     */
    @PostMapping("/{ticket_id}/read")
    public ResponseEntity<Result<CsWsTicketReadUpdatedEventDataRespond>> markTicketRead(@PathVariable("ticket_id") Long ticketId,
                                                                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                        @RequestBody TicketReadRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        TicketReadUpdateView updated = adminTicketService.markTicketRead(
                actorUserId,
                ticketId,
                request.getLastReadMessageId(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toReadUpdatedEventDataRespond(updated)));
    }

    /**
     * 管理侧查询工单参与方列表
     *
     * @param ticketId 工单 ID
     * @return 参与方列表
     */
    @GetMapping("/{ticket_id}/participants")
    public ResponseEntity<Result<List<TicketParticipantRespond>>> listTicketParticipants(@PathVariable("ticket_id") Long ticketId) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");

        Long actorUserId = requireCurrentUserId();
        List<TicketParticipant> participantList = adminTicketService.listTicketParticipants(actorUserId, ticketId);
        List<TicketParticipantRespond> data = participantList.stream()
                .map(AdminTicketRespondAssembler::toParticipantRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 管理侧新增工单参与方
     *
     * @param ticketId       工单 ID
     * @param idempotencyKey 幂等键
     * @param request        新增参与方请求
     * @return 新增后的参与方
     */
    @PostMapping("/{ticket_id}/participants")
    public ResponseEntity<Result<TicketParticipantRespond>> createTicketParticipant(@PathVariable("ticket_id") Long ticketId,
                                                                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                    @RequestBody AdminTicketParticipantCreateRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        TicketParticipant participant = adminTicketService.createTicketParticipant(
                actorUserId,
                ticketId,
                request.getParticipantType(),
                request.getParticipantUserId(),
                request.getRole(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(AdminTicketRespondAssembler.toParticipantRespond(participant)));
    }

    /**
     * 管理侧更新工单参与方角色
     *
     * @param ticketId       工单 ID
     * @param participantId  参与方 ID
     * @param idempotencyKey 幂等键
     * @param request        角色更新请求
     * @return 更新后的参与方
     */
    @PatchMapping("/{ticket_id}/participants/{participant_id}")
    public ResponseEntity<Result<TicketParticipantRespond>> patchTicketParticipant(@PathVariable("ticket_id") Long ticketId,
                                                                                   @PathVariable("participant_id") Long participantId,
                                                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                   @RequestBody AdminTicketParticipantPatchRequest request) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        require(participantId != null && participantId >= 1, "participant_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );
        request.validate();

        Long actorUserId = requireCurrentUserId();
        TicketParticipant participant = adminTicketService.patchTicketParticipant(
                actorUserId,
                ticketId,
                participantId,
                request.getRole(),
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toParticipantRespond(participant)));
    }

    /**
     * 管理侧将工单参与方设为离开
     *
     * @param ticketId       工单 ID
     * @param participantId  参与方 ID
     * @param idempotencyKey 幂等键
     * @return 空数据响应
     */
    @PostMapping("/{ticket_id}/participants/{participant_id}/leave")
    public ResponseEntity<Result<Object>> leaveTicketParticipant(@PathVariable("ticket_id") Long ticketId,
                                                                 @PathVariable("participant_id") Long participantId,
                                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        require(participantId != null && participantId >= 1, "participant_id 必须大于等于 1");
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        Long actorUserId = requireCurrentUserId();
        adminTicketService.leaveTicketParticipant(
                actorUserId,
                ticketId,
                participantId,
                normalizedIdempotencyKey
        );
        return ResponseEntity.ok(Result.ok(null));
    }

    /**
     * 管理侧查询工单状态流转日志
     *
     * @param ticketId 工单 ID
     * @param page     页码
     * @param size     每页条数
     * @return 状态日志分页结果
     */
    @GetMapping("/{ticket_id}/status-logs")
    public ResponseEntity<Result<List<TicketStatusLogRespond>>> listTicketStatusLogs(@PathVariable("ticket_id") Long ticketId,
                                                                                     @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                                     @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        PageQuery pageQuery = pagePreprocess(ticketId, page, size);
        Long actorUserId = requireCurrentUserId();
        PageResult<TicketStatusLog> pageResult = adminTicketService.listTicketStatusLogs(actorUserId, ticketId, pageQuery);
        List<TicketStatusLogRespond> data = pageResult.items().stream()
                .map(AdminTicketRespondAssembler::toStatusLogRespond)
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
     * 管理侧查询工单指派日志
     *
     * @param ticketId 工单 ID
     * @param page     页码
     * @param size     每页条数
     * @return 指派日志分页结果
     */
    @GetMapping("/{ticket_id}/assignment-logs")
    public ResponseEntity<Result<List<TicketAssignmentLogRespond>>> listTicketAssignmentLogs(@PathVariable("ticket_id") Long ticketId,
                                                                                             @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                                             @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        PageQuery pageQuery = pagePreprocess(ticketId, page, size);
        Long actorUserId = requireCurrentUserId();
        PageResult<TicketAssignmentLog> pageResult = adminTicketService.listTicketAssignmentLogs(actorUserId, ticketId, pageQuery);
        List<TicketAssignmentLogRespond> data = pageResult.items().stream()
                .map(AdminTicketRespondAssembler::toAssignmentLogRespond)
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
     * 对分页查询参数进行预处理, 确保其符合要求
     *
     * @param ticketId 票据 ID, 必须大于等于 1
     * @param page     分页的页码, 如果为 null, 则默认值为 1, 必须大于等于 1
     * @param size     每页的数据条数, 如果为 null, 则默认值为 20, 必须在 1 到 200 之间
     * @return 返回一个经过验证和规范化后的 {@code PageQuery} 对象
     */
    private static @NotNull PageQuery pagePreprocess(Long ticketId, Integer page, Integer size) {
        require(ticketId != null && ticketId >= 1, "ticket_id 必须大于等于 1");
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 20 : size;
        require(normalizedPage >= 1, "page 必须大于等于 1");
        require(normalizedSize >= 1 && normalizedSize <= 200, "size 必须在 1 到 200 之间");

        return PageQuery.of(normalizedPage, normalizedSize, 200);
    }

    /**
     * 解析问题类型查询参数
     *
     * @param issueType 问题类型文本
     * @return 问题类型枚举
     */
    private @Nullable TicketIssueType parseIssueType(@Nullable String issueType) {
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
     * 解析工单状态查询参数
     *
     * @param status 工单状态文本
     * @return 工单状态枚举
     */
    private @Nullable TicketStatus parseStatus(@Nullable String status) {
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
     * 解析工单优先级查询参数
     *
     * @param priority 工单优先级文本
     * @return 工单优先级枚举
     */
    private @Nullable TicketPriority parsePriority(@Nullable String priority) {
        if (priority == null || priority.isBlank())
            return null;
        String normalized = priority.strip().toUpperCase(Locale.ROOT);
        try {
            return TicketPriority.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalParamException("priority 不合法: " + priority);
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

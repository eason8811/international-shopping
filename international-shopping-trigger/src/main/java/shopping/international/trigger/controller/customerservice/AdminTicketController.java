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
import shopping.international.api.req.customerservice.AdminTicketPatchRequest;
import shopping.international.api.req.customerservice.TicketStatusTransitionRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.AdminTicketDetailRespond;
import shopping.international.api.resp.customerservice.AdminTicketSummaryRespond;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;
import shopping.international.domain.service.customerservice.IAdminTicketService;
import shopping.international.types.constant.SecurityConstants;
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
                .map(AdminTicketRespondAssembler::toSummaryRespond)
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
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView)));
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
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView)));
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
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView)));
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
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toDetailRespond(detailView)));
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

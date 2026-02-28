package shopping.international.domain.model.aggregate.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.*;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 客服工单聚合根, 对应表 `cs_ticket`
 *
 * <p>聚合职责, 维护工单状态机, 指派规则, 消息和参与方的一致性边界</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "ticketNo")
@Accessors(chain = true)
public class CustomerServiceTicket implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private final Long id;
    /**
     * 工单编号
     */
    private final TicketNo ticketNo;
    /**
     * 发起用户 ID
     */
    private final Long userId;
    /**
     * 关联订单 ID
     */
    @Nullable
    private final Long orderId;
    /**
     * 关联订单明细 ID
     */
    @Nullable
    private final Long orderItemId;
    /**
     * 关联物流单 ID
     */
    @Nullable
    private final Long shipmentId;
    /**
     * 问题类型
     */
    private final TicketIssueType issueType;
    /**
     * 工单状态
     */
    private TicketStatus status;
    /**
     * 工单优先级
     */
    private TicketPriority priority;
    /**
     * 工单来源渠道
     */
    private final TicketChannel channel;
    /**
     * 工单标题
     */
    private String title;
    /**
     * 工单描述
     */
    @Nullable
    private String description;
    /**
     * 附件链接列表
     */
    private List<String> attachments;
    /**
     * 证据链接列表
     */
    private List<String> evidence;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 申请退款金额, 单位分
     */
    @Nullable
    private Long requestedRefundAmount;
    /**
     * 币种
     */
    private String currency;
    /**
     * 承运商理赔外部编号
     */
    @Nullable
    private String claimExternalId;
    /**
     * 当前指派坐席用户 ID
     */
    @Nullable
    private Long assignedToUserId;
    /**
     * 指派时间
     */
    @Nullable
    private LocalDateTime assignedAt;
    /**
     * 最近消息时间
     */
    @Nullable
    private LocalDateTime lastMessageAt;
    /**
     * SLA 到期时间
     */
    @Nullable
    private LocalDateTime slaDueAt;
    /**
     * 解决时间
     */
    @Nullable
    private LocalDateTime resolvedAt;
    /**
     * 关闭时间
     */
    @Nullable
    private LocalDateTime closedAt;
    /**
     * 当前状态进入时间
     */
    private LocalDateTime statusChangedAt;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;
    /**
     * 扩展字段 JSON
     */
    @Nullable
    private String extra;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 参与方列表
     */
    private List<TicketParticipant> participantList;
    /**
     * 消息列表
     */
    private List<TicketMessage> messageList;
    /**
     * 状态日志列表
     */
    private List<TicketStatusLog> statusLogList;
    /**
     * 指派日志列表
     */
    private List<TicketAssignmentLog> assignmentLogList;

    /**
     * 构造客服工单聚合根
     *
     * @param id                    主键 ID
     * @param ticketNo              工单编号
     * @param userId                发起用户 ID
     * @param orderId               关联订单 ID
     * @param orderItemId           关联订单明细 ID
     * @param shipmentId            关联物流单 ID
     * @param issueType             问题类型
     * @param status                工单状态
     * @param priority              工单优先级
     * @param channel               工单来源渠道
     * @param title                 工单标题
     * @param description           工单描述
     * @param attachments           附件链接列表
     * @param evidence              证据链接列表
     * @param tags                  标签列表
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param claimExternalId       承运商理赔外部编号
     * @param assignedToUserId      当前指派坐席用户 ID
     * @param assignedAt            指派时间
     * @param lastMessageAt         最近消息时间
     * @param slaDueAt              SLA 到期时间
     * @param resolvedAt            解决时间
     * @param closedAt              关闭时间
     * @param statusChangedAt       当前状态进入时间
     * @param sourceRef             来源引用 ID
     * @param extra                 扩展字段 JSON
     * @param createdAt             创建时间
     * @param updatedAt             更新时间
     * @param participantList       参与方列表
     * @param messageList           消息列表
     * @param statusLogList         状态日志列表
     * @param assignmentLogList     指派日志列表
     */
    private CustomerServiceTicket(@Nullable Long id,
                                  TicketNo ticketNo,
                                  Long userId,
                                  @Nullable Long orderId,
                                  @Nullable Long orderItemId,
                                  @Nullable Long shipmentId,
                                  TicketIssueType issueType,
                                  TicketStatus status,
                                  TicketPriority priority,
                                  TicketChannel channel,
                                  String title,
                                  @Nullable String description,
                                  List<String> attachments,
                                  List<String> evidence,
                                  List<String> tags,
                                  @Nullable Long requestedRefundAmount,
                                  String currency,
                                  @Nullable String claimExternalId,
                                  @Nullable Long assignedToUserId,
                                  @Nullable LocalDateTime assignedAt,
                                  @Nullable LocalDateTime lastMessageAt,
                                  @Nullable LocalDateTime slaDueAt,
                                  @Nullable LocalDateTime resolvedAt,
                                  @Nullable LocalDateTime closedAt,
                                  LocalDateTime statusChangedAt,
                                  @Nullable String sourceRef,
                                  @Nullable String extra,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt,
                                  List<TicketParticipant> participantList,
                                  List<TicketMessage> messageList,
                                  List<TicketStatusLog> statusLogList,
                                  List<TicketAssignmentLog> assignmentLogList) {
        this.id = id;
        this.ticketNo = ticketNo;
        this.userId = userId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.shipmentId = shipmentId;
        this.issueType = issueType;
        this.status = status;
        this.priority = priority;
        this.channel = channel;
        this.title = title;
        this.description = description;
        this.attachments = attachments;
        this.evidence = evidence;
        this.tags = tags;
        this.requestedRefundAmount = requestedRefundAmount;
        this.currency = currency;
        this.claimExternalId = claimExternalId;
        this.assignedToUserId = assignedToUserId;
        this.assignedAt = assignedAt;
        this.lastMessageAt = lastMessageAt;
        this.slaDueAt = slaDueAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.statusChangedAt = statusChangedAt;
        this.sourceRef = sourceRef;
        this.extra = extra;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.participantList = participantList;
        this.messageList = messageList;
        this.statusLogList = statusLogList;
        this.assignmentLogList = assignmentLogList;
    }

    /**
     * 创建新的客服工单聚合根
     *
     * @param userId                发起用户 ID
     * @param orderId               关联订单 ID
     * @param orderItemId           关联订单明细 ID
     * @param shipmentId            关联物流单 ID
     * @param issueType             问题类型
     * @param title                 工单标题
     * @param description           工单描述
     * @param attachments           附件链接列表
     * @param evidence              证据链接列表
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param channel               工单来源渠道
     * @param sourceRef             来源引用 ID
     * @return 新建的客服工单聚合根
     */
    public static CustomerServiceTicket create(Long userId,
                                               @Nullable Long orderId,
                                               @Nullable Long orderItemId,
                                               @Nullable Long shipmentId,
                                               TicketIssueType issueType,
                                               String title,
                                               @Nullable String description,
                                               @Nullable List<String> attachments,
                                               @Nullable List<String> evidence,
                                               @Nullable Long requestedRefundAmount,
                                               @Nullable String currency,
                                               @Nullable TicketChannel channel,
                                               @Nullable String sourceRef) {
        LocalDateTime now = LocalDateTime.now();
        CustomerServiceTicket ticket = new CustomerServiceTicket(
                null,
                TicketNo.generate(),
                userId,
                orderId,
                orderItemId,
                shipmentId,
                issueType,
                TicketStatus.OPEN,
                TicketPriority.NORMAL,
                channel == null ? TicketChannel.CLIENT : channel,
                normalizeNotNullField(title, "title 不能为空", value -> value.length() <= 200, "title 长度不能超过 200"),
                normalizeNullableField(description, "description 不能为空", value -> value.length() <= 2000, "description 长度不能超过 2000"),
                normalizeLinkList(attachments, 20, "attachments"),
                normalizeLinkList(evidence, 50, "evidence"),
                List.of(),
                requestedRefundAmount,
                normalizeCurrency(currency),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                normalizeNullableField(sourceRef, "sourceRef 不能为空", value -> value.length() <= 128, "sourceRef 长度不能超过 128"),
                null,
                now,
                now,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
        ticket.validate();
        return ticket;
    }

    /**
     * 从持久化数据重建客服工单聚合根
     *
     * @param id                    主键 ID
     * @param ticketNo              工单编号
     * @param userId                发起用户 ID
     * @param orderId               关联订单 ID
     * @param orderItemId           关联订单明细 ID
     * @param shipmentId            关联物流单 ID
     * @param issueType             问题类型
     * @param status                工单状态
     * @param priority              工单优先级
     * @param channel               工单来源渠道
     * @param title                 工单标题
     * @param description           工单描述
     * @param attachments           附件链接列表
     * @param evidence              证据链接列表
     * @param tags                  标签列表
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param claimExternalId       承运商理赔外部编号
     * @param assignedToUserId      当前指派坐席用户 ID
     * @param assignedAt            指派时间
     * @param lastMessageAt         最近消息时间
     * @param slaDueAt              SLA 到期时间
     * @param resolvedAt            解决时间
     * @param closedAt              关闭时间
     * @param statusChangedAt       当前状态进入时间
     * @param sourceRef             来源引用 ID
     * @param extra                 扩展字段 JSON
     * @param createdAt             创建时间
     * @param updatedAt             更新时间
     * @param participantList       参与方列表
     * @param messageList           消息列表
     * @param statusLogList         状态日志列表
     * @param assignmentLogList     指派日志列表
     * @return 重建后的客服工单聚合根
     */
    public static CustomerServiceTicket reconstitute(@Nullable Long id,
                                                     TicketNo ticketNo,
                                                     Long userId,
                                                     @Nullable Long orderId,
                                                     @Nullable Long orderItemId,
                                                     @Nullable Long shipmentId,
                                                     TicketIssueType issueType,
                                                     TicketStatus status,
                                                     TicketPriority priority,
                                                     TicketChannel channel,
                                                     String title,
                                                     @Nullable String description,
                                                     @Nullable List<String> attachments,
                                                     @Nullable List<String> evidence,
                                                     @Nullable List<String> tags,
                                                     @Nullable Long requestedRefundAmount,
                                                     String currency,
                                                     @Nullable String claimExternalId,
                                                     @Nullable Long assignedToUserId,
                                                     @Nullable LocalDateTime assignedAt,
                                                     @Nullable LocalDateTime lastMessageAt,
                                                     @Nullable LocalDateTime slaDueAt,
                                                     @Nullable LocalDateTime resolvedAt,
                                                     @Nullable LocalDateTime closedAt,
                                                     @Nullable LocalDateTime statusChangedAt,
                                                     @Nullable String sourceRef,
                                                     @Nullable String extra,
                                                     LocalDateTime createdAt,
                                                     LocalDateTime updatedAt,
                                                     @Nullable List<TicketParticipant> participantList,
                                                     @Nullable List<TicketMessage> messageList,
                                                     @Nullable List<TicketStatusLog> statusLogList,
                                                     @Nullable List<TicketAssignmentLog> assignmentLogList) {
        CustomerServiceTicket ticket = new CustomerServiceTicket(
                id,
                ticketNo,
                userId,
                orderId,
                orderItemId,
                shipmentId,
                issueType,
                status,
                priority,
                channel,
                normalizeNotNullField(title, "title 不能为空", value -> value.length() <= 200, "title 长度不能超过 200"),
                normalizeNullableField(description, "description 不能为空", value -> value.length() <= 2000, "description 长度不能超过 2000"),
                normalizeLinkList(attachments, 20, "attachments"),
                normalizeLinkList(evidence, 50, "evidence"),
                normalizeTagList(tags),
                requestedRefundAmount,
                normalizeCurrency(currency),
                normalizeNullableField(claimExternalId, "claimExternalId 不能为空", value -> value.length() <= 128, "claimExternalId 长度不能超过 128"),
                assignedToUserId,
                assignedAt,
                lastMessageAt,
                slaDueAt,
                resolvedAt,
                closedAt,
                statusChangedAt == null ? updatedAt : statusChangedAt,
                normalizeNullableField(sourceRef, "sourceRef 不能为空", value -> value.length() <= 128, "sourceRef 长度不能超过 128"),
                normalizeNullableField(extra, "extra 不能为空", value -> value.length() <= 20000, "extra 长度不能超过 20000"),
                createdAt,
                updatedAt,
                new ArrayList<>(normalizeFieldList(participantList)),
                new ArrayList<>(normalizeFieldList(messageList)),
                new ArrayList<>(normalizeFieldList(statusLogList)),
                new ArrayList<>(normalizeFieldList(assignmentLogList))
        );
        ticket.validate();
        return ticket;
    }

    /**
     * 更新工单可编辑元数据
     *
     * @param priority              工单优先级
     * @param tags                  标签列表
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param claimExternalId       承运商理赔外部编号
     * @param slaDueAt              SLA 到期时间
     */
    public void patch(@Nullable TicketPriority priority,
                      @Nullable List<String> tags,
                      @Nullable Long requestedRefundAmount,
                      @Nullable String currency,
                      @Nullable String claimExternalId,
                      @Nullable LocalDateTime slaDueAt) {
        if (priority != null)
            this.priority = priority;
        if (tags != null)
            this.tags = normalizeTagList(tags);
        if (requestedRefundAmount != null) {
            require(requestedRefundAmount >= 1, "requestedRefundAmount 必须大于等于 1");
            this.requestedRefundAmount = requestedRefundAmount;
        }
        if (currency != null)
            this.currency = normalizeCurrency(currency);
        if (claimExternalId != null)
            this.claimExternalId = normalizeNullableField(claimExternalId, "claimExternalId 不能为空", value -> value.length() <= 128, "claimExternalId 长度不能超过 128");
        if (slaDueAt != null)
            this.slaDueAt = slaDueAt;
        if (priority != null || tags != null || requestedRefundAmount != null
                || currency != null || claimExternalId != null || slaDueAt != null)
            this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 推进工单状态, 并追加状态流转日志
     *
     * @param toStatus    目标状态
     * @param actorType   操作者类型
     * @param actorUserId 操作者用户 ID
     * @param sourceRef   来源引用 ID
     * @param note        备注
     * @return 追加的状态流转日志实体
     */
    public TicketStatusLog transitionStatus(TicketStatus toStatus,
                                            TicketActorType actorType,
                                            @Nullable Long actorUserId,
                                            @Nullable String sourceRef,
                                            @Nullable String note) {
        requirePersisted();
        requireNotNull(toStatus, "toStatus 不能为空");
        requireNotNull(actorType, "actorType 不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (!status.canTransitTo(toStatus, statusChangedAt, now))
            throw new ConflictException("当前状态不允许流转到目标状态");

        TicketStatus fromStatus = this.status;
        this.status = toStatus;
        this.statusChangedAt = now;
        this.updatedAt = now;
        if (toStatus == TicketStatus.RESOLVED)
            this.resolvedAt = now;
        if (toStatus == TicketStatus.CLOSED)
            this.closedAt = now;

        TicketStatusLog log = TicketStatusLog.create(id, fromStatus, toStatus, actorType, actorUserId, sourceRef, note);
        statusLogList.add(log);
        validate();
        return log;
    }

    /**
     * 执行工单指派动作, 并追加指派日志
     *
     * @param toAssigneeUserId 目标指派坐席用户 ID
     * @param actionType       指派动作
     * @param actorType        操作者类型
     * @param actorUserId      操作者用户 ID
     * @param sourceRef        来源引用 ID
     * @param note             备注
     * @return 追加的指派日志实体
     */
    public TicketAssignmentLog assign(@Nullable Long toAssigneeUserId,
                                      TicketAssignmentActionType actionType,
                                      TicketActorType actorType,
                                      @Nullable Long actorUserId,
                                      @Nullable String sourceRef,
                                      @Nullable String note) {
        requirePersisted();
        requireNotNull(actionType, "actionType 不能为空");
        requireNotNull(actorType, "actorType 不能为空");
        if (toAssigneeUserId != null)
            require(toAssigneeUserId > 0, "toAssigneeUserId 必须大于 0");
        if (actionType.requiresAssignee())
            requireNotNull(toAssigneeUserId, "当前 actionType 需要提供 toAssigneeUserId");

        Long fromAssigneeUserId = this.assignedToUserId;
        LocalDateTime now = LocalDateTime.now();
        if (actionType == TicketAssignmentActionType.UNASSIGN) {
            this.assignedToUserId = null;
            this.assignedAt = null;
        } else {
            this.assignedToUserId = toAssigneeUserId;
            this.assignedAt = now;
        }
        this.updatedAt = now;

        TicketAssignmentLog log = TicketAssignmentLog.create(
                id,
                fromAssigneeUserId,
                this.assignedToUserId,
                actionType,
                actorType,
                actorUserId,
                sourceRef,
                note
        );
        assignmentLogList.add(log);
        validate();
        return log;
    }

    /**
     * 新增工单参与方
     *
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @return 新增的工单参与方实体
     */
    public TicketParticipant addParticipant(TicketParticipantType participantType,
                                            @Nullable Long participantUserId,
                                            TicketParticipantRole role) {
        requirePersisted();
        requireNotNull(participantType, "participantType 不能为空");
        requireNotNull(role, "role 不能为空");
        Long normalizedUserId = participantUserId == null ? 0L : participantUserId;
        boolean duplicated = participantList.stream()
                .filter(TicketParticipant::isActive)
                .anyMatch(item -> item.getParticipantType() == participantType
                        && Objects.equals(item.getParticipantUserId() == null ? 0L : item.getParticipantUserId(), normalizedUserId));
        if (duplicated)
            throw new ConflictException("同类型同用户的活跃参与方已存在");

        TicketParticipant participant = TicketParticipant.create(id, participantType, participantUserId, role);
        participantList.add(participant);
        this.updatedAt = LocalDateTime.now();
        validate();
        return participant;
    }

    /**
     * 更新工单参与方角色
     *
     * @param participantId 参与方记录 ID
     * @param role          目标角色
     */
    public void changeParticipantRole(Long participantId, TicketParticipantRole role) {
        requireNotNull(participantId, "participantId 不能为空");
        require(participantId > 0, "participantId 必须大于 0");
        requireNotNull(role, "role 不能为空");
        TicketParticipant participant = findParticipant(participantId);
        participant.changeRole(role);
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 将工单参与方标记为离开
     *
     * @param participantId 参与方记录 ID
     */
    public void leaveParticipant(Long participantId) {
        requireNotNull(participantId, "participantId 不能为空");
        require(participantId > 0, "participantId 必须大于 0");
        TicketParticipant participant = findParticipant(participantId);
        participant.leave(null);
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 追加一条工单消息
     *
     * @param senderType      发送方类型
     * @param senderUserId    发送方用户 ID
     * @param messageType     消息类型
     * @param content         消息内容
     * @param attachments     附件链接列表
     * @param metadata        扩展元数据 JSON
     * @param clientMessageId 客户端消息幂等键
     * @return 新增的工单消息实体
     */
    public TicketMessage appendMessage(TicketParticipantType senderType,
                                       @Nullable Long senderUserId,
                                       @Nullable TicketMessageType messageType,
                                       @Nullable String content,
                                       @Nullable List<String> attachments,
                                       @Nullable String metadata,
                                       @Nullable String clientMessageId) {
        requirePersisted();
        TicketMessage message = TicketMessage.create(id, senderType, senderUserId, messageType, content, attachments, metadata, clientMessageId);
        messageList.add(message);
        this.lastMessageAt = message.getSentAt();
        this.updatedAt = message.getSentAt();
        validate();
        return message;
    }

    /**
     * 编辑已存在的工单消息
     *
     * @param messageId  消息 ID
     * @param newContent 新文本内容
     */
    public void editMessage(Long messageId, String newContent) {
        requireNotNull(messageId, "messageId 不能为空");
        require(messageId > 0, "messageId 必须大于 0");
        TicketMessage message = findMessage(messageId);
        message.editContent(newContent);
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 撤回已存在的工单消息
     *
     * @param messageId 消息 ID
     */
    public void recallMessage(Long messageId) {
        requireNotNull(messageId, "messageId 不能为空");
        require(messageId > 0, "messageId 必须大于 0");
        TicketMessage message = findMessage(messageId);
        message.recall();
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 更新参与方已读位点
     *
     * @param participantId     参与方记录 ID
     * @param lastReadMessageId 最后已读消息 ID
     */
    public void markParticipantRead(Long participantId, Long lastReadMessageId) {
        requireNotNull(participantId, "participantId 不能为空");
        require(participantId > 0, "participantId 必须大于 0");
        TicketParticipant participant = findParticipant(participantId);
        participant.markRead(lastReadMessageId, null);
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 校验客服工单聚合不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(ticketNo, "ticketNo 不能为空");
        requireNotNull(userId, "userId 不能为空");
        require(userId > 0, "userId 必须大于 0");
        if (orderId != null)
            require(orderId > 0, "orderId 必须大于 0");
        if (orderItemId != null)
            require(orderItemId > 0, "orderItemId 必须大于 0");
        if (shipmentId != null)
            require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(issueType, "issueType 不能为空");
        requireNotNull(status, "status 不能为空");
        requireNotNull(priority, "priority 不能为空");
        requireNotNull(channel, "channel 不能为空");
        title = normalizeNotNullField(title, "title 不能为空", value -> value.length() <= 200, "title 长度不能超过 200");
        description = normalizeNullableField(description, "description 不能为空", value -> value.length() <= 2000, "description 长度不能超过 2000");
        attachments = normalizeLinkList(attachments, 20, "attachments");
        evidence = normalizeLinkList(evidence, 50, "evidence");
        tags = normalizeTagList(tags);
        if (requestedRefundAmount != null)
            require(requestedRefundAmount >= 1, "requestedRefundAmount 必须大于等于 1");
        currency = normalizeCurrency(currency);
        claimExternalId = normalizeNullableField(claimExternalId, "claimExternalId 不能为空", value -> value.length() <= 128, "claimExternalId 长度不能超过 128");
        if (assignedToUserId != null)
            require(assignedToUserId > 0, "assignedToUserId 必须大于 0");
        if (sourceRef != null)
            sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空", value -> value.length() <= 128, "sourceRef 长度不能超过 128");
        if (extra != null)
            extra = normalizeNullableField(extra, "extra 不能为空", value -> value.length() <= 20000, "extra 长度不能超过 20000");
        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");
        requireNotNull(statusChangedAt, "statusChangedAt 不能为空");
        if (assignedAt != null)
            require(assignedToUserId != null, "assignedAt 存在时必须具备 assignedToUserId");
        if (closedAt != null)
            require(status == TicketStatus.CLOSED, "closedAt 存在时状态必须为 CLOSED");
        if (status == TicketStatus.CLOSED)
            requireNotNull(closedAt, "CLOSED 状态必须记录 closedAt");

        participantList = new ArrayList<>(normalizeFieldList(participantList));
        messageList = new ArrayList<>(normalizeFieldList(messageList));
        statusLogList = new ArrayList<>(normalizeFieldList(statusLogList));
        assignmentLogList = new ArrayList<>(normalizeFieldList(assignmentLogList));

        Set<String> activeParticipantDedup = new LinkedHashSet<>();
        for (TicketParticipant participant : participantList) {
            if (id != null)
                require(Objects.equals(participant.getTicketId(), id), "参与方 ticketId 与工单 id 不一致");
            if (!participant.isActive())
                continue;
            String key = participant.getParticipantType().name() + "#" + (participant.getParticipantUserId() == null ? 0L : participant.getParticipantUserId());
            require(activeParticipantDedup.add(key), "同类型同用户的活跃参与方不允许重复");
        }

        for (TicketMessage message : messageList) {
            if (id != null)
                require(Objects.equals(message.getTicketId(), id), "消息 ticketId 与工单 id 不一致");
        }
        for (TicketStatusLog log : statusLogList) {
            if (id != null)
                require(Objects.equals(log.getTicketId(), id), "状态日志 ticketId 与工单 id 不一致");
        }
        for (TicketAssignmentLog log : assignmentLogList) {
            if (id != null)
                require(Objects.equals(log.getTicketId(), id), "指派日志 ticketId 与工单 id 不一致");
        }
    }

    /**
     * 获取指定 ID 的参与方实体
     *
     * @param participantId 参与方记录 ID
     * @return 参与方实体
     */
    private TicketParticipant findParticipant(Long participantId) {
        return participantList.stream()
                .filter(item -> Objects.equals(item.getId(), participantId))
                .findFirst()
                .orElseThrow(() -> new ConflictException("指定参与方不存在"));
    }

    /**
     * 获取指定 ID 的消息实体
     *
     * @param messageId 消息 ID
     * @return 消息实体
     */
    private TicketMessage findMessage(Long messageId) {
        return messageList.stream()
                .filter(item -> Objects.equals(item.getId(), messageId))
                .findFirst()
                .orElseThrow(() -> new ConflictException("指定消息不存在"));
    }

    /**
     * 确保工单已持久化
     */
    private void requirePersisted() {
        requireNotNull(id, "工单未持久化, 当前操作不可执行");
        require(id > 0, "工单 ID 非法");
    }

    /**
     * 规范化链接列表并去重
     *
     * @param values 原始链接列表
     * @param limit  最大元素数量
     * @param field  字段名
     * @return 规范化后的链接列表
     */
    private static List<String> normalizeLinkList(@Nullable List<String> values, int limit, String field) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= limit, field + " 元素数量不能超过 " + limit);
        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, field + " 元素不能为空", item -> item.length() <= 2048,
                    field + " 元素长度不能超过 2048");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }

    /**
     * 规范化标签列表并去重
     *
     * @param values 原始标签列表
     * @return 规范化后的标签列表
     */
    private static List<String> normalizeTagList(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 50, "tags 元素数量不能超过 50");
        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, "tags 元素不能为空", item -> item.length() <= 64,
                    "tags 元素长度不能超过 64");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }
}

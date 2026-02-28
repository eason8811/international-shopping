package shopping.international.domain.model.entity.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.vo.customerservice.TicketMessageNo;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单消息实体, 对应表 `cs_ticket_message`
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class TicketMessage implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 消息编号
     */
    private TicketMessageNo messageNo;
    /**
     * 工单 ID
     */
    private Long ticketId;
    /**
     * 发送方类型
     */
    private TicketParticipantType senderType;
    /**
     * 发送方用户 ID, 当类型为 SYSTEM 或 BOT 时可为空
     */
    @Nullable
    private Long senderUserId;
    /**
     * 消息类型
     */
    private TicketMessageType messageType;
    /**
     * 文本内容
     */
    @Nullable
    private String content;
    /**
     * 附件链接列表
     */
    private List<String> attachments;
    /**
     * 扩展元数据 JSON
     */
    @Nullable
    private String metadata;
    /**
     * 客户端消息幂等键
     */
    @Nullable
    private String clientMessageId;
    /**
     * 发送时间
     */
    private LocalDateTime sentAt;
    /**
     * 编辑时间
     */
    @Nullable
    private LocalDateTime editedAt;
    /**
     * 撤回时间
     */
    @Nullable
    private LocalDateTime recalledAt;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 构造工单消息实体
     *
     * @param id              主键 ID
     * @param messageNo       消息编号
     * @param ticketId        工单 ID
     * @param senderType      发送方类型
     * @param senderUserId    发送方用户 ID
     * @param messageType     消息类型
     * @param content         消息文本内容
     * @param attachments     附件链接列表
     * @param metadata        扩展元数据 JSON
     * @param clientMessageId 客户端消息幂等键
     * @param sentAt          发送时间
     * @param editedAt        编辑时间
     * @param recalledAt      撤回时间
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     */
    private TicketMessage(@Nullable Long id,
                          TicketMessageNo messageNo,
                          Long ticketId,
                          TicketParticipantType senderType,
                          @Nullable Long senderUserId,
                          TicketMessageType messageType,
                          @Nullable String content,
                          List<String> attachments,
                          @Nullable String metadata,
                          @Nullable String clientMessageId,
                          LocalDateTime sentAt,
                          @Nullable LocalDateTime editedAt,
                          @Nullable LocalDateTime recalledAt,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id = id;
        this.messageNo = messageNo;
        this.ticketId = ticketId;
        this.senderType = senderType;
        this.senderUserId = senderUserId;
        this.messageType = messageType;
        this.content = content;
        this.attachments = attachments;
        this.metadata = metadata;
        this.clientMessageId = clientMessageId;
        this.sentAt = sentAt;
        this.editedAt = editedAt;
        this.recalledAt = recalledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的工单消息实体
     *
     * @param ticketId        工单 ID
     * @param senderType      发送方类型
     * @param senderUserId    发送方用户 ID
     * @param messageType     消息类型
     * @param content         消息文本内容
     * @param attachments     附件链接列表
     * @param metadata        扩展元数据 JSON
     * @param clientMessageId 客户端消息幂等键
     * @return 新建的工单消息实体
     */
    public static TicketMessage create(Long ticketId,
                                       TicketParticipantType senderType,
                                       @Nullable Long senderUserId,
                                       TicketMessageType messageType,
                                       @Nullable String content,
                                       @Nullable List<String> attachments,
                                       @Nullable String metadata,
                                       @Nullable String clientMessageId) {
        LocalDateTime now = LocalDateTime.now();
        TicketMessage message = new TicketMessage(
                null,
                TicketMessageNo.generate(),
                ticketId,
                senderType,
                senderUserId,
                messageType == null ? TicketMessageType.TEXT : messageType,
                normalizeNullableField(content, "content 不能为空", value -> value.length() <= 4000, "content 长度不能超过 4000"),
                normalizeAttachments(attachments),
                normalizeNullableField(metadata, "metadata 不能为空", value -> value.length() <= 20000, "metadata 长度不能超过 20000"),
                normalizeNullableField(clientMessageId, "clientMessageId 不能为空", value -> value.length() <= 64, "clientMessageId 长度不能超过 64"),
                now,
                null,
                null,
                now,
                now
        );
        message.validate();
        return message;
    }

    /**
     * 从持久化数据重建工单消息实体
     *
     * @param id              主键 ID
     * @param messageNo       消息编号
     * @param ticketId        工单 ID
     * @param senderType      发送方类型
     * @param senderUserId    发送方用户 ID
     * @param messageType     消息类型
     * @param content         消息文本内容
     * @param attachments     附件链接列表
     * @param metadata        扩展元数据 JSON
     * @param clientMessageId 客户端消息幂等键
     * @param sentAt          发送时间
     * @param editedAt        编辑时间
     * @param recalledAt      撤回时间
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     * @return 重建后的工单消息实体
     */
    public static TicketMessage reconstitute(@Nullable Long id,
                                             TicketMessageNo messageNo,
                                             Long ticketId,
                                             TicketParticipantType senderType,
                                             @Nullable Long senderUserId,
                                             TicketMessageType messageType,
                                             @Nullable String content,
                                             @Nullable List<String> attachments,
                                             @Nullable String metadata,
                                             @Nullable String clientMessageId,
                                             LocalDateTime sentAt,
                                             @Nullable LocalDateTime editedAt,
                                             @Nullable LocalDateTime recalledAt,
                                             LocalDateTime createdAt,
                                             LocalDateTime updatedAt) {
        TicketMessage message = new TicketMessage(
                id,
                messageNo,
                ticketId,
                senderType,
                senderUserId,
                messageType,
                normalizeNullableField(content, "content 不能为空", value -> value.length() <= 4000, "content 长度不能超过 4000"),
                normalizeAttachments(attachments),
                normalizeNullableField(metadata, "metadata 不能为空", value -> value.length() <= 20000, "metadata 长度不能超过 20000"),
                normalizeNullableField(clientMessageId, "clientMessageId 不能为空", value -> value.length() <= 64, "clientMessageId 长度不能超过 64"),
                sentAt,
                editedAt,
                recalledAt,
                createdAt,
                updatedAt
        );
        message.validate();
        return message;
    }

    /**
     * 编辑消息文本内容
     *
     * @param newContent 新文本内容
     */
    public void editContent(String newContent) {
        if (recalledAt != null)
            throw new ConflictException("已撤回消息不允许编辑");
        this.content = normalizeNullableField(newContent, "content 不能为空", value -> value.length() <= 4000, "content 长度不能超过 4000");
        this.editedAt = LocalDateTime.now();
        this.updatedAt = this.editedAt;
        validate();
    }

    /**
     * 撤回消息
     */
    public void recall() {
        if (recalledAt != null)
            throw new ConflictException("消息已撤回");
        this.recalledAt = LocalDateTime.now();
        this.updatedAt = this.recalledAt;
    }

    /**
     * 校验工单消息实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(messageNo, "messageNo 不能为空");
        requireNotNull(ticketId, "ticketId 不能为空");
        require(ticketId > 0, "ticketId 必须大于 0");
        requireNotNull(senderType, "senderType 不能为空");
        if (senderType.requiresUserId()) {
            requireNotNull(senderUserId, "当前 senderType 必须提供 senderUserId");
            require(senderUserId > 0, "senderUserId 必须大于 0");
        } else if (senderUserId != null)
            require(senderUserId > 0, "senderUserId 必须大于 0");
        requireNotNull(messageType, "messageType 不能为空");
        if (content != null)
            require(content.length() <= 4000, "content 长度不能超过 4000");
        requireNotNull(attachments, "attachments 不能为空");
        require(content != null || !attachments.isEmpty(), "content 与 attachments 不能同时为空");
        if (clientMessageId != null)
            require(clientMessageId.length() <= 64, "clientMessageId 长度不能超过 64");
        requireNotNull(sentAt, "sentAt 不能为空");
        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");
        if (editedAt != null)
            require(!editedAt.isBefore(sentAt), "editedAt 不可早于 sentAt");
        if (recalledAt != null)
            require(!recalledAt.isBefore(sentAt), "recalledAt 不可早于 sentAt");
    }

    /**
     * 规范化附件列表, 并执行去重
     *
     * @param values 原始附件列表
     * @return 规范化后的附件列表
     */
    private static List<String> normalizeAttachments(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 20, "attachments 元素数量不能超过 20");
        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNullableField(value, "attachments 元素不能为空", item -> item.length() <= 2048,
                    "attachments 元素长度不能超过 2048");
            requireNotNull(normalized, "attachments 元素不能为空");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }
}

package shopping.international.infrastructure.dao.customerservice.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象, cs_ticket_message 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("cs_ticket_message")
public class CsTicketMessagePO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 消息编号
     */
    @TableField("message_no")
    private String messageNo;
    /**
     * 工单 ID
     */
    @TableField("ticket_id")
    private Long ticketId;
    /**
     * 发送方类型
     */
    @TableField("sender_type")
    private String senderType;
    /**
     * 发送方用户 ID
     */
    @TableField("sender_user_id")
    private Long senderUserId;
    /**
     * 消息类型
     */
    @TableField("message_type")
    private String messageType;
    /**
     * 消息内容
     */
    @TableField("content")
    private String content;
    /**
     * 附件 JSON
     */
    @TableField("attachments")
    private String attachments;
    /**
     * 扩展元数据 JSON
     */
    @TableField("metadata")
    private String metadata;
    /**
     * 客户端消息幂等键
     */
    @TableField("client_message_id")
    private String clientMessageId;
    /**
     * 发送时间
     */
    @TableField("sent_at")
    private LocalDateTime sentAt;
    /**
     * 编辑时间
     */
    @TableField("edited_at")
    private LocalDateTime editedAt;
    /**
     * 撤回时间
     */
    @TableField("recalled_at")
    private LocalDateTime recalledAt;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}


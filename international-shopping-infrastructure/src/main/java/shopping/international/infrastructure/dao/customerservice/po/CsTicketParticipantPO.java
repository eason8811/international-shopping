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
 * 持久化对象, cs_ticket_participant 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("cs_ticket_participant")
public class CsTicketParticipantPO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 工单 ID
     */
    @TableField("ticket_id")
    private Long ticketId;
    /**
     * 参与方类型
     */
    @TableField("participant_type")
    private String participantType;
    /**
     * 参与方用户 ID
     */
    @TableField("participant_user_id")
    private Long participantUserId;
    /**
     * 参与方角色
     */
    @TableField("role")
    private String role;
    /**
     * 加入时间
     */
    @TableField("joined_at")
    private LocalDateTime joinedAt;
    /**
     * 离开时间
     */
    @TableField("left_at")
    private LocalDateTime leftAt;
    /**
     * 最后已读消息 ID
     */
    @TableField("last_read_message_id")
    private Long lastReadMessageId;
    /**
     * 最后已读时间
     */
    @TableField("last_read_at")
    private LocalDateTime lastReadAt;
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

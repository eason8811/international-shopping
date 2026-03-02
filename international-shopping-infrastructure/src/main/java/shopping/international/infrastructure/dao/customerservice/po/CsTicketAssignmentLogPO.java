package shopping.international.infrastructure.dao.customerservice.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象, cs_ticket_assignment_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cs_ticket_assignment_log")
public class CsTicketAssignmentLogPO {

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
     * 原指派用户 ID
     */
    @TableField("from_assignee_user_id")
    private Long fromAssigneeUserId;
    /**
     * 新指派用户 ID
     */
    @TableField("to_assignee_user_id")
    private Long toAssigneeUserId;
    /**
     * 指派动作类型
     */
    @TableField("action_type")
    private String actionType;
    /**
     * 操作者类型
     */
    @TableField("actor_type")
    private String actorType;
    /**
     * 操作者用户 ID
     */
    @TableField("actor_user_id")
    private Long actorUserId;
    /**
     * 来源引用 ID
     */
    @TableField("source_ref")
    private String sourceRef;
    /**
     * 备注
     */
    @TableField("note")
    private String note;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

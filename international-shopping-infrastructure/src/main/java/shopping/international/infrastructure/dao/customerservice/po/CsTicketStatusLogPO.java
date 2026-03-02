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
 * 持久化对象, cs_ticket_status_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "createdAt")
@TableName("cs_ticket_status_log")
public class CsTicketStatusLogPO {

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
     * 原状态
     */
    @TableField("from_status")
    private String fromStatus;
    /**
     * 目标状态
     */
    @TableField("to_status")
    private String toStatus;
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

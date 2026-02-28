package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单消息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageRespond {
    /**
     * 消息 ID
     */
    @NotNull
    private Long id;
    /**
     * 消息编号
     */
    @NotNull
    private String messageNo;
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 发送方类型
     */
    @NotNull
    private String senderType;
    /**
     * 发送方用户 ID
     */
    @Nullable
    private Long senderUserId;
    /**
     * 消息类型
     */
    @NotNull
    private String messageType;
    /**
     * 消息内容
     */
    @Nullable
    private String content;
    /**
     * 附件链接列表
     */
    @Nullable
    private List<String> attachments;
    /**
     * 客户端消息幂等键
     */
    @Nullable
    private String clientMessageId;
    /**
     * 发送时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
    /**
     * 编辑时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editedAt;
    /**
     * 撤回时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recalledAt;
}

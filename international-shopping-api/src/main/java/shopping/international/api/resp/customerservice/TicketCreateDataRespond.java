package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * 工单创建结果响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateDataRespond {
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 工单编号
     */
    @NotNull
    private String ticketNo;
    /**
     * 工单状态
     */
    @NotNull
    private String status;
    /**
     * 创建时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

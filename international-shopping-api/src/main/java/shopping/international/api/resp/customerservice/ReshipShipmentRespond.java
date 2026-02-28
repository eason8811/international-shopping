package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * 补发单关联物流单响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReshipShipmentRespond {
    /**
     * 补发单 ID
     */
    @NotNull
    private Long reshipId;
    /**
     * 物流单 ID
     */
    @NotNull
    private Long shipmentId;
    /**
     * 关联创建时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

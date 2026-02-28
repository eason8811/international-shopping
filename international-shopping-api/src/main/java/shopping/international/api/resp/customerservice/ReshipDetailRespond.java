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
 * 补发单详情响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReshipDetailRespond {
    /**
     * 补发单 ID
     */
    @NotNull
    private Long id;
    /**
     * 补发单编号
     */
    @NotNull
    private String reshipNo;
    /**
     * 订单 ID
     */
    @NotNull
    private Long orderId;
    /**
     * 关联工单 ID
     */
    @Nullable
    private Long ticketId;
    /**
     * 补发原因编码
     */
    @NotNull
    private String reasonCode;
    /**
     * 补发状态
     */
    @NotNull
    private String status;
    /**
     * 币种代码
     */
    @NotNull
    private String currency;
    /**
     * 货品成本（分）
     */
    @Nullable
    private Long itemsCost;
    /**
     * 运费成本（分）
     */
    @Nullable
    private Long shippingCost;
    /**
     * 备注信息
     */
    @Nullable
    private String note;
    /**
     * 创建时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    /**
     * 补发明细列表
     */
    @Nullable
    private List<ReshipItemRespond> items;
    /**
     * 关联物流单列表
     */
    @Nullable
    private List<ReshipShipmentRespond> shipments;
}

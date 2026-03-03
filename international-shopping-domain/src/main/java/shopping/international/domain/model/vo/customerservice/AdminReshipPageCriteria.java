package shopping.international.domain.model.vo.customerservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧补发单分页查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipPageCriteria implements Verifiable {

    /**
     * 补发单号
     */
    @Nullable
    private String reshipNo;
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 工单 ID
     */
    @Nullable
    private Long ticketId;
    /**
     * 补发状态
     */
    @Nullable
    private ReshipStatus status;
    /**
     * 补发原因编码
     */
    @Nullable
    private ReshipReasonCode reasonCode;
    /**
     * 创建时间起始
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间结束
     */
    @Nullable
    private LocalDateTime createdTo;

    /**
     * 校验分页查询条件
     */
    @Override
    public void validate() {
        if (orderId != null)
            require(orderId >= 1 ,"orderId 必须大于等于 1");
        if (ticketId != null)
            require(ticketId >= 1 ,"ticketId 必须大于等于 1");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo) ,"createdFrom 不能晚于 createdTo");
    }
}

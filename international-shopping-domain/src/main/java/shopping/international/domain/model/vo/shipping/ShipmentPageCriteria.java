package shopping.international.domain.model.vo.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 物流单分页查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPageCriteria implements Verifiable {

    /**
     * 物流单号
     */
    @Nullable
    private String shipmentNo;
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
    /**
     * 订单主键
     */
    @Nullable
    private Long orderId;
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 三方外部单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 状态集合
     */
    @Nullable
    private List<ShipmentStatus> statusIn;
    /**
     * 更新时间起始
     */
    @Nullable
    private LocalDateTime updatedFrom;
    /**
     * 更新时间结束
     */
    @Nullable
    private LocalDateTime updatedTo;
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
     * 排序字段, 仅允许白名单字段
     */
    @Builder.Default
    private String sortField = "updated_at";
    /**
     * 排序方向
     */
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * 校验并规范化查询条件
     */
    @Override
    public void validate() {
        if (orderId != null)
            require(orderId > 0, "orderId 必须大于 0");
        if (updatedFrom != null && updatedTo != null)
            require(!updatedFrom.isAfter(updatedTo), "updatedFrom 不能晚于 updatedTo");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");

        if (statusIn == null || statusIn.isEmpty())
            statusIn = List.of(ShipmentStatus.CREATED, ShipmentStatus.LABEL_CREATED);
        else {
            Set<ShipmentStatus> dedup = new LinkedHashSet<>(statusIn);
            statusIn = new ArrayList<>(dedup);
        }

        if (sortField == null || sortField.isBlank())
            sortField = "updated_at";
        else
            sortField = sortField.strip().toLowerCase();
        require("updated_at".equals(sortField) || "created_at".equals(sortField) || "id".equals(sortField),
                "sortField 仅支持 updated_at, created_at, id");

        if (sortDirection == null || sortDirection.isBlank())
            sortDirection = "desc";
        else
            sortDirection = sortDirection.strip().toLowerCase();
        require("asc".equals(sortDirection) || "desc".equals(sortDirection), "sortDirection 仅支持 asc 或 desc");
    }
}

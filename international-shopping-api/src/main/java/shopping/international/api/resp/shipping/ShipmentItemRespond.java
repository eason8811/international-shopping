package shopping.international.api.resp.shipping;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 物流单商品映射响应对象 (ShipmentItemRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentItemRespond {
    /**
     * 主键 ID
     */
    @Nullable
    private Long id;
    /**
     * 物流单 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 订单明细 ID
     */
    @Nullable
    private Long orderItemId;
    /**
     * 商品 SPU ID
     */
    @Nullable
    private Long productId;
    /**
     * 商品 SKU ID
     */
    @Nullable
    private Long skuId;
    /**
     * 发货数量
     */
    @Nullable
    private Integer quantity;
    /**
     * 创建时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

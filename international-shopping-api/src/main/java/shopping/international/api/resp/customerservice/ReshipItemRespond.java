package shopping.international.api.resp.customerservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 补发单明细响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReshipItemRespond {
    /**
     * 明细 ID
     */
    @NotNull
    private Long id;
    /**
     * 补发单 ID
     */
    @NotNull
    private Long reshipId;
    /**
     * 原订单明细 ID
     */
    @NotNull
    private Long orderItemId;
    /**
     * SKU ID
     */
    @NotNull
    private Long skuId;
    /**
     * 补发数量
     */
    @NotNull
    private Integer quantity;
}

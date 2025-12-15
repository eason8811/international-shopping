package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单预览响应 (OrderPreviewRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewRespond {
    /**
     * 预览明细列表
     */
    private List<OrderItemRespond> items;
    /**
     * 购买件数
     */
    private Integer itemsCount;
    /**
     * 订单总金额 (金额字符串)
     */
    private String totalAmount;
    /**
     * 折扣金额 (金额字符串)
     */
    private String discountAmount;
    /**
     * 运费金额 (金额字符串)
     */
    private String shippingAmount;
    /**
     * 实付金额 (金额字符串)
     */
    private String payAmount;
    /**
     * 币种
     */
    private String currency;
    /**
     * 折扣拆分明细 (可为空)
     */
    private List<DiscountAppliedViewRespond> discountBreakdown;
}


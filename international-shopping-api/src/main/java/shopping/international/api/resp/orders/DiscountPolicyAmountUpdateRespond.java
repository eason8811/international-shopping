package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 折扣策略金额配置更新响应
 *
 * <p>用于 FX_AUTO 重算 / 模式切换等操作</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyAmountUpdateRespond {
    /**
     * 折扣策略 ID
     */
    private Long policyId;
    /**
     * 受影响的币种列表
     */
    private List<String> currencies;
}


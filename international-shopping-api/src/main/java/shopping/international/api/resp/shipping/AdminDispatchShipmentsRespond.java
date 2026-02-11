package shopping.international.api.resp.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 管理侧批量发货结果响应对象 (AdminDispatchShipmentsRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDispatchShipmentsRespond {
    /**
     * 发货成功的物流单号列表
     */
    @Nullable
    private List<String> shipmentIds;
}

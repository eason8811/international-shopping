package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧补发单绑定物流单请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipBindShipmentsRequest implements Verifiable {
    /**
     * 物流单 ID 列表
     */
    @Nullable
    private List<Long> shipmentIds;

    /**
     * 对补发单绑定物流单参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(shipmentIds, "shipmentIds 不能为空");
        require(!shipmentIds.isEmpty(), "shipmentIds 不能为空数组");

        Set<Long> dedup = new LinkedHashSet<>();
        for (Long shipmentId : shipmentIds) {
            requireNotNull(shipmentId, "shipmentIds 元素不能为空");
            require(shipmentId >= 1, "shipmentIds 元素必须大于等于 1");
            dedup.add(shipmentId);
        }

        shipmentIds = new ArrayList<>(dedup);
    }
}

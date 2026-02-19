package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧批量发货请求对象 (AdminDispatchShipmentsRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDispatchShipmentsRequest implements Verifiable {
    /**
     * 待发货物流单 ID 列表
     */
    @NotNull
    private List<Long> shipmentIds;
    /**
     * 备注信息
     */
    @NotNull
    private String note;

    /**
     * 对批量发货参数进行校验与规范化
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

        note = normalizeNotNullField(note, "note 不能为空", s -> s.length() <= 255, "note 长度不能超过 255 个字符");
    }
}

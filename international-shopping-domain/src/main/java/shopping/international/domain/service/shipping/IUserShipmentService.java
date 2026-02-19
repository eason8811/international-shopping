package shopping.international.domain.service.shipping;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.ShipmentNo;

import java.util.List;

/**
 * 用户侧物流领域服务接口
 */
public interface IUserShipmentService {

    /**
     * 查询用户订单关联物流单列表
     *
     * @param userId      用户主键
     * @param orderNo     订单号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情列表
     */
    @NotNull
    List<Shipment> listMyOrderShipments(@NotNull Long userId,
                                        @NotNull OrderNo orderNo,
                                        boolean includeLogs);

    /**
     * 查询用户物流单详情
     *
     * @param userId     用户主键
     * @param shipmentNo 物流单号
     * @return 物流单详情
     */
    @NotNull
    Shipment getMyShipment(@NotNull Long userId,
                                     @NotNull ShipmentNo shipmentNo);
}

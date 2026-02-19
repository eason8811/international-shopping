package shopping.international.domain.service.shipping.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.adapter.repository.shipping.IShipmentRepository;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.ShipmentNo;
import shopping.international.domain.service.shipping.IUserShipmentService;
import shopping.international.types.exceptions.NotFoundException;

import java.util.List;

/**
 * 用户侧物流领域服务实现
 */
@Service
@RequiredArgsConstructor
public class UserShipmentService implements IUserShipmentService {

    /**
     * 物流仓储
     */
    private final IShipmentRepository shipmentRepository;
    /**
     * 订单仓储
     */
    private final IOrderRepository orderRepository;

    /**
     * 查询用户订单关联物流单列表
     *
     * @param userId      用户主键
     * @param orderNo     订单号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情列表
     */
    @Override
    public @NotNull List<Shipment> listMyOrderShipments(@NotNull Long userId,
                                                        @NotNull OrderNo orderNo,
                                                        boolean includeLogs) {
        boolean orderExists = orderRepository.findUserOrderDetail(userId, orderNo).isPresent();
        if (!orderExists)
            throw new NotFoundException("订单不存在");
        return shipmentRepository.listUserOrderShipments(userId, orderNo, includeLogs);
    }

    /**
     * 查询用户物流单详情
     *
     * @param userId     用户主键
     * @param shipmentNo 物流单号
     * @return 物流单详情
     */
    @Override
    public @NotNull Shipment getMyShipment(@NotNull Long userId,
                                                     @NotNull ShipmentNo shipmentNo) {
        return shipmentRepository.findUserShipmentDetail(userId, shipmentNo)
                .orElseThrow(() -> new NotFoundException("物流单不存在"));
    }
}

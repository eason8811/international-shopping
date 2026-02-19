package shopping.international.trigger.controller.shipping;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.shipping.ShipmentDetailRespond;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.ShipmentNo;
import shopping.international.domain.service.shipping.IUserShipmentService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧物流控制器, 提供订单物流查询能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX)
public class UserShipmentController {

    /**
     * 用户侧物流领域服务
     */
    private final IUserShipmentService userShipmentService;

    /**
     * 查询当前用户订单关联的物流单列表
     *
     * @param orderNo     订单号
     * @param includeLogs 是否返回状态日志
     * @return 物流单详情列表
     */
    @GetMapping("/users/me/orders/{order_no}/shipments")
    public ResponseEntity<Result<List<ShipmentDetailRespond>>> listMyOrderShipments(@PathVariable("order_no") String orderNo,
                                                                                    @RequestParam(value = "include_logs", required = false) Boolean includeLogs) {
        orderNo = normalizeNotNullField(orderNo, "orderNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "orderNo 长度需在 10~32 个字符之间");
        if (includeLogs == null)
            includeLogs = true;

        Long userId = requireCurrentUserId();
        List<Shipment> shipmentList = userShipmentService.listMyOrderShipments(
                userId,
                OrderNo.of(orderNo),
                includeLogs
        );
        List<ShipmentDetailRespond> data = shipmentList.stream()
                .map(ShipmentRespondAssembler::toShipmentDetailRespond)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 查询当前用户物流单详情
     *
     * @param shipmentNo 物流单号
     * @return 物流单详情
     */
    @GetMapping("/users/me/shipments/{shipment_no}")
    public ResponseEntity<Result<ShipmentDetailRespond>> getMyShipment(@PathVariable("shipment_no") String shipmentNo) {
        shipmentNo = normalizeNotNullField(shipmentNo, "shipmentNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "shipmentNo 长度需在 10~32 个字符之间");

        Long userId = requireCurrentUserId();
        Shipment shipment = userShipmentService.getMyShipment(userId, ShipmentNo.of(shipmentNo));
        return ResponseEntity.ok(Result.ok(ShipmentRespondAssembler.toShipmentDetailRespond(shipment)));
    }

    /**
     * 从安全上下文读取当前用户主键
     *
     * @return 当前用户主键
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId)
            return userId;
        if (principal instanceof String userId)
            return Long.parseLong(userId);
        throw new AccountException("无法解析当前用户");
    }
}

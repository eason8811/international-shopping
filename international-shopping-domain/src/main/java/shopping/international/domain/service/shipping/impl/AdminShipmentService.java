package shopping.international.domain.service.shipping.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.shipping.ISeventeenTrackPort;
import shopping.international.domain.adapter.repository.shipping.IShipmentRepository;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.shipping.*;
import shopping.international.domain.service.shipping.IAdminShipmentService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧物流领域服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminShipmentService implements IAdminShipmentService {

    /**
     * 物流仓储
     */
    private final IShipmentRepository shipmentRepository;
    /**
     * 17Track 端口
     */
    private final ISeventeenTrackPort seventeenTrackPort;

    /**
     * 管理侧分页查询物流单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<ShipmentSummaryView> pageShipments(@NotNull ShipmentPageCriteria criteria,
                                                                  @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return shipmentRepository.pageShipments(criteria, pageQuery);
    }

    /**
     * 管理侧查询物流单详情
     *
     * @param shipmentId 物流单主键
     * @return 物流单详情
     */
    @Override
    public @NotNull Shipment getShipmentDetail(@NotNull Long shipmentId) {
        return shipmentRepository.findShipmentDetailById(shipmentId, true)
                .orElseThrow(() -> new NotFoundException("物流单不存在"));
    }

    /**
     * 管理侧回填面单
     *
     * @param shipmentId        物流单主键
     * @param label             面单信息
     * @param shipFromAddressId 物流单寄出地址 ID (user_address.id)
     * @param idempotencyKey    请求幂等键
     * @param sourceRef         来源引用
     * @param actorUserId       操作者主键
     * @param note              备注
     * @return 更新后的物流单
     */
    @Override
    public @NotNull Shipment fillShipmentLabel(@NotNull Long shipmentId,
                                               @NotNull ShipmentLabel label,
                                               @NotNull Integer shipFromAddressId,
                                               @NotNull String idempotencyKey,
                                               @NotNull String sourceRef,
                                               @Nullable Long actorUserId,
                                               @Nullable String note) {
        IShipmentRepository.FillLabelResult fillLabelResult = shipmentRepository.fillLabel(
                shipmentId,
                label,
                shipFromAddressId,
                idempotencyKey,
                sourceRef,
                actorUserId,
                note
        );
        Shipment shipment = fillLabelResult.shipment();
        String trackingNo = shipment.getTrackingNo();
        require(trackingNo != null && !trackingNo.isBlank(), "trackingNo 不能为空");

        String trackingDigest = shortTrackingDigest(trackingNo);
        String registerSuccessSourceRef = "shipment:" + shipmentId + ":17track:registered:" + trackingDigest;
        String registerOpIdempotencyKey = "shipment:" + shipmentId + ":17track:register:" + trackingDigest;
        boolean hasRegisterSuccessLog = hasApiLog(shipment, registerSuccessSourceRef);

        // 重放场景且已有“注册成功”标记时, 直接返回同一结果
        if (fillLabelResult.replayed() && hasRegisterSuccessLog)
            return shipment;

        if (!hasRegisterSuccessLog) {
            // 事务外调用外部 API, 避免长事务占锁
            try {
                seventeenTrackPort.registerTracking(
                        new ISeventeenTrackPort.RegisterTrackingCommand(
                                trackingNo,
                                shipment.getCarrierCode(),
                                registerOpIdempotencyKey
                        )
                );
            } catch (Exception exception) {
                throw new ConflictException("17Track 注册单号失败, 请重试, " + exception.getMessage());
            }

            shipmentRepository.applyTrackingEvent(
                    shipmentId,
                    ShipmentTrackingEvent.keepCurrent(
                            LocalDateTime.now(),
                            ShipmentStatusEventSource.API,
                            registerSuccessSourceRef,
                            shipment.getCarrierCode(),
                            trackingNo,
                            "17Track 注册单号成功",
                            Map.of("idempotency_key", registerOpIdempotencyKey),
                            actorUserId
                    )
            );
        }

        return shipmentRepository.findShipmentDetailById(shipmentId, true).orElse(shipment);
    }

    /**
     * 管理侧批量发货
     *
     * @param shipmentIds    物流单主键列表
     * @param idempotencyKey 请求幂等键
     * @param sourceRef      来源引用
     * @param note           备注
     * @param actorUserId    操作者主键
     * @return 更新后的物流单列表
     */
    @Override
    public @NotNull List<Shipment> dispatchShipments(@NotNull List<Long> shipmentIds,
                                                     @NotNull String idempotencyKey,
                                                     @NotNull String sourceRef,
                                                     @NotNull String note,
                                                     @Nullable Long actorUserId) {
        require(!shipmentIds.isEmpty(), "shipmentIds 不能为空");
        return shipmentRepository.dispatch(shipmentIds, idempotencyKey, sourceRef, note, actorUserId);
    }

    /**
     * 管理侧手工创建物流单
     *
     * @param command               创建命令
     * @param requestIdempotencyKey 请求幂等键
     * @param actorUserId           操作者主键
     * @return 创建后的物流单
     */
    @Override
    public @NotNull Shipment manualCreateShipment(@NotNull ManualCreateShipmentCommand command,
                                                  @NotNull String requestIdempotencyKey,
                                                  @Nullable Long actorUserId) {
        command.validate();
        return shipmentRepository.manualCreate(command, requestIdempotencyKey, actorUserId);
    }

    /**
     * 管理侧分页查询物流状态日志
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<ShipmentStatusLog> pageStatusLogs(@NotNull ShipmentStatusLogPageCriteria criteria,
                                                                 @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return shipmentRepository.pageStatusLogs(criteria, pageQuery);
    }

    /**
     * 低频补偿任务, 补建 PAID 且无物流单的订单
     *
     * @param limit           批次数量
     * @param sourceRefPrefix 来源引用前缀
     * @return 本批补建数量
     */
    @Override
    public int compensatePaidOrdersWithoutShipment(int limit,
                                                   @NotNull String sourceRefPrefix) {
        require(limit > 0, "limit 必须大于 0");
        return shipmentRepository.compensatePaidOrdersWithoutShipment(limit, sourceRefPrefix);
    }

    /**
     * 判断物流单是否存在指定来源引用的 API 日志
     *
     * @param shipment 物流单
     * @param sourceRef 来源引用
     * @return true 表示日志已存在
     */
    private boolean hasApiLog(@NotNull Shipment shipment, @NotNull String sourceRef) {
        return shipment.getStatusLogList().stream()
                .anyMatch(log -> log.getSourceType() == ShipmentStatusEventSource.API
                        && Objects.equals(log.getSourceRef(), sourceRef));
    }

    /**
     * 计算追踪号短摘要, 用于构造稳定的来源引用
     *
     * @param trackingNo 追踪号
     * @return 短摘要
     */
    private static @NotNull String shortTrackingDigest(@NotNull String trackingNo) {
        require(!trackingNo.isBlank(), "trackingNo 不能为空");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hex = HexFormat.of().formatHex(digest.digest(trackingNo.getBytes(StandardCharsets.UTF_8)));
            return hex.substring(0, 16);
        } catch (Exception exception) {
            throw new IllegalStateException("计算 trackingNo 摘要失败, " + exception.getMessage(), exception);
        }
    }
}

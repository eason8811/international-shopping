package shopping.international.domain.model.entity.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 补发单和物流单关联实体, 对应表 `aftersales_reship_shipment`
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"reshipId", "shipmentId"})
@NoArgsConstructor
@Accessors(chain = true)
public class ReshipShipment implements Verifiable {

    /**
     * 补发单 ID
     */
    @Nullable
    private Long reshipId;
    /**
     * 物流单 ID
     */
    private Long shipmentId;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造补发单和物流单关联实体
     *
     * @param reshipId   补发单 ID
     * @param shipmentId 物流单 ID
     * @param createdAt  创建时间
     */
    private ReshipShipment(@Nullable Long reshipId, Long shipmentId, LocalDateTime createdAt) {
        this.reshipId = reshipId;
        this.shipmentId = shipmentId;
        this.createdAt = createdAt;
    }

    /**
     * 创建补发单和物流单关联实体
     *
     * @param reshipId   补发单 ID
     * @param shipmentId 物流单 ID
     * @return 新建的补发单和物流单关联实体
     */
    public static ReshipShipment create(@Nullable Long reshipId, Long shipmentId) {
        ReshipShipment relation = new ReshipShipment(reshipId, shipmentId, LocalDateTime.now());
        relation.validate();
        return relation;
    }

    /**
     * 从持久化数据重建补发单和物流单关联实体
     *
     * @param reshipId   补发单 ID
     * @param shipmentId 物流单 ID
     * @param createdAt  创建时间
     * @return 重建后的补发单和物流单关联实体
     */
    public static ReshipShipment reconstitute(@Nullable Long reshipId, Long shipmentId, LocalDateTime createdAt) {
        ReshipShipment relation = new ReshipShipment(reshipId, shipmentId, createdAt);
        relation.validate();
        return relation;
    }

    /**
     * 校验补发单和物流单关联实体不变式
     */
    @Override
    public void validate() {
        if (reshipId != null)
            require(reshipId > 0, "reshipId 必须大于 0");
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(createdAt, "createdAt 不能为空");
    }
}

package shopping.international.domain.model.entity.shipping;

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
 * 物流单商品映射实体
 *
 * <p>对应表 {@code shipment_item}, 用于表达包裹与订单明细的 N:N 关系</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class ShipmentItem implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 物流单 ID, 未持久化前可为空
     */
    @Nullable
    private Long shipmentId;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单明细 ID
     */
    private Long orderItemId;
    /**
     * 商品 SPU ID
     */
    private Long productId;
    /**
     * 商品 SKU ID
     */
    private Long skuId;
    /**
     * 发货数量
     */
    private int quantity;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造物流单商品映射实体
     *
     * @param id 主键 ID
     * @param shipmentId 物流单 ID
     * @param orderId 订单 ID
     * @param orderItemId 订单明细 ID
     * @param productId 商品 SPU ID
     * @param skuId 商品 SKU ID
     * @param quantity 发货数量
     * @param createdAt 创建时间
     */
    private ShipmentItem(@Nullable Long id,
                         @Nullable Long shipmentId,
                         Long orderId,
                         Long orderItemId,
                         Long productId,
                         Long skuId,
                         int quantity,
                         LocalDateTime createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    /**
     * 创建新的物流单商品映射
     *
     * @param orderId 订单 ID
     * @param orderItemId 订单明细 ID
     * @param productId 商品 SPU ID
     * @param skuId 商品 SKU ID
     * @param quantity 发货数量
     * @return 新建实体
     */
    public static ShipmentItem create(Long orderId,
                                      Long orderItemId,
                                      Long productId,
                                      Long skuId,
                                      int quantity) {
        ShipmentItem item = new ShipmentItem(null, null, orderId, orderItemId, productId, skuId, quantity, LocalDateTime.now());
        item.validate();
        return item;
    }

    /**
     * 从持久化层重建物流单商品映射
     *
     * @param id 主键 ID
     * @param shipmentId 物流单 ID
     * @param orderId 订单 ID
     * @param orderItemId 订单明细 ID
     * @param productId 商品 SPU ID
     * @param skuId 商品 SKU ID
     * @param quantity 发货数量
     * @param createdAt 创建时间
     * @return 重建后的实体
     */
    public static ShipmentItem reconstitute(Long id,
                                            Long shipmentId,
                                            Long orderId,
                                            Long orderItemId,
                                            Long productId,
                                            Long skuId,
                                            int quantity,
                                            LocalDateTime createdAt) {
        ShipmentItem item = new ShipmentItem(id, shipmentId, orderId, orderItemId, productId, skuId, quantity, createdAt);
        item.validate();
        return item;
    }

    /**
     * 绑定物流单 ID
     *
     * @param shipmentId 物流单 ID
     */
    public void bindShipmentId(Long shipmentId) {
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        if (this.shipmentId != null)
            require(this.shipmentId.equals(shipmentId), "shipmentId 不允许被修改");
        this.shipmentId = shipmentId;
    }

    /**
     * 校验实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        if (shipmentId != null)
            require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(orderId, "orderId 不能为空");
        require(orderId > 0, "orderId 必须大于 0");
        requireNotNull(orderItemId, "orderItemId 不能为空");
        require(orderItemId > 0, "orderItemId 必须大于 0");
        requireNotNull(productId, "productId 不能为空");
        require(productId > 0, "productId 必须大于 0");
        requireNotNull(skuId, "skuId 不能为空");
        require(skuId > 0, "skuId 必须大于 0");
        require(quantity > 0, "quantity 必须大于 0");
        requireNotNull(createdAt, "createdAt 不能为空");
    }
}

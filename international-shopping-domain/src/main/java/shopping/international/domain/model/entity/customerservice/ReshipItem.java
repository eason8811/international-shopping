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
 * 补发明细实体, 对应表 `aftersales_reship_item`
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class ReshipItem implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 补发单 ID
     */
    @Nullable
    private Long reshipId;
    /**
     * 原订单明细 ID
     */
    private Long orderItemId;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 补发数量
     */
    private Integer quantity;
    /**
     * 这一项的补发明细成本, Minor 形式
     */
    private Long amount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造补发明细实体
     *
     * @param id          主键 ID
     * @param reshipId    补发单 ID
     * @param orderItemId 原订单明细 ID
     * @param skuId       SKU ID
     * @param quantity    补发数量
     * @param amount      这一项的补发明细成本, Minor 形式
     * @param createdAt   创建时间
     */
    private ReshipItem(@Nullable Long id,
                       @Nullable Long reshipId,
                       Long orderItemId,
                       Long skuId,
                       Integer quantity,
                       Long amount,
                       LocalDateTime createdAt) {
        this.id = id;
        this.reshipId = reshipId;
        this.orderItemId = orderItemId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    /**
     * 创建补发明细实体
     *
     * @param reshipId    补发单 ID
     * @param orderItemId 原订单明细 ID
     * @param skuId       SKU ID
     * @param quantity    补发数量
     * @return 新建的补发明细实体
     */
    public static ReshipItem create(@Nullable Long reshipId, Long orderItemId, Long skuId, Integer quantity, Long amount) {
        ReshipItem item = new ReshipItem(null, reshipId, orderItemId, skuId, quantity, amount, LocalDateTime.now());
        item.validate();
        return item;
    }

    /**
     * 从持久化数据重建补发明细实体
     *
     * @param id          主键 ID
     * @param reshipId    补发单 ID
     * @param orderItemId 原订单明细 ID
     * @param skuId       SKU ID
     * @param quantity    补发数量
     * @param amount      这一项的补发明细成本, Minor 形式
     * @param createdAt   创建时间
     * @return 重建后的补发明细实体
     */
    public static ReshipItem reconstitute(@Nullable Long id,
                                          @Nullable Long reshipId,
                                          Long orderItemId,
                                          Long skuId,
                                          Integer quantity,
                                          Long amount,
                                          LocalDateTime createdAt) {
        ReshipItem item = new ReshipItem(id, reshipId, orderItemId, skuId, quantity, amount, createdAt);
        item.validate();
        return item;
    }

    /**
     * 校验补发明细实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        if (reshipId != null)
            require(reshipId > 0, "reshipId 必须大于 0");
        requireNotNull(orderItemId, "orderItemId 不能为空");
        require(orderItemId > 0, "orderItemId 必须大于 0");
        requireNotNull(skuId, "skuId 不能为空");
        require(skuId > 0, "skuId 必须大于 0");
        requireNotNull(quantity, "quantity 不能为空");
        require(quantity > 0, "quantity 必须大于 0");
        requireNotNull(amount, "amount 不能为空");
        require(amount > 0, "amount 必须大于 0");
        requireNotNull(createdAt, "createdAt 不能为空");
    }
}

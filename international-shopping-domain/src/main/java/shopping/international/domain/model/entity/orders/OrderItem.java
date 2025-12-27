package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import shopping.international.domain.model.vo.orders.Money;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 订单明细实体 (对应表 order_item), 归属 Order 聚合
 *
 * <p>明细的核心字段在下单时即固化为快照 (标题/单价/属性/图片等)</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OrderItem implements Verifiable {
    /**
     * 订单明细主键 ID
     */
    private Long id;
    /**
     * 所属订单 ID
     */
    private Long orderId;
    /**
     * 关联的 SPU ID (冗余)
     */
    private Long productId;
    /**
     * 关联的 SKU ID
     */
    private Long skuId;
    /**
     * 使用的折扣码 ID
     */
    private Long discountCodeId;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品 SKU 属性, 键值对形式存储的额外信息
     */
    private Map<String, Object> skuAttrs;
    /**
     * 封面图片 URL
     */
    private String coverImageUrl;
    /**
     * 关联的 SKU 单价
     */
    private Money unitPrice;
    /**
     * SKU 的数量
     */
    private int quantity;
    /**
     * 订单明细的小计金额
     */
    private Money subtotalAmount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建一个新的订单明细快照 (用于下单时固化)
     *
     * @param productId      商品 ID, 不能为空
     * @param skuId          SKU ID, 不能为空
     * @param discountCodeId 优惠码 ID, 可为空
     * @param title          商品标题, 不能为空且不能仅包含空白字符
     * @param skuAttrs       SKU 属性, 键值对形式存储的额外信息
     * @param coverImageUrl  封面图片 URL, 可为空, 如果提供则不能仅包含空白字符
     * @param unitPrice      单价, 不能为空
     * @param quantity       数量, 必须大于 0
     * @return 新创建的 <code>OrderItem</code> 实例, 包含小计金额和创建时间
     */
    public static OrderItem snapshot(Long productId, Long skuId, Long discountCodeId, String title,
                                     Map<String, Object> skuAttrs, String coverImageUrl,
                                     Money unitPrice, int quantity) {
        requireNotNull(productId, "商品 ID 不能为空");
        requireNotNull(skuId, "SKU ID 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(unitPrice, "单价不能为空");
        require(quantity > 0, "数量必须大于 0");
        Money subtotal = unitPrice.multiply(quantity);
        return new OrderItem(null, null, productId, skuId, discountCodeId, title.strip(),
                skuAttrs, coverImageUrl == null ? null : coverImageUrl.strip(),
                unitPrice, quantity, subtotal, LocalDateTime.now());
    }

    /**
     * 从给定参数中重新构建一个 <code>OrderItem</code> 对象
     *
     * @param id             订单明细 ID, 必须提供
     * @param orderId        订单 ID, 必须提供
     * @param productId      商品 ID, 必须提供
     * @param skuId          SKU ID, 必须提供
     * @param discountCodeId 优惠码 ID, 可选, 如果提供则不能为无效值
     * @param title          商品标题, 必须提供且不能仅包含空白字符
     * @param skuAttrs       SKU 属性, 键值对形式存储的额外信息
     * @param coverImageUrl  封面图片 URL, 可选, 如果提供则不能仅包含空白字符
     * @param unitPrice      单价, 必须提供且有效
     * @param quantity       数量, 必须大于 0
     * @param subtotalAmount 小计金额, 必须提供
     * @param createdAt      创建时间, 必须提供
     * @return 新创建的 <code>OrderItem</code> 实例, 经过验证确保所有关键字段都符合预期的业务规则
     */
    public static OrderItem reconstitute(Long id, Long orderId, Long productId, Long skuId, Long discountCodeId,
                                         String title, Map<String, Object> skuAttrs, String coverImageUrl,
                                         Money unitPrice, int quantity, Money subtotalAmount, LocalDateTime createdAt) {
        OrderItem item = new OrderItem(id, orderId, productId, skuId, discountCodeId,
                title, skuAttrs, coverImageUrl, unitPrice, quantity, subtotalAmount, createdAt);
        item.validate();
        return item;
    }

    /**
     * 验证订单明细的有效性, 此方法确保所有关键字段都符合预期的业务规则, 包括但不限于:
     * <ul>
     *      <li>商品 ID 不能为空</li>
     *      <li>SKU ID 不能为空</li>
     *      <li>商品标题既不能为 null 也不能仅包含空白字符</li>
     *      <li>单价必须提供且有效</li>
     *      <li>数量必须大于 0</li>
     *      <li>小计金额需正确计算, 并与单价和数量相乘的结果一致</li>
     *      <li>所有涉及金额的操作使用相同的货币类型</li>
     * </ul>
     *
     * @throws IllegalParamException 如果任何验证条件不满足
     */
    @Override
    public void validate() {
        requireNotNull(productId, "商品 ID 不能为空");
        requireNotNull(skuId, "SKU ID 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(unitPrice, "单价不能为空");
        require(quantity > 0, "数量必须大于 0");
        requireNotNull(subtotalAmount, "小计不能为空");
        unitPrice.ensureSameCurrency(subtotalAmount);
        Money expected = unitPrice.multiply(quantity);
        require(expected.getAmount().compareTo(subtotalAmount.getAmount()) == 0, "订单明细小计不一致");
    }
}


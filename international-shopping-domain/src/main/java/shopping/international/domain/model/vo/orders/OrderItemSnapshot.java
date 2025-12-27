package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Collections;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 订单明细快照值对象 (用于下单/试算输出)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class OrderItemSnapshot implements Verifiable {
    /**
     * SPU ID
     */
    @NotNull
    private Long productId;
    /**
     * SKU ID
     */
    @NotNull
    private Long skuId;
    /**
     * 折扣码ID (可空, 表示未绑定或未应用)
     */
    @Nullable
    private Long discountCodeId;
    /**
     * 商品标题快照
     */
    @NotNull
    private String title;
    /**
     * SKU 属性快照 (可空)
     */
    @Nullable
    private Map<String, Object> skuAttrs = Collections.emptyMap();
    /**
     * 商品图快照 (可空)
     */
    @Nullable
    private String coverImageUrl;
    /**
     * 单价快照
     */
    @NotNull
    private Money unitPrice;
    /**
     * 数量
     */
    private int quantity;
    /**
     * 小计 (= unitPrice * quantity)
     */
    @NotNull
    private Money subtotalAmount;

    /**
     * 创建订单明细快照, 并自动计算小计
     *
     * @param productId      SPU ID
     * @param skuId          SKU ID
     * @param discountCodeId 折扣码ID, 可空
     * @param title          商品标题快照
     * @param skuAttrs       SKU 属性快照, 可空
     * @param coverImageUrl  商品图快照, 可空
     * @param unitPrice      单价快照
     * @param quantity       数量, 必须大于 0
     * @return 订单明细快照
     */
    public static OrderItemSnapshot of(@NotNull Long productId, @NotNull Long skuId, @Nullable Long discountCodeId,
                                       @NotNull String title, @Nullable Map<String, Object> skuAttrs,
                                       @Nullable String coverImageUrl, @NotNull Money unitPrice, int quantity) {
        requireNotNull(productId, "productId 不能为空");
        requireNotNull(skuId, "skuId 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(unitPrice, "unitPrice 不能为空");
        require(quantity > 0, "数量必须大于 0");
        Money subtotal = unitPrice.multiply(quantity);
        return OrderItemSnapshot.builder()
                .productId(productId)
                .skuId(skuId)
                .discountCodeId(discountCodeId)
                .title(title.strip())
                .skuAttrs(skuAttrs == null ? Collections.emptyMap() : skuAttrs)
                .coverImageUrl(coverImageUrl == null ? null : coverImageUrl.strip())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotalAmount(subtotal)
                .build();
    }

    /**
     * 校验明细快照字段与不变式
     *
     * <ul>
     *     <li>{@code productId/skuId/title/unitPrice/subtotalAmount} 必填</li>
     *     <li>{@code quantity} 必须大于 0</li>
     *     <li>{@code subtotalAmount} 必须等于 {@code unitPrice * quantity}</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(productId, "productId 不能为空");
        requireNotNull(skuId, "skuId 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(unitPrice, "unitPrice 不能为空");
        require(quantity > 0, "数量必须大于 0");
        requireNotNull(subtotalAmount, "subtotalAmount 不能为空");
        unitPrice.ensureSameCurrency(subtotalAmount);
        Money expected = unitPrice.multiply(quantity);
        require(expected.getAmount().compareTo(subtotalAmount.getAmount()) == 0, "小计不一致");
    }
}

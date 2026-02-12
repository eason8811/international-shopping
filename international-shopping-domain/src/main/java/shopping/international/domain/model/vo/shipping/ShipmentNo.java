package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.NoGenerator;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 物流单号值对象
 *
 * <p>对应表字段 {@code shipment.shipment_no}, 长度固定为 26 位</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ShipmentNo implements Verifiable {

    /**
     * 物流单号原始值
     */
    private String value;

    /**
     * 构造物流单号值对象
     *
     * @param value 物流单号
     */
    private ShipmentNo(String value) {
        this.value = value;
    }

    /**
     * 创建物流单号值对象
     *
     * @param value 物流单号文本
     * @return 物流单号值对象
     */
    public static @NotNull ShipmentNo of(@NotNull String value) {
        ShipmentNo shipmentNo = new ShipmentNo(value);
        shipmentNo.validate();
        return shipmentNo;
    }

    /**
     * 生成新的物流单号
     *
     * <p>生成策略与订单号一致, 采用 26 位 Crockford Base32 字符串</p>
     *
     * @return 新生成的物流单号值对象
     */
    public static @NotNull ShipmentNo generate() {
        return ShipmentNo.of(NoGenerator.generate());
    }

    /**
     * 校验物流单号合法性
     */
    @Override
    public void validate() {
        value = normalizeNotNullField(value, "shipmentNo 不能为空",
                text -> text.length() == 26,
                "shipmentNo 必须为 26 位");
    }
}

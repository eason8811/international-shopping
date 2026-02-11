package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 包裹尺寸与重量值对象
 *
 * <p>字段对应 {@code shipment.weight_kg/length_cm/width_cm/height_cm}</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ShipmentDimension implements Verifiable {

    /**
     * 毛重 (kg), 最多 3 位小数
     */
    @Nullable
    private final BigDecimal weightKg;
    /**
     * 长 (cm), 最多 1 位小数
     */
    @Nullable
    private final BigDecimal lengthCm;
    /**
     * 宽 (cm), 最多 1 位小数
     */
    @Nullable
    private final BigDecimal widthCm;
    /**
     * 高 (cm), 最多 1 位小数
     */
    @Nullable
    private final BigDecimal heightCm;

    /**
     * 构造包裹尺寸值对象
     *
     * @param weightKg 毛重
     * @param lengthCm 长度
     * @param widthCm 宽度
     * @param heightCm 高度
     */
    private ShipmentDimension(@Nullable BigDecimal weightKg,
                              @Nullable BigDecimal lengthCm,
                              @Nullable BigDecimal widthCm,
                              @Nullable BigDecimal heightCm) {
        this.weightKg = weightKg;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
    }

    /**
     * 基于数值创建包裹尺寸值对象
     *
     * @param weightKg 毛重, 可为空
     * @param lengthCm 长度, 可为空
     * @param widthCm 宽度, 可为空
     * @param heightCm 高度, 可为空
     * @return 包裹尺寸值对象
     */
    public static ShipmentDimension of(@Nullable BigDecimal weightKg,
                                       @Nullable BigDecimal lengthCm,
                                       @Nullable BigDecimal widthCm,
                                       @Nullable BigDecimal heightCm) {
        ShipmentDimension dimension = new ShipmentDimension(weightKg, lengthCm, widthCm, heightCm);
        dimension.validate();
        return dimension;
    }

    /**
     * 基于字符串创建包裹尺寸值对象
     *
     * @param weightKg 毛重字符串, 可为空
     * @param lengthCm 长度字符串, 可为空
     * @param widthCm 宽度字符串, 可为空
     * @param heightCm 高度字符串, 可为空
     * @return 包裹尺寸值对象
     */
    public static ShipmentDimension ofText(@Nullable String weightKg,
                                           @Nullable String lengthCm,
                                           @Nullable String widthCm,
                                           @Nullable String heightCm) {
        return ShipmentDimension.of(
                parseDecimal(weightKg, "weightKg", 3),
                parseDecimal(lengthCm, "lengthCm", 1),
                parseDecimal(widthCm, "widthCm", 1),
                parseDecimal(heightCm, "heightCm", 1)
        );
    }

    /**
     * 判断是否至少存在一个尺寸字段
     *
     * @return 若任一字段非空返回 {@code true}, 否则返回 {@code false}
     */
    public boolean hasAnyValue() {
        return weightKg != null || lengthCm != null || widthCm != null || heightCm != null;
    }

    /**
     * 校验包裹尺寸字段
     */
    @Override
    public void validate() {
        validateValue(weightKg, 3, "weightKg");
        validateValue(lengthCm, 1, "lengthCm");
        validateValue(widthCm, 1, "widthCm");
        validateValue(heightCm, 1, "heightCm");
    }

    /**
     * 校验单个尺寸字段
     *
     * @param value 数值
     * @param maxScale 最大小数位
     * @param field 字段名
     */
    private static void validateValue(@Nullable BigDecimal value, int maxScale, String field) {
        if (value == null)
            return;
        require(value.compareTo(BigDecimal.ZERO) >= 0, field + " 不能为负数");
        require(value.scale() <= maxScale, field + " 最多保留 " + maxScale + " 位小数");
    }

    /**
     * 解析十进制文本
     *
     * @param raw 原始文本
     * @param field 字段名
     * @param maxScale 最大小数位
     * @return 解析后的数值, 空值返回 {@code null}
     */
    private static @Nullable BigDecimal parseDecimal(@Nullable String raw, String field, int maxScale) {
        String normalized = normalizeNullableField(raw, field + " 不能为空", text -> true, null);
        if (normalized == null)
            return null;
        try {
            BigDecimal value = new BigDecimal(normalized);
            require(value.compareTo(BigDecimal.ZERO) >= 0, field + " 不能为负数");
            require(value.scale() <= maxScale, field + " 最多保留 " + maxScale + " 位小数");
            return value.stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw IllegalParamException.of(field + " 数值格式不合法");
        }
    }
}

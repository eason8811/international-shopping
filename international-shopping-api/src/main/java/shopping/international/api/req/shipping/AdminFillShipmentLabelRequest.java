package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.CURRENCY_PATTERN;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧回填物流面单请求对象 (AdminFillShipmentLabelRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminFillShipmentLabelRequest implements Verifiable {
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 承运商名称
     */
    @Nullable
    private String carrierName;
    /**
     * 服务编码
     */
    @Nullable
    private String serviceCode;
    /**
     * 承运商追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 三方物流平台 externalId
     */
    @Nullable
    private String extExternalId;
    /**
     * 面单地址
     */
    @Nullable
    private String labelUrl;
    /**
     * 重量 (公斤), 十进制字符串, 最多 3 位小数
     */
    @Nullable
    private String weightKg;
    /**
     * 长 (厘米), 十进制字符串, 最多 1 位小数
     */
    @Nullable
    private String lengthCm;
    /**
     * 宽 (厘米), 十进制字符串, 最多 1 位小数
     */
    @Nullable
    private String widthCm;
    /**
     * 高 (厘米), 十进制字符串, 最多 1 位小数
     */
    @Nullable
    private String heightCm;
    /**
     * 申报价值 (最小货币单位)
     */
    @Nullable
    private Long declaredValue;
    /**
     * 币种代码
     */
    @Nullable
    private String currency;

    /**
     * 对回填面单参数进行校验与规范化
     */
    @Override
    public void validate() {
        carrierCode = normalizeNotNullField(carrierCode, "carrierCode 不能为空", s -> s.length() <= 64, "carrierCode 长度不能超过 64 个字符");
        carrierName = normalizeNotNullField(carrierName, "carrierName 不能为空", s -> s.length() <= 128, "carrierName 长度不能超过 128 个字符");
        trackingNo = normalizeNotNullField(trackingNo, "trackingNo 不能为空", s -> s.length() <= 128, "trackingNo 长度不能超过 128 个字符");

        serviceCode = normalizeNullableField(serviceCode, "serviceCode 不能为空", s -> s.length() <= 64, "serviceCode 长度不能超过 64 个字符");
        extExternalId = normalizeNullableField(extExternalId, "extExternalId 不能为空", s -> s.length() <= 128, "extExternalId 长度不能超过 128 个字符");
        labelUrl = normalizeNullableField(labelUrl, "labelUrl 不能为空", s -> s.length() <= 500, "labelUrl 长度不能超过 500 个字符");

        weightKg = normalizeDecimalString(weightKg, 3, "weightKg");
        lengthCm = normalizeDecimalString(lengthCm, 1, "lengthCm");
        widthCm = normalizeDecimalString(widthCm, 1, "widthCm");
        heightCm = normalizeDecimalString(heightCm, 1, "heightCm");

        if (declaredValue != null)
            require(declaredValue >= 0L, "declaredValue 不能为负数");

        if (currency != null) {
            currency = currency.strip().toUpperCase(Locale.ROOT);
            require(CURRENCY_PATTERN.matcher(currency).matches(), "currency 需为 3 位字母代码");
        }
    }

    /**
     * 规范化十进制字符串并校验非负与小数位数
     *
     * @param raw      原始数值字符串
     * @param maxScale 最大小数位
     * @param field    字段名
     * @return 规范化后的字符串, 若传入为空则返回 {@code null}
     */
    private static @Nullable String normalizeDecimalString(@Nullable String raw, int maxScale, String field) {
        String normalized = normalizeNullableField(raw, field + " 不能为空", s -> true, null);
        if (normalized == null)
            return null;

        try {
            BigDecimal number = new BigDecimal(normalized);
            require(number.compareTo(BigDecimal.ZERO) >= 0, field + " 不能为负数");
            require(number.scale() <= maxScale, field + " 最多保留 " + maxScale + " 位小数");
            return number.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw IllegalParamException.of(field + " 数值格式不合法");
        }
    }
}

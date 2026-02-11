package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.CURRENCY_PATTERN;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 物流面单信息值对象
 *
 * <p>用于管理侧回填承运商、追踪号、体积重量等字段</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ShipmentLabel implements Verifiable {

    /**
     * 承运商编码
     */
    private String carrierCode;
    /**
     * 承运商名称
     */
    private String carrierName;
    /**
     * 服务编码
     */
    @Nullable
    private String serviceCode;
    /**
     * 追踪号
     */
    private String trackingNo;
    /**
     * 三方物流外部单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 面单地址
     */
    @Nullable
    private String labelUrl;
    /**
     * 包裹尺寸与重量
     */
    @Nullable
    private final ShipmentDimension dimension;
    /**
     * 申报价值 (最小货币单位)
     */
    @Nullable
    private final Long declaredValue;
    /**
     * 申报币种
     */
    @Nullable
    private String currency;

    /**
     * 构造物流面单信息
     *
     * @param carrierCode 承运商编码
     * @param carrierName 承运商名称
     * @param serviceCode 服务编码
     * @param trackingNo 追踪号
     * @param extExternalId 三方物流外部单号
     * @param labelUrl 面单地址
     * @param dimension 包裹尺寸
     * @param declaredValue 申报价值
     * @param currency 申报币种
     */
    private ShipmentLabel(String carrierCode,
                          String carrierName,
                          @Nullable String serviceCode,
                          String trackingNo,
                          @Nullable String extExternalId,
                          @Nullable String labelUrl,
                          @Nullable ShipmentDimension dimension,
                          @Nullable Long declaredValue,
                          @Nullable String currency) {
        this.carrierCode = carrierCode;
        this.carrierName = carrierName;
        this.serviceCode = serviceCode;
        this.trackingNo = trackingNo;
        this.extExternalId = extExternalId;
        this.labelUrl = labelUrl;
        this.dimension = dimension;
        this.declaredValue = declaredValue;
        this.currency = currency;
    }

    /**
     * 创建物流面单信息值对象
     *
     * @param carrierCode 承运商编码, 必填, 最大 64 个字符
     * @param carrierName 承运商名称, 必填, 最大 128 个字符
     * @param serviceCode 服务编码, 可选, 最大 64 个字符
     * @param trackingNo 追踪号, 必填, 最大 128 个字符
     * @param extExternalId 三方物流外部单号, 可选, 最大 128 个字符
     * @param labelUrl 面单地址, 可选, 最大 500 个字符
     * @param dimension 包裹尺寸, 可选
     * @param declaredValue 申报价值, 可选, 不能为负数
     * @param currency 申报币种, 可选, 需为 3 位字母代码
     * @return 物流面单信息值对象
     */
    public static ShipmentLabel of(String carrierCode,
                                   String carrierName,
                                   @Nullable String serviceCode,
                                   String trackingNo,
                                   @Nullable String extExternalId,
                                   @Nullable String labelUrl,
                                   @Nullable ShipmentDimension dimension,
                                   @Nullable Long declaredValue,
                                   @Nullable String currency) {
        ShipmentLabel label = new ShipmentLabel(carrierCode, carrierName, serviceCode, trackingNo,
                extExternalId, labelUrl, dimension, declaredValue, currency);
        label.validate();
        return label;
    }

    /**
     * 校验并规范化面单信息
     */
    @Override
    public void validate() {
        carrierCode = normalizeNotNullField(carrierCode, "carrierCode 不能为空",
                value -> value.length() <= 64,
                "carrierCode 长度不能超过 64 个字符");
        carrierName = normalizeNotNullField(carrierName, "carrierName 不能为空",
                value -> value.length() <= 128,
                "carrierName 长度不能超过 128 个字符");
        serviceCode = normalizeNullableField(serviceCode, "serviceCode 不能为空",
                value -> value.length() <= 64,
                "serviceCode 长度不能超过 64 个字符");
        trackingNo = normalizeNotNullField(trackingNo, "trackingNo 不能为空",
                value -> value.length() <= 128,
                "trackingNo 长度不能超过 128 个字符");
        extExternalId = normalizeNullableField(extExternalId, "extExternalId 不能为空",
                value -> value.length() <= 128,
                "extExternalId 长度不能超过 128 个字符");
        labelUrl = normalizeNullableField(labelUrl, "labelUrl 不能为空",
                value -> value.length() <= 500,
                "labelUrl 长度不能超过 500 个字符");

        if (dimension != null)
            dimension.validate();

        if (declaredValue != null)
            require(declaredValue >= 0L, "declaredValue 不能为负数");

        if (currency != null) {
            currency = currency.strip().toUpperCase(Locale.ROOT);
            require(CURRENCY_PATTERN.matcher(currency).matches(), "currency 需为 3 位字母代码");
        }
    }
}

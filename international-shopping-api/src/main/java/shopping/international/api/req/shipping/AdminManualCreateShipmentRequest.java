package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * 管理侧手动创建物流单请求对象 (AdminManualCreateShipmentRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminManualCreateShipmentRequest implements Verifiable {
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
    /**
     * 业务幂等键
     */
    @Nullable
    private String idempotencyKey;
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
     * 运单号
     */
    @Nullable
    private String trackingNo;
    /**
     * 外部物流平台单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 备注
     */
    @Nullable
    private String note;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;

    /**
     * 对手动创建物流单参数进行校验与规范化
     */
    @Override
    public void validate() {
        orderNo = normalizeNotNullField(orderNo, "orderNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "orderNo 长度需在 10~32 个字符之间");

        idempotencyKey = normalizeNullableField(idempotencyKey, "idempotencyKey 不能为空", s -> s.length() <= 64, "idempotencyKey 长度不能超过 64 个字符");
        carrierCode = normalizeNullableField(carrierCode, "carrierCode 不能为空", s -> s.length() <= 64, "carrierCode 长度不能超过 64 个字符");
        carrierName = normalizeNullableField(carrierName, "carrierName 不能为空", s -> s.length() <= 128, "carrierName 长度不能超过 128 个字符");
        serviceCode = normalizeNullableField(serviceCode, "serviceCode 不能为空", s -> s.length() <= 64, "serviceCode 长度不能超过 64 个字符");
        trackingNo = normalizeNullableField(trackingNo, "trackingNo 不能为空", s -> s.length() <= 128, "trackingNo 长度不能超过 128 个字符");
        extExternalId = normalizeNullableField(extExternalId, "extExternalId 不能为空", s -> s.length() <= 128, "extExternalId 长度不能超过 128 个字符");
        note = normalizeNullableField(note, "note 不能为空", s -> s.length() <= 255, "note 长度不能超过 255 个字符");
        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空", s -> s.length() <= 128, "sourceRef 长度不能超过 128 个字符");
    }
}

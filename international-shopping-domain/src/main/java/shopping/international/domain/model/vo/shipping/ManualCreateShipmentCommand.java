package shopping.international.domain.model.vo.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧手工补建物流单命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualCreateShipmentCommand implements Verifiable {
    /**
     * 物流单寄出地址 ID (user_address.id)
     */
    @NotNull
    private Integer shipFromAddressId;
    /**
     * 订单号
     */
    @NotNull
    private OrderNo orderNo;
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
    @NotNull
    private Long declaredValue;
    /**
     * 币种代码
     */
    @NotNull
    private String currency;
    /**
     * 备注
     */
    @Nullable
    private String note;

    /**
     * 校验命令
     */
    @Override
    public void validate() {
        requireNotNull(orderNo, "orderNo 不能为空");
    }
}

package shopping.international.api.resp.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * 物流地址快照响应对象 (AddressSnapshotRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshotRespond {
    /**
     * 收件人姓名
     */
    @Nullable
    private String receiverName;
    /**
     * 联系电话
     */
    @Nullable
    private String phone;
    /**
     * 国家/地区
     */
    @Nullable
    private String country;
    /**
     * 省/州
     */
    @Nullable
    private String province;
    /**
     * 城市
     */
    @Nullable
    private String city;
    /**
     * 区/县
     */
    @Nullable
    private String district;
    /**
     * 地址行 1
     */
    @Nullable
    private String addressLine1;
    /**
     * 地址行 2
     */
    @Nullable
    private String addressLine2;
    /**
     * 邮编
     */
    @Nullable
    private String zipcode;
}

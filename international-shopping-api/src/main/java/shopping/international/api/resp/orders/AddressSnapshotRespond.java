package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货信息快照响应 (AddressSnapshotRespond)
 *
 * <p>用于在订单中展示下单时保存的收货信息快照, 建议与用户地址结构保持一致</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshotRespond {
    /**
     * 收货人姓名
     */
    private String receiverName;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 国家/地区 (可为空)
     */
    private String country;
    /**
     * 省/州 (可为空)
     */
    private String province;
    /**
     * 城市 (可为空)
     */
    private String city;
    /**
     * 区/县 (可为空)
     */
    private String district;
    /**
     * 地址行 1
     */
    private String addressLine1;
    /**
     * 地址行 2 (可为空)
     */
    private String addressLine2;
    /**
     * 邮编 (可为空)
     */
    private String zipcode;
}


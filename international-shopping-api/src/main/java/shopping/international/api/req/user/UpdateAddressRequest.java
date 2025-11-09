package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 修改收货地址请求
 */
@Data
public class UpdateAddressRequest {
    /**
     * 收货人 (可空则不改)
     */
    @Nullable
    private String receiverName;
    /**
     * 手机号 (可空则不改)
     */
    @Nullable
    private String phone;
    /**
     * 国家/省/市/区县 (可空)
     */
    @Nullable
    private String country;
    @Nullable
    private String province;
    @Nullable
    private String city;
    @Nullable
    private String district;
    /**
     * 地址行1/2、邮编 (可空)
     */
    @Nullable
    private String addressLine1;
    @Nullable
    private String addressLine2;
    @Nullable
    private String zipcode;
    /**
     * 是否设为默认 (可空)
     */
    @Nullable
    private Boolean isDefault;

    /**
     * 入参校验 (若提供值需合法)
     */
    public void validate() {
        if (receiverName != null) {
            requireNotBlank(receiverName, "收货人不能为空");
            receiverName = receiverName.strip();
        }
        if (phone != null) {
            requireNotBlank(phone, "手机号不能为空");
            phone = phone.strip();
        }
        if (country != null) {
            requireNotBlank(country, "国家不能为空");
            country = country.strip();
        }
        if (province != null) {
            requireNotBlank(province, "省份不能为空");
            province = province.strip();
        }
        if (city != null) {
            requireNotBlank(city, "城市不能为空");
            city = city.strip();
        }
        if (district != null) {
            requireNotBlank(district, "区不能为空");
            district = district.strip();
        }
        if (addressLine1 != null) {
            requireNotBlank(addressLine1, "地址行1不能为空");
            addressLine1 = addressLine1.strip();
        }
        if (zipcode != null) {
            requireNotBlank(zipcode, "邮编不能为空");
            zipcode = zipcode.strip();
        }
    }
}

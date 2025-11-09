package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 新增收货地址请求
 */
@Data
public class CreateAddressRequest {
    /**
     * 收货人
     */
    private String receiverName;
    /**
     * 手机号 (E.164 简化)
     */
    private String phone;
    /**
     * 国家/省/市/区县
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
     * 地址行1/2, 邮编
     */
    private String addressLine1;
    @Nullable
    private String addressLine2;
    @Nullable
    private String zipcode;
    /**
     * 是否默认
     */
    @Nullable
    private Boolean isDefault;

    /**
     * 入参校验
     */
    public void validate() {
        requireNotBlank(receiverName, "收货人不能为空");
        requireNotBlank(phone, "联系电话不能为空");
        requireNotBlank(addressLine1, "地址行1不能为空");

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
            requireNotBlank(district, "区县不能为空");
            district = district.strip();
        }
        if (addressLine2 != null) {
            requireNotBlank(addressLine2, "地址行2不能为空");
            addressLine2 = addressLine2.strip();
        }
        if (zipcode != null) {
            requireNotBlank(zipcode, "邮编不能为空");
            zipcode = zipcode.strip();
        }
    }
}

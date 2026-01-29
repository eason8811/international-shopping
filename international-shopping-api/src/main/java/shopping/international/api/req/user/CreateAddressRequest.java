package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static shopping.international.types.utils.FieldValidateUtils.*;

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
     * 联系电话国家码 (E.164, 不含 '+')
     */
    private String phoneCountryCode;
    /**
     * 联系电话 national number (E.164, 国家码之后的 National Significant Number, 仅数字)
     */
    private String phoneNationalNumber;
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
        receiverName = receiverName.strip();
        requireNotBlank(phoneCountryCode, "联系电话国家码不能为空");
        requireNotBlank(phoneNationalNumber, "联系电话不能为空");
        phoneCountryCode = phoneCountryCode.strip();
        phoneNationalNumber = phoneNationalNumber.strip();
        requireNotBlank(phoneCountryCode, "联系电话国家码不能为空");
        requireNotBlank(phoneNationalNumber, "联系电话不能为空");
        require(phoneCountryCode.matches("^[1-9][0-9]{0,2}$"), "country_code 格式不正确");
        require(phoneNationalNumber.matches("^[0-9]{1,14}$"), "national_number 格式不正确");
        require((phoneCountryCode.length() + phoneNationalNumber.length()) <= 15, "手机号格式不正确");
        requireNotBlank(addressLine1, "地址行1不能为空");
        addressLine1 = addressLine1.strip();
        requireNotBlank(country, "国家不能为空");
        country = country.strip();
        requireNotBlank(province, "省份不能为空");
        province = province.strip();
        requireNotBlank(city, "城市不能为空");
        city = city.strip();
        requireNotBlank(district, "区县不能为空");
        district = district.strip();
        if (addressLine2 != null) {
            requireNotBlank(addressLine2, "地址行2不能为空");
            addressLine2 = addressLine2.strip();
        }
        requireNotBlank(zipcode, "邮编不能为空");
        zipcode = zipcode.strip();
    }
}

package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.AddressSource;

import java.util.Locale;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.require;
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
    private String countryCode;
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
     * 地址语言
     */
    @Nullable
    private String languageCode;
    /**
     * 地址来源
     */
    private String addressSource;
    /**
     * Google 扩展输入
     */
    @Nullable
    private String rawInput;
    @Nullable
    private String googlePlaceId;
    @Nullable
    private Map<String, Object> placeResponse;
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
        requireNotBlank(countryCode, "国家编码不能为空");
        countryCode = countryCode.strip().toUpperCase(Locale.ROOT);
        require(countryCode.matches("^[A-Z]{2}$"), "countryCode 格式不正确");
        requireNotBlank(addressLine1, "地址行1不能为空");
        addressLine1 = addressLine1.strip();
        requireNotBlank(country, "国家不能为空");
        country = country.strip();
        province = normalizeNullable(province);
        city = normalizeNullable(city);
        district = normalizeNullable(district);
        addressLine2 = normalizeNullable(addressLine2);
        zipcode = normalizeNullable(zipcode);
        languageCode = normalizeNullable(languageCode);
        rawInput = normalizeNullable(rawInput);
        googlePlaceId = normalizeNullable(googlePlaceId);
        addressSource = AddressSource.parse(addressSource).name();
    }

    private static String normalizeNullable(String value) {
        if (value == null)
            return null;
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}

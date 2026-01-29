package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 收货信息快照值对象 (对应 orders.address_snapshot)
 *
 * <p>该值对象属于订单领域, 不依赖用户领域实体。</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class AddressSnapshot {
    /**
     * 收货人
     */
    private final String receiverName;
    /**
     * 联系电话国家代码
     */
    private final String phoneCountryCode;
    /**
     * 联系电话本地号码
     */
    private final String phoneNationalNumber;
    /**
     * 国家/省/市/区县/地址行1/行2/邮编
     */
    private final String country;
    private final String province;
    private final String city;
    private final String district;
    private final String addressLine1;
    private final String addressLine2;
    private final String zipcode;

    /**
     * 创建一个地址快照对象, 用于存储订单中的收货信息
     *
     * @param receiverName   收货人姓名 必填
     * @param phoneCountryCode    联系电话国家代码
     * @param phoneNationalNumber 联系电话本地号码
     * @param country        国家 可选
     * @param province       省份 可选
     * @param city           城市 可选
     * @param district       区县 可选
     * @param addressLine1   地址行1 必填
     * @param addressLine2   地址行2 可选
     * @param zipcode        邮政编码 可选
     */
    private AddressSnapshot(String receiverName, String phoneCountryCode, String phoneNationalNumber,
                            String country, String province, String city, String district,
                            String addressLine1, String addressLine2, String zipcode) {
        this.receiverName = receiverName;
        this.phoneCountryCode = phoneCountryCode;
        this.phoneNationalNumber = phoneNationalNumber;
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.zipcode = zipcode;
    }

    /**
     * 创建一个地址快照对象, 用于存储订单中的收货信息。该方法会自动去除输入字符串两端的空白字符。
     *
     * @param receiverName 收货人姓名, 必填
     * @param phoneCountryCode    联系电话国家代码
     * @param phoneNationalNumber 联系电话本地号码
     * @param country      国家, 可选
     * @param province     省份, 可选
     * @param city         城市, 可选
     * @param district     区县, 可选
     * @param addressLine1 地址行1, 必填
     * @param addressLine2 地址行2, 可选
     * @param zipcode      邮政编码, 可选
     * @return 返回一个新的 {@link AddressSnapshot} 对象
     * @throws IllegalParamException 如果 <code>receiverName</code>, <code>phone</code>, 或 <code>addressLine1</code> 为 <code>null</code> 或仅包含空白字符
     */
    public static AddressSnapshot of(String receiverName, String phoneCountryCode, String phoneNationalNumber,
                                     String country, String province, String city, String district,
                                     String addressLine1, String addressLine2, String zipcode) {
        requireNotBlank(receiverName, "收货人不能为空");
        requireNotBlank(phoneCountryCode, "联系电话国家代码不能为空");
        requireNotBlank(phoneNationalNumber, "联系电话本地号码不能为空");
        require(phoneCountryCode.matches("^[1-9][0-9]{0,2}$"), "country_code 格式不正确");
        require(phoneNationalNumber.matches("^[0-9]{1,14}$"), "national_number 格式不正确");
        requireNotBlank(addressLine1, "地址行1不能为空");
        return new AddressSnapshot(
                receiverName.strip(),
                phoneCountryCode.strip(),
                phoneNationalNumber.strip(),
                country == null ? null : country.strip(),
                province == null ? null : province.strip(),
                city == null ? null : city.strip(),
                district == null ? null : district.strip(),
                addressLine1.strip(),
                addressLine2 == null ? null : addressLine2.strip(),
                zipcode == null ? null : zipcode.strip());
    }
}


package shopping.international.domain.model.entity.user;

import lombok.*;
import lombok.experimental.Accessors;
import shopping.international.domain.model.enums.user.AddressSource;
import shopping.international.domain.model.enums.user.AddressValidationStatus;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.model.vo.user.UserAddressExtension;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户收货地址实体 (对应表 user_address), 归属 User 聚合
 * <p>负责维护自身可修改字段与基本校验；默认地址唯一性的约束由聚合根 User 负责</p>
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserAddress {
    /**
     * 主键ID (可为 null, 表示尚未持久化)
     */
    private Long id;
    /**
     * 收货人
     */
    private String receiverName;
    /**
     * 联系电话 (值对象)
     */
    private PhoneNumber phone;
    /**
     * 国家/省/市/区县/地址行1/行2/邮编
     */
    private String countryCode;
    private String country;
    private String province;
    private String city;
    private String district;
    private String addressLine1;
    private String addressLine2;
    private String zipcode;
    /**
     * 地址语言与来源
     */
    private String languageCode;
    private AddressSource addressSource;
    private AddressValidationStatus validationStatus;
    private LocalDateTime validatedAt;
    /**
     * 是否默认地址 (聚合根保证唯一)
     */
    private boolean defaultAddress;
    /**
     * 创建与更新时间 (快照)
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * Google 地址扩展快照
     */
    private UserAddressExtension extension;

    /**
     * 快速构建一个地址 (新增)
     *
     * @param receiverName 收货人姓名 不能为空
     * @param phone        联系电话 值对象, 不可为空
     * @param countryCode  国家编码
     * @param country      国家
     * @param province     省份
     * @param city         城市
     * @param district     区县
     * @param addressLine1 地址行1 不能为空
     * @param addressLine2 地址行2 可选
     * @param zipcode      邮编 可选
     * @param languageCode 地址语言 可选
     * @param addressSource 地址来源
     * @param isDefault    是否设为默认地址
     * @param validationStatus 校验状态
     * @param validatedAt  最近校验时间
     * @param extension    地址扩展快照
     * @return 新建的 {@link UserAddress} 对象, 其 id 为 null 表示尚未持久化到数据库
     * @throws IllegalParamException 如果收货人姓名或地址行1为空白时抛出
     */
    public static UserAddress of(String receiverName, PhoneNumber phone,
                                 String countryCode, String country, String province, String city, String district,
                                 String addressLine1, String addressLine2, String zipcode,
                                 String languageCode, AddressSource addressSource, boolean isDefault,
                                 AddressValidationStatus validationStatus, LocalDateTime validatedAt,
                                 UserAddressExtension extension) {
        validateRequiredFields(receiverName, phone, countryCode, country, addressLine1, addressSource);
        return UserAddress.builder()
                .receiverName(receiverName.strip())
                .phone(phone)
                .countryCode(countryCode.strip().toUpperCase(Locale.ROOT))
                .country(country.strip())
                .province(normalizeNullable(province))
                .city(normalizeNullable(city))
                .district(normalizeNullable(district))
                .addressLine1(addressLine1.strip())
                .addressLine2(normalizeNullable(addressLine2))
                .zipcode(normalizeNullable(zipcode))
                .languageCode(normalizeNullable(languageCode))
                .addressSource(addressSource)
                .validationStatus(validationStatus == null ? AddressValidationStatus.UNVALIDATED : validationStatus)
                .validatedAt(validatedAt)
                .defaultAddress(isDefault)
                .extension(extension)
                .build();
    }

    /**
     * 用新的地址快照覆盖当前地址的非标识字段
     *
     * @param source 新地址快照
     */
    public void replaceWith(UserAddress source) {
        requireNotNull(source, "地址信息不能为空");
        validateRequiredFields(source.receiverName, source.phone, source.countryCode, source.country,
                source.addressLine1, source.addressSource);

        this.receiverName = source.receiverName.strip();
        this.phone = source.phone;
        this.countryCode = source.countryCode.strip().toUpperCase(Locale.ROOT);
        this.country = source.country.strip();
        this.province = normalizeNullable(source.province);
        this.city = normalizeNullable(source.city);
        this.district = normalizeNullable(source.district);
        this.addressLine1 = source.addressLine1.strip();
        this.addressLine2 = normalizeNullable(source.addressLine2);
        this.zipcode = normalizeNullable(source.zipcode);
        this.languageCode = normalizeNullable(source.languageCode);
        this.addressSource = source.addressSource;
        this.validationStatus = source.validationStatus == null ? AddressValidationStatus.UNVALIDATED : source.validationStatus;
        this.validatedAt = source.validatedAt;
        this.extension = source.extension;
    }

    /**
     * 设置当前地址是否为默认地址。
     *
     * @param isDefault 如果设置为 <code>true</code>, 则将此地址设为用户的默认收货地址; 否则, 不作为默认地址使用
     */
    public void setDefault(boolean isDefault) {
        this.defaultAddress = isDefault;
    }

    /**
     * 为用户地址分配一个 ID, 如果该用户地址已存在一个非空的 ID, 则不允许重新赋值
     *
     * @param id 要分配给用户地址的新 ID, 类型为 Long
     * @throws IllegalStateException 如果尝试给已经拥有非空 ID 的 UserAddress 对象重新赋值 ID
     */
    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id))
            throw new IllegalStateException("UserAddress 实体已存在 ID, 不允许重新赋值, current: " + this.id + ", new: " + id);
        this.id = id;
    }

    private static void validateRequiredFields(String receiverName, PhoneNumber phone,
                                               String countryCode, String country,
                                               String addressLine1, AddressSource addressSource) {
        requireNotBlank(receiverName, "收货人不能为空");
        requireNotNull(phone, "联系电话不能为空");
        requireNotBlank(countryCode, "国家编码不能为空");
        require(countryCode.strip().toUpperCase(Locale.ROOT).matches("^[A-Z]{2}$"), "国家编码格式不正确");
        requireNotBlank(country, "国家不能为空");
        requireNotBlank(addressLine1, "地址行1 不能为空");
        requireNotNull(addressSource, "地址来源不能为空");
    }

    private static String normalizeNullable(String value) {
        if (value == null)
            return null;
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}

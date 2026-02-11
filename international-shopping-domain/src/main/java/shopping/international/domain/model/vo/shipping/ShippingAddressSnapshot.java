package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * 物流地址快照值对象
 *
 * <p>用于承载 {@code shipment.ship_from} 与 {@code shipment.ship_to} 的 JSON 快照数据</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ShippingAddressSnapshot implements Verifiable {

    /**
     * 收件人姓名
     */
    private String receiverName;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 国家或地区
     */
    @Nullable
    private String country;
    /**
     * 省或州
     */
    @Nullable
    private String province;
    /**
     * 城市
     */
    @Nullable
    private String city;
    /**
     * 区或县
     */
    @Nullable
    private String district;
    /**
     * 地址行 1
     */
    private String addressLine1;
    /**
     * 地址行 2
     */
    @Nullable
    private String addressLine2;
    /**
     * 邮政编码
     */
    @Nullable
    private String zipcode;

    /**
     * 构造物流地址快照
     *
     * @param receiverName 收件人姓名
     * @param phone 联系电话
     * @param country 国家或地区
     * @param province 省或州
     * @param city 城市
     * @param district 区或县
     * @param addressLine1 地址行 1
     * @param addressLine2 地址行 2
     * @param zipcode 邮编
     */
    private ShippingAddressSnapshot(String receiverName,
                                    String phone,
                                    @Nullable String country,
                                    @Nullable String province,
                                    @Nullable String city,
                                    @Nullable String district,
                                    String addressLine1,
                                    @Nullable String addressLine2,
                                    @Nullable String zipcode) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.zipcode = zipcode;
    }

    /**
     * 创建物流地址快照
     *
     * @param receiverName 收件人姓名, 必填, 最大 64 个字符
     * @param phone 联系电话, 必填, 最大 32 个字符
     * @param country 国家或地区, 可选, 最大 64 个字符
     * @param province 省或州, 可选, 最大 64 个字符
     * @param city 城市, 可选, 最大 64 个字符
     * @param district 区或县, 可选, 最大 64 个字符
     * @param addressLine1 地址行 1, 必填, 最大 255 个字符
     * @param addressLine2 地址行 2, 可选, 最大 255 个字符
     * @param zipcode 邮政编码, 可选, 最大 20 个字符
     * @return 物流地址快照值对象
     */
    public static ShippingAddressSnapshot of(String receiverName,
                                             String phone,
                                             @Nullable String country,
                                             @Nullable String province,
                                             @Nullable String city,
                                             @Nullable String district,
                                             String addressLine1,
                                             @Nullable String addressLine2,
                                             @Nullable String zipcode) {
        ShippingAddressSnapshot snapshot = new ShippingAddressSnapshot(
                receiverName,
                phone,
                country,
                province,
                city,
                district,
                addressLine1,
                addressLine2,
                zipcode
        );
        snapshot.validate();
        return snapshot;
    }

    /**
     * 校验并规范化当前地址快照
     */
    @Override
    public void validate() {
        receiverName = normalizeNotNullField(receiverName, "receiverName 不能为空",
                value -> value.length() <= 64,
                "receiverName 长度不能超过 64 个字符");
        phone = normalizeNotNullField(phone, "phone 不能为空",
                value -> value.length() <= 32,
                "phone 长度不能超过 32 个字符");
        country = normalizeNullableField(country, "country 不能为空",
                value -> value.length() <= 64,
                "country 长度不能超过 64 个字符");
        province = normalizeNullableField(province, "province 不能为空",
                value -> value.length() <= 64,
                "province 长度不能超过 64 个字符");
        city = normalizeNullableField(city, "city 不能为空",
                value -> value.length() <= 64,
                "city 长度不能超过 64 个字符");
        district = normalizeNullableField(district, "district 不能为空",
                value -> value.length() <= 64,
                "district 长度不能超过 64 个字符");
        addressLine1 = normalizeNotNullField(addressLine1, "addressLine1 不能为空",
                value -> value.length() <= 255,
                "addressLine1 长度不能超过 255 个字符");
        addressLine2 = normalizeNullableField(addressLine2, "addressLine2 不能为空",
                value -> value.length() <= 255,
                "addressLine2 长度不能超过 255 个字符");
        zipcode = normalizeNullableField(zipcode, "zipcode 不能为空",
                value -> value.length() <= 20,
                "zipcode 长度不能超过 20 个字符");
    }
}

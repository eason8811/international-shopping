package shopping.international.domain.model.entity.user;

import lombok.*;
import lombok.experimental.Accessors;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.domain.model.vo.user.PhoneNumber;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 用户收货地址实体 (对应表 user_address), 归属 User 聚合
 * <p>负责维护自身可修改字段与基本校验；默认地址唯一性的约束由聚合根 User 负责</p>
 */
@Getter
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
    private String country;
    private String province;
    private String city;
    private String district;
    private String addressLine1;
    private String addressLine2;
    private String zipcode;
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
     * 快速构建一个地址 (新增)
     *
     * @param receiverName 收货人姓名 不能为空
     * @param phone        联系电话 值对象, 不可为空
     * @param country      国家
     * @param province     省份
     * @param city         城市
     * @param district     区县
     * @param addressLine1 地址行1 不能为空
     * @param addressLine2 地址行2 可选
     * @param zipcode      邮编 可选
     * @param isDefault    是否设为默认地址
     * @return 新建的 {@link UserAddress} 对象, 其 id 为 null 表示尚未持久化到数据库
     * @throws IllegalParamException 如果收货人姓名或地址行1为空白时抛出
     */
    public static UserAddress of(String receiverName, PhoneNumber phone,
                                 String country, String province, String city, String district,
                                 String addressLine1, String addressLine2, String zipcode,
                                 boolean isDefault) {
        requireNotBlank(receiverName, "收货人不能为空");
        requireNotBlank(addressLine1, "地址行1 不能为空");
        return new UserAddress(null, receiverName, phone, country, province, city, district,
                addressLine1, addressLine2, zipcode, isDefault, null, null);
    }

    /**
     * 更新用户地址信息。此方法允许更新收货人姓名、联系电话及详细的地址信息等字段。
     * 注意, 如果提供的 <code>receiverName</code> 或 <code>addressLine1</code> 为空白, 则会抛出异常
     *
     * @param receiverName 收货人姓名 不得为空白
     * @param phone        联系电话 值对象, 需要符合 E.164 标准格式
     * @param country      国家
     * @param province     省份
     * @param city         城市
     * @param district     区县
     * @param addressLine1 地址行1 不得为空白
     * @param addressLine2 地址行2 可选
     * @param zipcode      邮编 可选
     * @throws IllegalParamException 当 <code>receiverName</code> 或 <code>addressLine1</code> 为空白时抛出
     */
    public void update(String receiverName, PhoneNumber phone,
                       String country, String province, String city, String district,
                       String addressLine1, String addressLine2, String zipcode) {
        if (receiverName != null && receiverName.isBlank())
            throw new IllegalParamException("收货人不能为空");
        if (addressLine1 != null && addressLine1.isBlank())
            throw new IllegalParamException("地址行1 不能为空");

        if (receiverName != null) this.receiverName = receiverName;
        if (phone != null) this.phone = phone;
        if (country != null) this.country = country;
        if (province != null) this.province = province;
        if (city != null) this.city = city;
        if (district != null) this.district = district;
        if (addressLine1 != null) this.addressLine1 = addressLine1;
        if (addressLine2 != null) this.addressLine2 = addressLine2;
        if (zipcode != null) this.zipcode = zipcode;
    }

    /**
     * 设置当前地址是否为默认地址。
     *
     * @param isDefault 如果设置为 <code>true</code>, 则将此地址设为用户的默认收货地址; 否则, 不作为默认地址使用
     */
    public void setDefault(boolean isDefault) {
        this.defaultAddress = isDefault;
    }
}

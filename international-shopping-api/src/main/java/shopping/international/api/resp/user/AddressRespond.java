package shopping.international.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.user.UserAddress;

import java.time.LocalDateTime;

/**
 * 收货地址响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRespond {
    /**
     * 地址ID
     */
    private Long id;
    /**
     * 收货人
     */
    private String receiverName;
    /**
     * 电话
     */
    private String phone;
    /**
     * 国家/省/市/区县
     */
    private String country;
    private String province;
    private String city;
    private String district;
    /**
     * 地址行1/2、邮编
     */
    private String addressLine1;
    private String addressLine2;
    private String zipcode;
    /**
     * 是否默认
     */
    private Boolean isDefault;
    /**
     * 创建/更新时间
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 {@link UserAddress} 实体构建 {@link AddressRespond} 对象
     *
     * @param address 用户收货地址实体, 包含了用户地址的所有信息
     * @return 新建的 {@link AddressRespond} 对象, 包括了用户地址的 ID, 收货人姓名, 联系电话, 地址详细信息, 是否为默认地址等
     */
    public static AddressRespond from(UserAddress address) {
        return new AddressRespond(
                address.getId(),
                address.getReceiverName(),
                address.getPhone() == null ? null : address.getPhone().getValue(),
                address.getCountry(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getZipcode(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}

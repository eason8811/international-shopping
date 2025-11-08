package shopping.international.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.user.Gender;
import shopping.international.domain.model.vo.user.UserProfile;

import java.time.LocalDate;

/**
 * 用户资料响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRespond {
    /**
     * 显示名称
     */
    private String displayName;
    /**
     * 头像URL
     */
    private String avatarUrl;
    /**
     * 性别
     */
    private Gender gender;
    /**
     * 生日
     */
    private LocalDate birthday;
    /**
     * 国家/省/市/详细地址/邮编
     */
    private String country;
    private String province;
    private String city;
    private String addressLine;
    private String zipcode;

    /**
     * 从 {@link UserProfile} 对象转换为 {@link UserProfileRespond} 对象
     *
     * @param profile 需要转换的 {@link UserProfile} 实例
     * @return 一个新的 {@link UserProfileRespond} 实例, 包含了从给定的 {@code UserProfile} 中提取的信息
     */
    public static UserProfileRespond from(UserProfile profile) {
        return new UserProfileRespond(
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getGender(),
                profile.getBirthday(),
                profile.getCountry(),
                profile.getProvince(),
                profile.getCity(),
                profile.getAddressLine(),
                profile.getZipcode()
        );
    }
}

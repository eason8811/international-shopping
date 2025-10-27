package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import shopping.international.domain.model.enums.user.Gender;

/**
 * 用户资料值对象 (1:1)
 * <p>不可变；对外通过 withXxx 方法返回新实例</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class UserProfile {
    /**
     * 用户在系统中的显示名称, 用于界面展示
     * <p>该字段为不可变属性, 一旦设置后无法直接修改, 如果需要更新显示名, 应该通过 {@link UserProfile#withDisplayName(String)} 方法创建一个新的 {@link UserProfile} 实例</p>
     * <p>显示名称的长度必须 <b>小于或等于 64 个字符</b> </p>
     */
    private final String displayName;
    /**
     * 用户头像的 URL 地址
     */
    private final String avatarUrl;
    /**
     * 用户性别
     *
     * @see Gender
     */
    private final Gender gender;
    /**
     * 用户的生日日期
     */
    private final LocalDate birthday;
    /**
     * 用户的国家
     */
    private final String country;
    /**
     * 用户所在的省份
     */
    private final String province;
    /**
     * 用户所在的城市
     */
    private final String city;
    /**
     * 用户的详细地址
     */
    private final String addressLine;
    /**
     * 用户的邮编
     */
    private final String zipcode;
    /**
     * 用户的额外信息
     */
    private final Map<String, Object> extra;

    /**
     * 构造用户资料值对象
     * <p>此构造函数用于创建一个包含详细信息的 {@link UserProfile} 实例, 该实例是不可变的, 任何属性的修改都需要通过相应的 withXxx 方法返回一个新的实例</p>
     *
     * @param displayName 用户在系统中的显示名称, 用于界面展示
     * @param avatarUrl   用户头像的 URL 地址
     * @param gender      用户性别, 如果为 null 则默认设置为 {@link Gender#UNKNOWN}
     * @param birthday    用户的生日日期
     * @param country     用户所在的国家
     * @param province    用户所在的省份
     * @param city        用户所在的城市
     * @param addressLine 用户的详细地址
     * @param zipcode     用户的邮编
     * @param extra       用户的额外信息, 如果为 null 则使用空映射代替
     */
    private UserProfile(String displayName, String avatarUrl, Gender gender, LocalDate birthday,
                        String country, String province, String city, String addressLine,
                        String zipcode, Map<String, Object> extra) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.gender = gender == null ? Gender.UNKNOWN : gender;
        this.birthday = birthday;
        this.country = country;
        this.province = province;
        this.city = city;
        this.addressLine = addressLine;
        this.zipcode = zipcode;
        this.extra = extra == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(extra));
    }
    
    /**
     * 返回一个所有字段均为空或默认值的 {@link UserProfile} 实例
     *
     * @return 一个空的 {@link UserProfile} 对象, 其中所有字段要么为 null, 要么为其默认值(如 {@link Gender#UNKNOWN}), extra 字段则为一个空映射
     */
    public static UserProfile empty() {
        return new UserProfile(null, null, Gender.UNKNOWN, null, null, null, null, null, null, Map.of());
    }

    /**
     * 创建一个包含用户详细信息的 {@link UserProfile} 实例
     *
     * @param displayName 用户在系统中的显示名称, 用于界面展示
     * @param avatarUrl   用户头像的 URL 地址
     * @param gender      用户性别, 如果为 null 则默认设置为 {@link Gender#UNKNOWN}
     * @param birthday    用户的生日日期
     * @param country     用户所在的国家
     * @param province    用户所在的省份
     * @param city        用户所在的城市
     * @param addressLine 用户的详细地址
     * @param zipcode     用户的邮编
     * @param extra       用户的额外信息, 如果为 null 则使用空映射代替
     * @return 一个新的 {@link UserProfile} 对象
     */
    public static UserProfile of(String displayName, String avatarUrl, Gender gender, LocalDate birthday,
                                 String country, String province, String city, String addressLine,
                                 String zipcode, Map<String, Object> extra) {
        return new UserProfile(displayName, avatarUrl, gender, birthday, country, province, city, addressLine, zipcode, extra);
    }
    
    /**
     * 创建一个新的 {@link UserProfile} 实例, 其中显示名称被更新为指定值
     * <p>此方法用于更改用户的显示名称, 如果提供的名称长度超过 64 个字符, 则抛出异常</p>
     *
     * @param name 新的显示名称 需要满足长度限制, 最大为 64 个字符
     * @return 一个新的 {@link UserProfile} 对象, 包含更新后的显示名称
     * @throws IllegalParamException 如果提供的显示名称长度超过 64 个字符
     */
    public UserProfile withDisplayName(String name) {
        if (name != null && name.length() > 64)
            throw new IllegalParamException("显示名称长度不能超过 64 个字符");
        return new UserProfile(name, this.avatarUrl, this.gender, this.birthday, this.country,
                this.province, this.city, this.addressLine, this.zipcode, this.extra);
    }

    // 其他 withXxx 可按需补充……
}

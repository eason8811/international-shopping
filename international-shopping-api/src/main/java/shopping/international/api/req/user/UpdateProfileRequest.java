package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.Gender;
import shopping.international.domain.model.vo.user.UserProfile;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateProfileRequest {
    /**
     * 显示名 (可空)
     */
    @Nullable
    private String displayName;
    /**
     * 头像URL (可空)
     */
    @Nullable
    private String avatarUrl;
    /**
     * 性别 (UNKNOWN/MALE/FEMALE, 可空)
     */
    @Nullable
    private Gender gender;
    /**
     * 生日 (yyyy-MM-dd, 可空)
     */
    @Nullable
    private String birthday;
    /**
     * 国家/省/市/详细地址/邮编 (可空)
     */
    @Nullable
    private String country;
    @Nullable
    private String province;
    @Nullable
    private String city;
    @Nullable
    private String addressLine;
    @Nullable
    private String zipcode;

    /**
     * 额外信息 JSON 可由调用方在上层转换, 这里简化为 null
     */
    @Nullable
    private Map<String, Object> extra;

    /**
     * 入参校验 (长度与基本格式)
     */
    public void validate() {
        if (displayName != null && displayName.length() > 64)
            throw IllegalParamException.of("显示名称长度不能超过 64 个字符");

        Pattern URL_REGEX = Pattern.compile("^https?://.*$");
        if (avatarUrl != null) {
            avatarUrl = avatarUrl.strip();
            require(URL_REGEX.matcher(avatarUrl).matches(), "头像地址格式错误");
        }

        if (gender != null)
            try {
                Gender.valueOf(gender.name());
            } catch (RuntimeException e) {
                throw IllegalParamException.of("性别格式错误");
            }
        if (birthday != null) {
            try {
                LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (RuntimeException e) {
                throw IllegalParamException.of("生日格式错误");
            }
        }
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
        if (addressLine != null) {
            requireNotBlank(addressLine, "地址行不能为空");
            addressLine = addressLine.strip();
        }
        if (zipcode != null) {
            requireNotBlank(zipcode, "邮编不能为空");
            zipcode = zipcode.strip();
        }
        if (extra != null && extra.isEmpty())
            throw new IllegalParamException("额外信息不能为空");
    }

    /**
     * 转为领域值对象
     */
    public UserProfile toVO() {
        LocalDate birth = null;
        if (birthday != null && !birthday.isBlank())
            birth = LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return UserProfile.of(displayName, avatarUrl, gender, birth,
                country, province, city, addressLine, zipcode, extra);
    }
}

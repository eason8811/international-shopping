package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 修改账户请求体
 *
 * <p>可修改字段: nickname, phone_country_code + phone_national_number (可空)</p>
 */
@Data
public class UpdateAccountRequest {
    /**
     * 新昵称 (可空则不修改)
     */
    @Nullable
    private String nickname;
    /**
     * 新手机号国家码 (可空则不修改, E.164, 不含 '+')
     */
    @Nullable
    private String phoneCountryCode;
    /**
     * 新手机号 national number (可空则不修改, E.164, 国家码之后的 National Significant Number, 仅数字)
     */
    @Nullable
    private String phoneNationalNumber;

    /**
     * 参数合法性校验
     *
     * @throws IllegalParamException 参数非法
     */
    public void validate() {
        if (nickname != null) {
            require(!nickname.isBlank() && nickname.strip().length() <= 64, "昵称长度必须为 1~64 个字符");
            nickname = nickname.strip();
        }

        boolean ccBlank = (phoneCountryCode == null || phoneCountryCode.isBlank());
        boolean nnBlank = (phoneNationalNumber == null || phoneNationalNumber.isBlank());
        if (!ccBlank || !nnBlank) {
            require(!ccBlank && !nnBlank, "手机号字段不完整");
            phoneCountryCode = phoneCountryCode.strip();
            phoneNationalNumber = phoneNationalNumber.strip();
            require(phoneCountryCode.matches("^[1-9][0-9]{0,2}$"), "country_code 格式不正确");
            require(phoneNationalNumber.matches("^[0-9]{1,14}$"), "national_number 格式不正确");
            require((phoneCountryCode.length() + phoneNationalNumber.length()) <= 15, "手机号格式不正确");
        }
    }
}

package shopping.international.api.req.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 注册请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    /**
     * 用户名 (唯一登录名)
     */
    private String username;
    /**
     * 登录密码 (明文, 应用服务内会进行哈希)
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机号国家码 (可选, E.164, 不含 '+')
     */
    private String phoneCountryCode;
    /**
     * 手机号 national number (可选, E.164, 国家码之后的 National Significant Number, 仅数字)
     */
    private String phoneNationalNumber;

    /**
     * 验证请求 DTO 字段是否合法
     *
     * @throws IllegalParamException 如果任一被验证的字段不符合预期条件
     */
    public void validate() {
        requireNotBlank(username, "用户名不能为空");
        requireNotBlank(password, "密码不能为空");
        requireNotBlank(nickname, "昵称不能为空");
        requireNotBlank(email, "邮箱不能为空");

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

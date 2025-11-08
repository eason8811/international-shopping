package shopping.international.api.req.user;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.FieldValidateUtils;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 修改账户请求体
 *
 * <p>可修改字段: nickname, phone (可空)</p>
 */
@Data
public class UpdateAccountRequest {
    /**
     * 新昵称 (可空则不修改)
     */
    @Nullable
    private String nickname;
    /**
     * 新手机号 (可空则不修改, 若非空需符合 E.164 简化校验)
     */
    @Nullable
    private String phone;

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

        if (phone != null) {
            require(phone.strip().length() >= 6 && phone.strip().length() <= 32, "手机号格式不正确");
            phone = phone.strip();
        }
    }
}

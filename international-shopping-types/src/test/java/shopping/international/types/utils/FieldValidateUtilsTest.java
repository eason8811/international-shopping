package shopping.international.types.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * <b>FieldValidateUtilsTest</b> 通过覆盖核心工具方法验证异常处理与邮箱校验逻辑的健壮性
 */
class FieldValidateUtilsTest {

    /**
     * <b>shouldThrowWhenRequireNotNullFails</b> 验证 {@link FieldValidateUtils#requireNotNull(Object, String)} 会对 null 值抛出异常
     */
    @Test
    void shouldThrowWhenRequireNotNullFails() {
        Assertions.assertThrows(IllegalParamException.class, () -> FieldValidateUtils.requireNotNull(null, "字段必填"));
    }

    /**
     * <b>shouldAcceptValidEmail</b> 确认 {@link FieldValidateUtils#requireIsEmail(String, String)} 对合法邮箱不会抛出异常
     */
    @Test
    void shouldAcceptValidEmail() {
        Assertions.assertDoesNotThrow(() -> FieldValidateUtils.requireIsEmail("demo@example.com", "邮箱格式错误"));
    }

    /**
     * <b>shouldRejectInvalidEmail</b> 验证非法邮箱会导致 {@link FieldValidateUtils#requireIsEmail(String, String)} 抛出 {@link IllegalParamException}
     */
    @Test
    void shouldRejectInvalidEmail() {
        Assertions.assertThrows(IllegalParamException.class, () -> FieldValidateUtils.requireIsEmail("demo@invalid", "邮箱格式错误"));
    }
}

package shopping.international.domain.model.vo.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * <b>EmailAddressTest</b> 聚焦 {@link EmailAddress} 的规范化与合法性校验, 确保邮箱处理逻辑稳定
 */
class EmailAddressTest {

    /**
     * <b>shouldLowercaseAndTrimEmail</b> 断言 {@link EmailAddress#of(String)} 会去除首尾空白并统一转换为小写
     */
    @Test
    void shouldLowercaseAndTrimEmail() {
        EmailAddress emailAddress = EmailAddress.of("  USER@Example.COM  ");
        Assertions.assertEquals("user@example.com", emailAddress.getValue());
    }

    /**
     * <b>shouldRejectInvalidFormat</b> 验证不符合正则的邮箱会抛出 {@link IllegalParamException}
     */
    @Test
    void shouldRejectInvalidFormat() {
        Assertions.assertThrows(IllegalParamException.class, () -> EmailAddress.of("user@invalid"));
    }

    /**
     * <b>shouldSupportNullableFactory</b> 确保 {@link EmailAddress#ofNullable(String)} 在传入 null 时不会抛异常且保持 null 值
     */
    @Test
    void shouldSupportNullableFactory() {
        EmailAddress nullable = EmailAddress.ofNullable(null);
        Assertions.assertNull(nullable.getValue());
    }
}

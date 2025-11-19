package shopping.international.domain.model.vo.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * <b>PhoneNumberTest</b> 覆盖 {@link PhoneNumber} 的核心行为, 确保规范化与校验逻辑可以满足国际手机号格式的需求
 */
class PhoneNumberTest {

    /**
     * <b>shouldNormalizeAndAcceptValidNumbers</b> 验证 {@link PhoneNumber#of(String)} 会去除空格、短横线并维持合法的前缀
     */
    @Test
    void shouldNormalizeAndAcceptValidNumbers() {
        PhoneNumber number = PhoneNumber.of(" +1 23-456 789 ");
        Assertions.assertEquals("+123456789", number.getValue());
    }

    /**
     * <b>shouldRejectInvalidCharacters</b> 确认出现非法字符时 {@link PhoneNumber#of(String)} 会抛出 {@link IllegalParamException}
     */
    @Test
    void shouldRejectInvalidCharacters() {
        Assertions.assertThrows(IllegalParamException.class, () -> PhoneNumber.of("123-45A"));
    }

    /**
     * <b>shouldAllowNullableFactory</b> 断言 {@link PhoneNumber#nullableOf(String)} 在输入为空白时返回值对象但持有 null 值
     */
    @Test
    void shouldAllowNullableFactory() {
        PhoneNumber nullable = PhoneNumber.nullableOf("  ");
        Assertions.assertNull(nullable.getValue());
    }
}

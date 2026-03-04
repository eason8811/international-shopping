package shopping.international.trigger.controller.customerservice.support;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * customerservice 控制器公共支持类, 统一处理用户身份解析和常见查询参数解析
 */
public final class CustomerServiceControllerSupport {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private CustomerServiceControllerSupport() {
    }

    /**
     * 解析枚举查询参数, 空值返回 null
     *
     * @param raw       原始文本
     * @param enumType  枚举类型
     * @param fieldName 字段名
     * @param <E>       枚举类型参数
     * @return 枚举值, 或 null
     */
    public static <E extends Enum<E>> @Nullable E parseEnumIgnoreBlank(@Nullable String raw,
                                                                        @NotNull Class<E> enumType,
                                                                        @NotNull String fieldName) {
        if (raw == null || raw.isBlank())
            return null;
        String normalized = raw.strip().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalParamException(fieldName + " 不合法: " + raw);
        }
    }

    /**
     * 解析消息排序参数
     *
     * @param order 排序方向
     * @return true 表示升序, false 表示降序
     */
    public static boolean parseMessageOrder(@Nullable String order) {
        if (order == null || order.isBlank())
            return false;
        String normalized = order.strip().toLowerCase(Locale.ROOT);
        if (!"asc".equals(normalized) && !"desc".equals(normalized))
            throw new IllegalParamException("order 只支持 asc 或 desc");
        return "asc".equals(normalized);
    }

    /**
     * 解析时间文本, 支持 ISO_OFFSET_DATE_TIME 和 yyyy-MM-dd HH:mm:ss
     *
     * @param value 时间文本
     * @return 本地时间
     */
    public static @Nullable LocalDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        String text = value.strip();
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception exception) {
                throw new IllegalParamException("时间格式不合法: " + value);
            }
        }
    }

    /**
     * 从安全上下文读取当前用户主键
     *
     * @return 当前用户主键
     */
    public static @NotNull Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId)
            return userId;
        if (principal instanceof String userId)
            return Long.parseLong(userId);
        throw new AccountException("无法解析当前用户");
    }
}

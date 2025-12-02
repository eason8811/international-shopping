package shopping.international.types.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 字段验证工具类, 当字段不符合要求时抛出 {@link IllegalParamException} 异常
 */
public final class FieldValidateUtils {
    /**
     * locale 格式: 允许字母数字及横线/下划线, 段长 2-8
     */
    public static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
    /**
     * 货币格式: 3 位大写字母
     */
    public static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    /**
     * 工具类不能实例化
     */
    private FieldValidateUtils() {
    }

    /**
     * 确保给定的对象不为空, 如果对象为空则抛出 {@link IllegalParamException} 异常
     *
     * @param object 待检查的对象
     * @param msg    当 <code>object</code> 为空时, 抛出异常的信息
     * @throws IllegalParamException 如果 <code>object</code> 为 <code>null</code>
     */
    @Contract(value = "null, !null -> fail", pure = true)
    public static void requireNotNull(Object object, @Nls String msg) {
        if (Objects.isNull(object))
            throw IllegalParamException.of(msg);
    }

    /**
     * 确保给定的字符串既不为 <code>null</code> 也不为空白, 如果字符串不符合要求则抛出 {@link IllegalParamException} 异常
     *
     * @param string 待检查的字符串
     * @param msg    当 <code>string</code> 为 <code>null</code> 或空白时, 抛出异常的信息
     * @throws IllegalParamException 如果 <code>string</code> 为 <code>null</code> 或仅包含空白字符
     */
    @Contract(value = "null, !null -> fail", pure = true)
    public static void requireNotBlank(String string, @Nls String msg) {
        if (string == null || string.isBlank())
            throw IllegalParamException.of(msg);
    }

    /**
     * 确保给定的条件为 <code>true</code>, 如果条件不满足则抛出 {@link IllegalParamException} 异常
     *
     * @param ok  用于检查的布尔值, 应该是某个条件的结果
     * @param msg 当 <code>ok</code> 为 <code>false</code> 时, 抛出异常的信息
     * @throws IllegalParamException 如果 <code>ok</code> 为 <code>false</code>
     */
    @Contract(value = "false, !null -> fail", pure = true)
    public static void require(boolean ok, @Nls String msg) {
        if (!ok)
            throw IllegalParamException.of(msg);
    }

    /**
     * 确保给定的字符串是有效的电子邮件地址, 如果不是则抛出 {@link IllegalParamException} 异常
     *
     * @param email 待检查的电子邮件地址
     * @param msg   当 <code>email</code> 不符合电子邮件格式时, 抛出异常的信息
     * @throws IllegalParamException 如果 <code>email</code> 为 <code>null</code>, 空白或不符合电子邮件格式
     */
    @Contract("null,_->fail")
    public static void requireIsEmail(String email, String msg) {
        requireNotNull(email, "邮箱不能为空");
        email = email.strip();
        Pattern EMAIL_REGEX = Pattern.compile(
                "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@" +
                        "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$"
        );
        requireNotBlank(email, "邮箱不能为空");
        if (!EMAIL_REGEX.matcher(email).matches())
            throw IllegalParamException.of(msg);
    }

    /**
     * 规范化 locale
     *
     * @param locale 请求 locale
     * @return 合法 locale, 空则返回 null
     */
    public static String normalizeLocale(String locale) {
        if (locale == null)
            return null;
        String trimmed = locale.strip();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > 16)
            throw new IllegalParamException("locale 最长 16 个字符");
        if (!LOCALE_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("locale 格式不合法");
        return trimmed;
    }

    /**
     * 规范化 currency
     *
     * @param currency 请求币种
     * @return 合法币种, 空则返回 null
     */
    public static String normalizeCurrency(String currency) {
        if (currency == null)
            return null;
        String trimmed = currency.strip().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty())
            return null;
        if (!CURRENCY_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("currency 需为 3 位字母代码");
        return trimmed;
    }

    /**
     * 规范化标签列表
     *
     * @param tags 标签列表
     * @return 去重后的标签
     */
    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty())
            return Collections.emptyList();
        Set<String> dedup = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null)
                continue;
            String trimmed = tag.trim();
            if (trimmed.isEmpty())
                continue;
            dedup.add(trimmed);
        }
        return dedup.isEmpty() ? Collections.emptyList() : List.copyOf(dedup);
    }

    /**
     * 规范化关键词
     *
     * @param keyword 关键词
     * @param maxLen  最大长度
     * @return 合法关键词或 null
     */
    public static String normalizeKeyword(String keyword, int maxLen) {
        if (keyword == null)
            return null;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > maxLen)
            throw new IllegalParamException("关键词长度不能超过 " + maxLen);
        return trimmed;
    }

    /**
     * 校验非负价格
     *
     * @param price     价格
     * @param fieldName 字段名
     * @return 合法价格或 null
     */
    public static BigDecimal normalizeNonNegativePrice(BigDecimal price, String fieldName) {
        if (price == null)
            return null;
        if (price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalParamException(fieldName + " 不能为负数");
        return price;
    }

    /**
     * 确保给定的补丁字段满足特定条件, 如果不满足则抛出异常
     *
     * @param patchFiled 待检查的补丁字段
     * @param okFunc     用于判断 <code>patchFiled</code> 是否有效的函数, 接受一个字符串参数并返回布尔值
     * @param msg        当 <code>okFunc</code> 返回 <code>false</code> 时, 抛出异常的信息
     * @return 处理后的补丁字段, 去除首尾空白字符后的字符串
     * @throws IllegalParamException 如果 <code>patchFiled</code> 不符合 <code>okFunc</code> 的条件
     */
    public static String requirePatchField(String patchFiled, Function<String, Boolean> okFunc, String msg) {
        if (patchFiled != null) {
            patchFiled = patchFiled.strip();
            require(okFunc.apply(patchFiled), msg);
            return patchFiled;
        }
        return patchFiled;
    }

    /**
     * 确保给定的创建字段满足特定条件, 如果不满足则抛出异常
     *
     * @param createFiled 待检查的创建字段
     * @param nullMsg     当 <code>createFiled</code> 为 <code>null</code> 时, 抛出异常的信息
     * @param okFunc      用于判断 <code>createFiled</code> 是否有效的函数, 接受一个字符串参数并返回布尔值
     * @param notOkMsg    当 <code>okFunc</code> 返回 <code>false</code> 时, 抛出异常的信息
     * @return 处理后的创建字段, 去除首尾空白字符后的字符串
     * @throws IllegalParamException 如果 <code>createFiled</code> 为 <code>null</code>, 或者 <code>okFunc</code> 返回 <code>false</code>
     */
    public static @NotNull String requireCreateField(String createFiled, String nullMsg, @NotNull Function<String, Boolean> okFunc, String notOkMsg) {
        requireNotNull(createFiled, nullMsg);
        createFiled = createFiled.strip();
        require(okFunc.apply(createFiled), notOkMsg);
        return createFiled;
    }
}

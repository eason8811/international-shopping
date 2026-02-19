package shopping.international.types.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * 此方法用于检查传入的对象是否为 <code>null</code>, 如果对象是 <code>null</code>, 则抛出一个异常；否则, 返回该对象
     *
     * @param object 需要被检查是否为 <code>null</code> 的对象
     * @param msg    如果对象是 <code>null</code> 时, 抛出异常中携带的消息字符串
     * @return 返回非 <code>null</code> 的输入对象
     * @throws IllegalParamException 当传入的对象为 <code>null</code> 时抛出此异常
     */
    @NotNull
    @Contract(value = "null, !null -> fail; !null, !null -> !null", pure = true)
    public static <T> T normalizeNotNull(T object, @Nls String msg) {
        if (Objects.isNull(object))
            throw IllegalParamException.of(msg);
        return object;
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
     * 确保提供的字符串既非空也非空白, 并返回该字符串. 如果字符串为空或仅包含空白字符, 则抛出异常.
     *
     * @param string 需要验证和可能返回的字符串
     * @param msg    当提供的字符串为空或空白时, 用于构建异常信息的消息
     * @return 返回验证后的非空且非空白的原始字符串
     * @throws IllegalParamException 如果提供的字符串为空或仅由空白字符组成
     */
    @NotNull
    @Contract(value = "null, !null -> fail; !null, !null -> !null", pure = true)
    public static String normalizeNotBlank(String string, @Nls String msg) {
        if (string == null || string.isBlank())
            throw IllegalParamException.of(msg);
        return string;
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
            return "USD";
        String trimmed = currency.strip().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty())
            return "USD";
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
     * 规范化可为空的字段, 确保其不为 null 且满足特定条件
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>如果字段为 <code>null</code>, 直接返回 <code>null</code></li>
     *     <li>去除字段首尾空白字符</li>
     *     <li>确保字段既不为 <code>null</code> 也不为空白, 否则抛出 {@link IllegalParamException} 异常</li>
     *     <li>通过提供的函数验证字段, 如果不满足条件则抛出 {@link IllegalParamException} 异常</li>
     *     <li>如果字段为空白, 返回 <code>null</code>; 否则返回规范化后的字段</li>
     * </ul>
     *
     * @param nullableField 待处理的字段, 可以为 <code>null</code>
     * @param blankMsg      当字段为空白时, 抛出异常的信息
     * @param okFunc        用于验证字段是否满足特定条件的函数
     * @param msg           当字段不满足 <code>okFunc</code> 条件时, 抛出异常的信息
     * @return 规范化后的字段, 如果字段为空白则返回 <code>null</code>
     * @throws IllegalParamException 如果字段为空白或不满足 <code>okFunc</code> 条件
     */
    public static String normalizeNullableField(String nullableField, String blankMsg, Function<String, Boolean> okFunc, String msg) {
        if (nullableField != null) {
            nullableField = nullableField.strip();
            requireNotBlank(nullableField, blankMsg);
            require(okFunc.apply(nullableField), msg);
            return nullableField;
        }
        return nullableField;
    }

    /**
     * 规范化并验证一个非空字段, 确保其不为空且满足特定条件
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>去除字段首尾空白字符</li>
     *     <li>确保字段既不为 <code>null</code> 也不为空白, 否则抛出 {@link IllegalParamException} 异常</li>
     *     <li>通过提供的函数验证字段, 如果不满足条件则抛出 {@link IllegalParamException} 异常</li>
     * </ul>
     *
     * @param notNullField 待处理的字段, 必须不为 <code>null</code>
     * @param blankMsg     当字段为空白时, 抛出异常的信息
     * @param okFunc       用于验证字段是否满足特定条件的函数
     * @param notOkMsg     当字段不满足 <code>okFunc</code> 条件时, 抛出异常的信息
     * @return 规范化后的字段
     * @throws IllegalParamException 如果字段为空白或不满足 <code>okFunc</code> 条件
     */
    @NotNull
    public static String normalizeNotNullField(String notNullField, String blankMsg, @NotNull Function<String, Boolean> okFunc, String notOkMsg) {
        requireNotBlank(notNullField, blankMsg);
        notNullField = notNullField.strip();
        require(okFunc.apply(notNullField), notOkMsg);
        return notNullField;
    }

    /**
     * 确保给定的列表被规范化, 即去除所有 <code>null</code> 值并对每个元素进行验证
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>如果输入列表为 <code>null</code> 或空, 则返回一个空列表</li>
     *     <li>过滤掉列表中的所有 <code>null</code> 值</li>
     *     <li>对列表中剩余的每个非 <code>null</code> 元素调用其 {@link Verifiable#validate()} 方法来确保它们符合预定义的规则或条件</li>
     * </ul>
     *
     * @param <T>       泛型类型参数, 表示实现了 {@link Verifiable} 接口的具体类型
     * @param fieldList 待处理的列表, 其元素需要实现 {@link Verifiable} 接口
     * @return 一个新的列表, 包含了经过验证且非 <code>null</code> 的元素
     * @throws IllegalArgumentException 如果列表中的任何元素在验证过程中失败, 将抛出此异常, 异常信息将提供具体的错误详情
     */
    @NotNull
    public static <T extends Verifiable> List<T> normalizeFieldList(List<T> fieldList) {
        return normalizeFieldList(fieldList, Verifiable::validate);
    }

    /**
     * 确保给定的列表被规范化, 即去除所有 <code>null</code> 值并对每个元素进行验证
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>如果输入列表为 <code>null</code> 或空, 则返回一个空列表</li>
     *     <li>过滤掉列表中的所有 <code>null</code> 值</li>
     *     <li>对列表中剩余的每个非 <code>null</code> 元素调用提供的验证函数来确保它们符合预定义的规则或条件</li>
     * </ul>
     *
     * @param <T>          泛型类型参数, 表示实现了 {@link Verifiable} 接口的具体类型
     * @param fieldList    待处理的列表, 其元素需要实现 {@link Verifiable} 接口
     * @param validateFunc 用于验证列表中每个元素的函数
     * @return 一个新的列表, 包含了经过验证且非 <code>null</code> 的元素
     * @throws IllegalArgumentException 如果列表中的任何元素在验证过程中失败, 将抛出此异常, 异常信息将提供具体的错误详情
     */
    @NotNull
    public static <T extends Verifiable> List<T> normalizeFieldList(List<T> fieldList, Consumer<T> validateFunc) {
        if (fieldList == null || fieldList.isEmpty())
            return Collections.emptyList();
        return fieldList.stream()
                .filter(Objects::nonNull)
                .peek(validateFunc)
                .toList();
    }

    /**
     * 确保给定的列表被规范化, 并且所有元素基于提供的键函数是唯一的
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>如果输入列表为 {@code null} 或空, 则返回一个空列表</li>
     *     <li>过滤掉列表中的所有 {@code null} 值</li>
     *     <li>对列表中剩余的每个非 {@code null} 元素调用 {@link Verifiable#validate()} 方法来确保它们符合预定义的规则或条件</li>
     *     <li>使用提供的键函数 {@code distinctKeyFunc} 来提取每个元素的唯一标识, 并检查这些标识是否唯一</li>
     * </ul>
     *
     * @param <T>             泛型类型参数, 表示实现了 {@link Verifiable} 接口的具体类型
     * @param fieldList       待处理的列表, 其元素需要实现 {@link Verifiable} 接口
     * @param distinctKeyFunc 用于从每个元素中提取唯一标识的函数
     * @param duplicateMsg    如果列表中存在重复元素, 抛出异常时使用的错误信息
     * @return 一个新的列表, 包含了经过验证且非 {@code null} 的元素, 并且这些元素基于提供的键函数是唯一的
     * @throws IllegalParamException 如果列表中存在基于提供的键函数不唯一的元素
     */
    @NotNull
    public static <T extends Verifiable> List<T> normalizeDistinctList(List<T> fieldList, Function<T, Object> distinctKeyFunc, String duplicateMsg) {
        return normalizeDistinctList(fieldList, Verifiable::validate, distinctKeyFunc, duplicateMsg);
    }

    /**
     * 确保给定的列表被规范化, 并且所有元素基于提供的键函数是唯一的
     *
     * <p>该方法会执行以下操作:</p>
     * <ul>
     *     <li>如果输入列表为 {@code null} 或空, 则返回一个空列表</li>
     *     <li>过滤掉列表中的所有 {@code null} 值</li>
     *     <li>对列表中剩余的每个非 {@code null} 元素调用提供的验证函数来确保它们符合预定义的规则或条件</li>
     *     <li>使用提供的键函数 {@code distinctKeyFunc} 来提取每个元素的唯一标识, 并检查这些标识是否唯一</li>
     * </ul>
     *
     * @param <T>             泛型类型参数, 表示实现了 {@link Verifiable} 接口的具体类型
     * @param fieldList       待处理的列表, 其元素需要实现 {@link Verifiable} 接口
     * @param validateFunc    用于验证列表中每个元素的函数
     * @param distinctKeyFunc 用于从每个元素中提取唯一标识的函数
     * @param duplicateMsg    如果列表中存在重复元素, 抛出异常时使用的错误信息
     * @return 一个新的列表, 包含了经过验证且非 {@code null} 的元素, 并且这些元素基于提供的键函数是唯一的
     * @throws IllegalParamException 如果列表中存在基于提供的键函数不唯一的元素
     */
    @NotNull
    public static <T extends Verifiable> List<T> normalizeDistinctList(List<T> fieldList, Consumer<T> validateFunc, Function<T, Object> distinctKeyFunc, String duplicateMsg) {
        if (fieldList == null || fieldList.isEmpty())
            return Collections.emptyList();
        Set<Object> distinctKeys = fieldList.stream()
                .filter(Objects::nonNull)
                .map(distinctKeyFunc)
                .collect(Collectors.toSet());
        List<T> normalizedList = fieldList.stream()
                .filter(Objects::nonNull)
                .peek(validateFunc)
                .toList();
        require(distinctKeys.size() == normalizedList.size(), duplicateMsg);
        return normalizedList;
    }

    /**
     * 获取国际化字符串或默认值, 如果提供的国际化对象为空, 则返回默认的国际化字符串
     *
     * @param <T>         国际化对象类型
     * @param i18n        国际化对象, 可以是任何实现了对应接口的对象, 用于获取特定语言下的文本
     * @param i18nGetFunc 从国际化对象中提取所需字符串的方法引用或函数
     * @param defaultI18n 当国际化对象为 {@code null} 时使用的默认国际化字符串
     * @return 如果国际化对象不为空, 返回通过 {@code i18nGetFunc} 提取的字符串; 否则返回 {@code defaultI18n}
     */
    @Nullable
    public static <T> String getI18nOrDefault(T i18n, Function<T, String> i18nGetFunc, String defaultI18n) {
        return i18n == null ? defaultI18n : i18nGetFunc.apply(i18n);
    }

    /**
     * 从给定的映射中, 根据提供的路径逐层查找并返回最终值作为字符串
     *
     * <p>此方法接收一个映射和一个可变参数路径, 沿着路径在映射中查找值, 如果在任何点上找不到对应键或者找到的值不是映射,
     * 则返回 {@code null}, 如果成功遍历完整个路径, 则尝试将最后一个值转换为字符串后返回</p>
     *
     * @param map  映射, 从中开始查找路径
     * @param path 可变参数列表, 表示要遍历的键路径
     * @return 如果能够沿着指定路径找到非空值, 并且该值可以被成功转换为字符串, 则返回这个字符串; 否则返回 {@code null}
     */
    public static @Nullable String nestedString(@NotNull Map<String, Object> map, String... path) {
        Object cur = map;
        for (String p : path) {
            if (!(cur instanceof Map<?, ?> m))
                return null;
            cur = m.get(p);
            if (cur == null)
                return null;
        }
        return asString(cur);
    }

    /**
     * 将给定的对象转换为字符串形式
     *
     * <p>如果对象为 {@code null}, 则返回 {@code null}否则, 使用 {@link String#valueOf(Object)} 方法将对象转换为字符串</p>
     *
     * @param o 待转换的对象
     * @return 转换后的字符串, 如果输入对象为 {@code null}, 则返回 {@code null}
     */
    public static @Nullable String asString(Object o) {
        if (o == null)
            return null;
        String s = String.valueOf(o);
        if (s.isBlank())
            return null;
        return s;
    }
}

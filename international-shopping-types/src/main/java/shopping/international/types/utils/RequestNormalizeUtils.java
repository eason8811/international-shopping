package shopping.international.types.utils;

import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 接口入参规范化工具 RequestNormalizeUtils
 *
 * <p>负责对 locale、currency、关键词、标签等通用字段做基础校验与规范化</p>
 */
public final class RequestNormalizeUtils {
    /**
     * locale 格式: 允许字母数字及横线/下划线, 段长 2-8
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
    /**
     * 货币格式: 3 位大写字母
     */
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private RequestNormalizeUtils() {
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
        String trimmed = locale.trim();
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
        String trimmed = currency.trim().toUpperCase(Locale.ROOT);
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
}

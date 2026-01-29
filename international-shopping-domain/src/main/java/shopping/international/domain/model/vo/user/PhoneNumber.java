package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 手机号值对象 (E.164)
 *
 * <p>统一存储为 E.164 规范字符串: {@code +}{@code country_code}{@code national_number}</p>
 * <ul>
 *     <li>{@code country_code}: 国家码 (1~3 位数字, 首位不能为 0)</li>
 *     <li>{@code national_number}: 国家码之后的 National Significant Number (1~14 位数字)</li>
 *     <li>E.164 最大长度: 国家码 + national_number 总计不超过 15 位数字 (不含 {@code +})</li>
 * </ul>
 *
 * <p>注意: 若要持久化到类似 {@code user_account} 这种拆分字段的表, 请使用
 * {@link #ofParts(String, String)} 或 {@link #nullableOfParts(String, String)} 构造,
 * 以保证 {@code countryCode/nationalNumber} 可用。</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PhoneNumber {

    /**
     * E.164 正则 (严格要求以 '+' 开头, 且总位数不超过 15 位数字)
     */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{1,14}$");

    /**
     * 手机号值 (E.164 规范化后)
     */
    private final String value;

    /**
     * 国家码 (不含 '+')
     */
    private final @Nullable String countryCode;

    /**
     * 国家码之后的 national_number (仅数字)
     */
    private final @Nullable String nationalNumber;

    private PhoneNumber(String value, @Nullable String countryCode, @Nullable String nationalNumber) {
        this.value = value;
        this.countryCode = countryCode;
        this.nationalNumber = nationalNumber;
    }

    /**
     * 由拆分字段重建 {@link PhoneNumber} (用于持久化层读模型装配)
     *
     * @param countryCode    国家码 (不含 '+')
     * @param nationalNumber 国家码之后的 national_number (仅数字)
     * @return {@link PhoneNumber} 实例
     * @throws IllegalParamException 如果字段缺失或无法组成有效的 E.164 号码时抛出
     */
    public static PhoneNumber ofParts(String countryCode, String nationalNumber) {
        requireNotNull(countryCode, "country_code 不能为空");
        requireNotNull(nationalNumber, "national_number 不能为空");
        String cc = countryCode.trim();
        String nn = nationalNumber.trim();
        require(!cc.isEmpty(), "country_code 不能为空");
        require(!nn.isEmpty(), "national_number 不能为空");
        require(cc.matches("^[1-9][0-9]{0,2}$"), "country_code 格式不正确");
        require(nn.matches("^[0-9]{1,14}$"), "national_number 格式不正确");
        require((cc.length() + nn.length()) <= 15, "手机号格式不正确");
        String e164 = "+" + cc + nn;
        require(E164.matcher(e164).matches(), "手机号格式不正确");
        return new PhoneNumber(e164, cc, nn);
    }

    /**
     * 由拆分字段重建可为空的 {@link PhoneNumber}
     *
     * @param countryCode    国家码 (不含 '+'), 可空
     * @param nationalNumber 国家码之后的 national_number, 可空
     * @return {@link PhoneNumber} 实例, 若两者均为空则返回 {@code null}
     * @throws IllegalParamException 如果仅其中一个字段存在, 或无法组成有效号码时抛出
     */
    public static @Nullable PhoneNumber nullableOfParts(String countryCode, String nationalNumber) {
        boolean ccBlank = (countryCode == null || countryCode.trim().isEmpty());
        boolean nnBlank = (nationalNumber == null || nationalNumber.trim().isEmpty());
        if (ccBlank && nnBlank)
            return null;
        require(!ccBlank && !nnBlank, "手机号字段不完整");
        return ofParts(countryCode, nationalNumber);
    }

    /**
     * 将手机号文本做轻量规范化 (移除空格与短横线)
     *
     * @param raw 原始手机号字符串
     * @return 预处理后的手机号字符串
     * @throws IllegalParamException 如果提供的手机号为空或格式不正确时抛出
     */
    private static String normalize(String raw) {
        requireNotNull(raw, "手机号不能为空");
        String trimmed = raw.trim();
        require(!trimmed.isEmpty(), "手机号不能为空");

        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean firstChar = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char chr = trimmed.charAt(i);

            // 忽略常见分隔符: 空格和短横线
            if (chr == ' ' || chr == '-')
                continue;

            if (chr == '+') {
                // '+' 只能出现在第一个字符, 且只能出现一次
                if (!firstChar)
                    throw IllegalParamException.of("手机号格式不正确");
                sb.append(chr);
                firstChar = false;
            } else if (Character.isDigit(chr)) {
                sb.append(chr);
                firstChar = false;
            } else
                // 出现任何其它字符一律视为非法
                throw IllegalParamException.of("手机号格式不正确");

        }

        String normalized = sb.toString();
        require(!normalized.isEmpty(), "手机号不能为空");
        return normalized;
    }

    /**
     * 获取 country_code (强制要求存在)
     *
     * <p>用于像 {@code user_account} 这类以拆分字段持久化的场景。</p>
     *
     * @return country_code
     * @throws IllegalStateException 当该 {@link PhoneNumber} 未携带拆分字段时抛出
     */
    public String requireCountryCode() {
        if (countryCode == null || countryCode.isBlank())
            throw new IllegalStateException("手机号缺少 country_code, 请使用 PhoneNumber.ofParts(countryCode, nationalNumber) 构造");
        return countryCode;
    }

    /**
     * 获取 national_number (强制要求存在)
     *
     * <p>用于像 {@code user_account} 这类以拆分字段持久化的场景。</p>
     *
     * @return national_number
     * @throws IllegalStateException 当该 {@link PhoneNumber} 未携带拆分字段时抛出
     */
    public String requireNationalNumber() {
        if (nationalNumber == null || nationalNumber.isBlank())
            throw new IllegalStateException("手机号缺少 national_number, 请使用 PhoneNumber.ofParts(countryCode, nationalNumber) 构造");
        return nationalNumber;
    }
}

package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.security.SecureRandom;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 订单号值对象 (对外展示的业务单号)
 */
@Getter
@ToString
@EqualsAndHashCode
public final class OrderNo {
    /**
     * 订单号值
     */
    private final String value;

    /**
     * 订单号编码字符表 (Crockford Base32)
     *
     * <p>字符集不包含易混淆字符 I/L/O/U, 便于人工沟通与客服检索</p>
     */
    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /**
     * 生成随机段的安全随机数
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 构造一个新的 {@code OrderNo} 对象, 代表订单号值对象
     *
     * <p>该构造函数为私有, 应通过静态工厂方法 {@link OrderNo#of(String)} 创建实例</p>
     *
     * @param value 订单号的具体内容, 不允许为空字符串或不满足长度要求
     */
    private OrderNo(String value) {
        this.value = value;
    }

    /**
     * 创建一个新的 {@code OrderNo} 对象, 代表订单号值对象
     *
     * <p>该方法首先确保传入的原始订单号字符串不为空且非空白, 然后检查其长度是否在 10 到 32 个字符之间, 如果这些条件都满足, 则创建并返回一个新的 {@code OrderNo} 实例</p>
     *
     * @param raw 原始订单号字符串, 不允许为 null 或空白, 长度需在 10-32 个字符之间
     * @return 新创建的 {@link OrderNo} 实例
     * @throws IllegalParamException 如果原始订单号为空, 或者长度不在 10-32 个字符范围内
     */
    public static OrderNo of(String raw) {
        requireNotNull(raw, "订单号不能为空");
        String trimmed = raw.strip();
        requireNotBlank(trimmed, "订单号不能为空");
        require(trimmed.length() >= 10 && trimmed.length() <= 32, "订单号长度需在 10-32 之间");
        return new OrderNo(trimmed);
    }

    /**
     * 生成一个新的订单号
     *
     * <p>生成策略:</p>
     * <ul>
     *     <li>使用当前毫秒时间戳 (48 bit) + 安全随机数 (80 bit) 组成 128 bit</li>
     *     <li>按 Crockford Base32 编码为 26 位字符串, 与表字段 {@code orders.order_no CHAR(26)} 对齐</li>
     * </ul>
     *
     * <p><b>说明:</b> 该方法不保证单机内严格单调递增, 但具备较高的全局唯一性</p>
     *
     * @return 新生成的 {@link OrderNo}
     */
    public static OrderNo generate() {
        byte[] bytes = new byte[16];

        // 1) 48bit 时间戳写入前 6 个字节 (大端)
        long ms = System.currentTimeMillis();
        bytes[0] = (byte) ((ms >>> 40) & 0xFF);
        bytes[1] = (byte) ((ms >>> 32) & 0xFF);
        bytes[2] = (byte) ((ms >>> 24) & 0xFF);
        bytes[3] = (byte) ((ms >>> 16) & 0xFF);
        bytes[4] = (byte) ((ms >>> 8) & 0xFF);
        bytes[5] = (byte) (ms & 0xFF);

        // 2) 80bit 随机段写入后 10 个字节
        byte[] rnd = new byte[10];
        SECURE_RANDOM.nextBytes(rnd);
        System.arraycopy(rnd, 0, bytes, 6, rnd.length);

        return new OrderNo(encodeCrockfordBase32(bytes));
    }

    /**
     * 将 16 字节 (128 bit) 编码为 26 位 Crockford Base32 字符串
     *
     * @param bytes 原始字节数组, 长度必须为 16
     * @return 26 位 Base32 字符串
     */
    private static String encodeCrockfordBase32(byte[] bytes) {
        requireNotNull(bytes, "bytes 不能为空");
        require(bytes.length == 16, "bytes 长度必须为 16");

        char[] out = new char[26];
        int outIndex = 0;
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int val = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                out[outIndex++] = CROCKFORD_BASE32[val];
                if (outIndex == 26)
                    return new String(out);
            }
        }

        // 16 bytes = 128 bits, 理论上会剩余 3 bits, 补齐最后一个 5bit 输出
        if (outIndex < 26) {
            int val = (buffer << (5 - bitsLeft)) & 0x1F;
            out[outIndex++] = CROCKFORD_BASE32[val];
        }
        while (outIndex < 26)
            out[outIndex++] = CROCKFORD_BASE32[0];
        return new String(out);
    }
}

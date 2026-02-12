package shopping.international.domain.model.vo;

import java.security.SecureRandom;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

public final class NoGenerator {
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
     * @return 新生成的号码
     */
    public static String generate() {
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

        return encodeCrockfordBase32(bytes);
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

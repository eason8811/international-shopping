package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.Arrays;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 加密密文值对象 (用于存储 VARBINARY: access_token / refresh_token)
 * <p>不可变，仅封装字节数组的拷贝，避免外部修改</p>
 */
@Getter
@EqualsAndHashCode
@ToString(exclude = "bytes")
public final class EncryptedSecret {
    /**
     * 加密后的密文字节数组
     * <p>此字段用于存储加密后的数据, 例如 access_token 或 refresh_token 的二进制形式</p>
     * <p>该字节数组是不可变的, 在构造时会复制一份以确保外部无法修改其内容, 从而保证数据的安全性</p>
     */
    private final byte[] bytes;

    /**
     * 构造方法, 私有化, 不允许外部直接创建实例
     *
     * @param bytes 原始密文字节数组 (构造器内部会复制一份以避免外部修改)
     */
    private EncryptedSecret(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * 通过给定的字节数组创建一个 <code>EncryptedSecret</code> 对象
     *
     * @param bytes 原始密文字节 (方法内会复制一份)
     * @return 新创建的 <code>EncryptedSecret</code> 对象
     * @throws IllegalParamException 如果提供的密文字节数组为 null
     */
    public static EncryptedSecret of(byte[] bytes) {
        requireNotNull(bytes, "密文字节数组不能为 null");
        return new EncryptedSecret(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * 返回拷贝，避免外部修改内部字节
     */
    public byte[] copy() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}

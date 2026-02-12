package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.vo.NoGenerator;
import shopping.international.types.exceptions.IllegalParamException;

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
        return new OrderNo(NoGenerator.generate());
    }
}

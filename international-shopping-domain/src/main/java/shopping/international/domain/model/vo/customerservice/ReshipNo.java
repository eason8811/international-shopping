package shopping.international.domain.model.vo.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.vo.NoGenerator;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 补发单号值对象, 对应表 `aftersales_reship.reship_no`
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ReshipNo {
    /**
     * 补发单号值
     */
    private final String value;

    /**
     * 构造补发单号值对象
     *
     * @param value 补发单号值
     */
    private ReshipNo(String value) {
        this.value = value;
    }

    /**
     * 从字符串创建补发单号值对象
     *
     * @param raw 原始补发单号
     * @return 补发单号值对象
     */
    public static ReshipNo of(String raw) {
        requireNotBlank(raw, "reshipNo 不能为空");
        String normalized = raw.strip();
        require(normalized.length() >= 10 && normalized.length() <= 32, "reshipNo 长度需在 10 到 32 之间");
        return new ReshipNo(normalized);
    }

    /**
     * 生成新的补发单号值对象
     *
     * @return 补发单号值对象
     */
    public static ReshipNo generate() {
        return new ReshipNo(NoGenerator.generate());
    }
}

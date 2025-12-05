package shopping.international.api.resp.products;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AbstractSpecValueRespond 规格值的抽象响应类
 */
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractSpecValueRespond {
    /**
     * 规格值 ID
     */
    private Long valueId;
    /**
     * 规格值编码
     */
    private String valueCode;
    /**
     * 规格值名称
     */
    private String valueName;
    /**
     * 规格值属性
     */
    private Object attributes;
}

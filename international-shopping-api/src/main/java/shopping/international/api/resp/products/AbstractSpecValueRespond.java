package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AbstractSpecValueRespond 规格值的抽象响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

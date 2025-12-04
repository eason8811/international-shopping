package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

/**
 * 抽象规格响应类, 用于表示商品的规格信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractSpecRespond {
    /**
     * 规格 ID
     */
    private Long specId;
    /**
     * 规格编码
     */
    private String specCode;
    /**
     * 规格名称
     */
    private String specName;
    /**
     * 规格类型
     */
    private SpecType specType;
    /**
     * 是否必选
     */
    private Boolean isRequired;
    /**
     * 规格值列表
     */
    private List<? extends AbstractSpecValueRespond> values;
}

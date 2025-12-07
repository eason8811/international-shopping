package shopping.international.api.resp.products;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import shopping.international.domain.model.enums.products.SpecType;

/**
 * 抽象规格响应类, 用于表示商品的规格信息
 */
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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
}

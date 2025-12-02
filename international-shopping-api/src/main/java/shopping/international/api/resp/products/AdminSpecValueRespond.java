package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.products.ProductSpecValue;

import java.util.List;

/**
 * 规格值响应 SpecValueRespond
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSpecValueRespond {
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
    /**
     * 规格值多语言列表
     */
    private List<SpecValueI18nPayloadRespond> i18nList;

    /**
     * 从规格值实体构建响应
     *
     * @param value 规格值实体
     * @return 规格值响应
     */
    public static AdminSpecValueRespond from(ProductSpecValue value) {
        List<SpecValueI18nPayloadRespond> i18nList = value.getI18nList() == null ? List.of()
                : value.getI18nList().stream()
                .map(item -> new SpecValueI18nPayloadRespond(item.getLocale(), item.getValueName()))
                .toList();
        return new AdminSpecValueRespond(
                value.getId(),
                value.getValueCode(),
                value.getValueName(),
                value.getAttributes(),
                i18nList
        );
    }

    /**
     * 规格值多语言响应 SpecValueI18nRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecValueI18nPayloadRespond {
        /**
         * 语言代码
         */
        private String locale;
        /**
         * 本地化规格值名称
         */
        private String valueName;
    }
}

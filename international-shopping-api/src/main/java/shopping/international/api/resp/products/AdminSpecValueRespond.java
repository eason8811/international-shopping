package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.products.ProductSpecValue;

import java.util.List;

/**
 * 规格值响应 AdminSpecValueRespond
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminSpecValueRespond extends AbstractSpecValueRespond {
    /**
     * 规格值多语言列表
     */
    private List<SpecValueI18nPayloadRespond> i18nList;

    /**
     * 构造一个 AdminSpecValueRespond 实例, 该实例用于表示规格值的响应信息, 包括多语言支持
     *
     * @param valueId    规格值 ID
     * @param valueCode  规规值代码
     * @param valueName  规格值名称
     * @param attributes 规格值属性, 可以是任何对象
     * @param i18nList   规格值的多语言信息列表, 每个元素为 {@link AdminSpecValueRespond.SpecValueI18nPayloadRespond} 类型
     */
    private AdminSpecValueRespond(Long valueId, String valueCode, String valueName, Object attributes, List<SpecValueI18nPayloadRespond> i18nList) {
        super(valueId, valueCode, valueName, attributes);
        this.i18nList = i18nList;
    }

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

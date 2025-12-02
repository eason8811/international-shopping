package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

/**
 * 规格响应 SpecRespond
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSpecRespond {
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
     * 规格多语言列表
     */
    private List<SpecI18nPayloadRespond> i18nList;
    /**
     * 规格值列表
     */
    private List<AdminSpecValueRespond> values;

    /**
     * 从规格实体构建响应
     *
     * @param spec 规格实体
     * @return 规格响应
     */
    public static AdminSpecRespond from(ProductSpec spec) {
        List<SpecI18nPayloadRespond> i18nResponds = spec.getI18nList() == null ? List.of()
                : spec.getI18nList().stream()
                .map(item -> new SpecI18nPayloadRespond(item.getLocale(), item.getSpecName()))
                .toList();
        List<AdminSpecValueRespond> valueResponds = spec.getValues() == null ? List.of()
                : spec.getValues().stream().map(AdminSpecValueRespond::from).toList();
        return new AdminSpecRespond(
                spec.getId(),
                spec.getSpecCode(),
                spec.getSpecName(),
                spec.getSpecType(),
                spec.isRequired(),
                i18nResponds,
                valueResponds
        );
    }

    /**
     * 规格多语言响应 SpecI18nRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecI18nPayloadRespond {
        /**
         * 语言代码
         */
        private String locale;
        /**
         * 本地化规格名称
         */
        private String specName;
    }
}

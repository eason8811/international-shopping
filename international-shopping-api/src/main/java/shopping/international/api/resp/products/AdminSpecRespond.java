package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

/**
 * 规格响应 AdminSpecRespond
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminSpecRespond extends AbstractSpecRespond {
    /**
     * 规格多语言列表
     */
    private List<SpecI18nPayloadRespond> i18nList;

    /**
     * 构造一个 AdminSpecRespond 实例
     *
     * @param specId 规格 ID
     * @param specCode 规格代码
     * @param specName 规格名称
     * @param specType 规格类型, 可以是 {@code COLOR}, {@code SIZE}, {@code CAPACITY}, {@code MATERIAL} 或 {@code OTHER}
     * @param isRequired 是否为必填规格
     * @param values 规格值列表
     * @param i18nList 规格多语言信息列表
     */
    private AdminSpecRespond(Long specId, String specCode, String specName, SpecType specType, Boolean isRequired, List<? extends AbstractSpecValueRespond> values, List<SpecI18nPayloadRespond> i18nList) {
        super(specId, specCode, specName, specType, isRequired, values);
        this.i18nList = i18nList;
    }

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
                valueResponds,
                i18nResponds
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

package shopping.international.api.resp.products;

import lombok.*;
import lombok.experimental.SuperBuilder;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

/**
 * 规格响应 AdminSpecRespond
 */
@Data
@SuperBuilder
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
     * 规格多语言响应 SpecI18nRespond
     */
    @Data
    @Builder
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

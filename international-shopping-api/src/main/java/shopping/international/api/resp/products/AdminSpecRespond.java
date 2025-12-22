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
     * 排序值 (小在前)
     */
    private int sortOrder;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 规格值列表
     */
    private List<AdminSpecValueRespond> values;
    /**
     * 规格多语言列表
     */
    private List<SpecI18nPayloadRespond> i18nList;

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

package shopping.international.api.resp.products;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 规格值响应 AdminSpecValueRespond
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminSpecValueRespond extends AbstractSpecValueRespond {
    /**
     * 排序值 (小在前)
     */
    private int sortOrder;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 规格值多语言列表
     */
    private List<SpecValueI18nPayloadRespond> i18nList;

    /**
     * 规格值多语言响应 SpecValueI18nRespond
     */
    @Data
    @Builder
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

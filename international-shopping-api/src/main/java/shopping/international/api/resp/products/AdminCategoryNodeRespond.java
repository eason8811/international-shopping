package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类树响应节点
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminCategoryNodeRespond extends AbstractCategoryNodeRespond {
    /**
     * 全量 i18n 列表
     */
    private List<CategoryI18nPayloadRespond> i18nList;

    /**
     * 构造 <code>AdminCategoryNodeRespond</code> 对象
     *
     * @param id        分类 ID
     * @param parentId  父分类节点 ID
     * @param name      分类名称
     * @param slug      分类别名
     * @param level     分类层级
     * @param path      分类路径
     * @param sortOrder 分类排序
     * @param brand     品牌文案
     * @param children  子分类节点列表
     * @param isEnabled 是否启用
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param i18nList  全量 i18n 列表
     */
    private AdminCategoryNodeRespond(Long id, Long parentId, String name, String slug, Integer level, String path, Integer sortOrder, String brand, List<AdminCategoryNodeRespond> children, Boolean isEnabled, LocalDateTime createdAt, LocalDateTime updatedAt, List<CategoryI18nPayloadRespond> i18nList) {
        super(id, parentId, name, slug, level, path, sortOrder, brand, children, isEnabled, createdAt, updatedAt);
        this.i18nList = i18nList;
    }

    /**
     * 带 locale 的 i18n 响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryI18nPayloadRespond {
        /**
         * 语言代码
         */
        private String locale;
        /**
         * 分类名称
         */
        private String name;
        /**
         * 分类 slug
         */
        private String slug;
        /**
         * 品牌文案
         */
        private String brand;
    }
}

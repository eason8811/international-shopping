package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.vo.products.CategoryI18n;

import java.util.List;
import java.util.Map;

/**
 * 商品分类仓储接口
 *
 * <p>职责: 读取 product_category 及其 i18n 覆盖, 向领域层提供启用分类的读取能力</p>
 */
public interface IProductCategoryRepository {

    /**
     * 列出所有启用状态的分类
     *
     * @return 启用分类列表
     */
    @NotNull
    List<Category> listEnabledCategories();

    /**
     * 按 locale 读取所有分类的 i18n 覆盖
     *
     * @param locale 语言代码
     * @return key 为 categoryId 的覆盖 Map
     */
    @NotNull
    Map<Long, CategoryI18n> mapI18nByLocale(@NotNull String locale);
}

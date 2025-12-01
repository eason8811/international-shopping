package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.products.CategoryTreeNode;

import java.util.List;

/**
 * 商品分类查询服务
 */
public interface ICategoryQueryService {

    /**
     * 获取启用分类的树形结构
     *
     * @param locale 语言代码, 可空; 若提供则优先使用对应的 i18n 覆盖
     * @return 分类树 (根列表)
     */
    @NotNull
    List<CategoryTreeNode> tree(@Nullable String locale);
}

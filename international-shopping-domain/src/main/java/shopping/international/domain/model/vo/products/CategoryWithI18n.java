package shopping.international.domain.model.vo.products;

import lombok.Getter;
import shopping.international.domain.model.entity.products.Category;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 包含分类本体与多语言覆盖的视图 ( CategoryWithI18n )
 *
 * <p>用于管理端返回分类详情时携带全量的本地化信息</p>
 */
@Getter
public class CategoryWithI18n {
    /**
     * 分类实体
     */
    private final Category category;
    /**
     * 多语言覆盖列表
     */
    private final List<CategoryI18n> i18nList;

    /**
     * 创建包含多语言的分类视图
     *
     * @param category 分类实体
     * @param i18nList 多语言覆盖列表
     */
    public CategoryWithI18n(Category category, List<CategoryI18n> i18nList) {
        requireNotNull(category, "分类不能为空");
        this.category = category;
        this.i18nList = Collections.unmodifiableList(i18nList == null ? List.of() : i18nList);
    }

    /**
     * 获取展示用品牌文案
     *
     * @return 第一条非空品牌文案
     */
    public String resolveBrand() {
        return i18nList.stream()
                .map(CategoryI18n::getBrand)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}

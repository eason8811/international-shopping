package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductCategoryI18nPO;

import java.util.List;
import java.util.Map;

/**
 * Mapper: product_category_i18n
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductCategoryI18nMapper extends BaseMapper<ProductCategoryI18nPO> {

    /**
     * 查询同一 locale 下的 i18n slug 是否冲突
     *
     * @param pairs             locale/slug 对列表
     * @param excludeCategoryId 需要排除的分类 ID, 可空
     * @return 冲突的 locale, 不存在则返回 null
     */
    String selectConflictingLocaleBySlugInLocale(@Param("pairs") List<Map<String, String>> pairs,
                                                 @Param("excludeCategoryId") Long excludeCategoryId);
}

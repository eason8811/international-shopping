package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;

/**
 * Mapper: product_category
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategoryPO> {

    /**
     * 聚合读取分类（含多语言）
     *
     * @param categoryId 分类ID
     * @return 分类聚合
     */
    ProductCategoryPO selectWithI18nById(@Param("categoryId") Long categoryId);

    /**
     * 按条件聚合查询分类列表（含多语言）
     *
     * @param parentId        父分类ID, 可空
     * @param parentSpecified 是否过滤父分类
     * @param keyword         名称/slug 关键词, 可空
     * @param status          状态, 可空
     * @param offset          偏移量
     * @param limit           数量, 可空
     * @return 分类聚合列表
     */
    java.util.List<ProductCategoryPO> selectWithI18n(@Param("parentId") Long parentId,
                                                     @Param("parentSpecified") boolean parentSpecified,
                                                     @Param("keyword") String keyword,
                                                     @Param("status") String status,
                                                     @Param("offset") Integer offset,
                                                     @Param("limit") Integer limit);
}

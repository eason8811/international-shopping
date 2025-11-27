package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;

import java.util.List;

/**
 * Mapper: product_category
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategoryPO> {

    /**
     * 分页查询分类
     */
    List<ProductCategoryPO> selectAdminPage(@Param("filterByParent") boolean filterByParent,
                                            @Param("parentId") Long parentId,
                                            @Param("keyword") String keyword,
                                            @Param("status") String status,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    /**
     * 统计满足给定条件的分类总数
     *
     * @param filterByParent 是否根据父级进行过滤
     * @param parentId       父级分类 ID, 当 <code>filterByParent</code> 为 true 时生效
     * @param keyword        搜索关键词, 用于模糊匹配分类名称等
     * @param status         分类状态, 如 active inactive 等
     * @return 满足条件的分类数量
     */
    long countAdminPage(@Param("filterByParent") boolean filterByParent,
                        @Param("parentId") Long parentId,
                        @Param("keyword") String keyword,
                        @Param("status") String status);

    /**
     * 更新子孙分类的路径与层级
     */
    int updateDescendantsPath(@Param("categoryId") Long categoryId,
                              @Param("oldPrefix") String oldPrefix,
                              @Param("newPrefix") String newPrefix,
                              @Param("levelDelta") int levelDelta);
}

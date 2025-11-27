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
     * 统计分类总数
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

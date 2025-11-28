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
     * 分页查询分类, 用于后台管理页面展示数据
     *
     * @param filterByParent 是否根据父级进行过滤, 如果为 true, 则只返回指定 parentId 的直接子类
     * @param parentId       父级分类 ID, 当 <code>filterByParent</code> 为 true 时生效
     * @param keyword        搜索关键词, 用于模糊匹配分类名称等
     * @param status         分类状态, 如 "active", "inactive" 等
     * @param limit          每页显示的记录数
     * @param offset         偏移量, 从第几条记录开始读取
     * @return 返回符合条件的 ProductCategoryPO 对象列表, 包含了当前页的所有分类信息
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
     * 更新指定分类及其所有子孙分类的路径信息, 此方法适用于在调整分类结构时, 如移动或重命名分类后, 保持数据库中相关路径信息的一致性
     *
     * @param categoryId 需要更新路径信息的起始分类 ID
     * @param oldPrefix  当前路径前缀, 即需要被替换的路径部分
     * @param newPrefix  新路径前缀, 将用于替换旧前缀
     * @param levelDelta 层级变化量, 用于调整分类层级, 可正可负
     * @return 被成功更新的记录数量
     */
    int updateDescendantsPath(@Param("categoryId") Long categoryId,
                              @Param("oldPrefix") String oldPrefix,
                              @Param("newPrefix") String newPrefix,
                              @Param("levelDelta") int levelDelta);
}

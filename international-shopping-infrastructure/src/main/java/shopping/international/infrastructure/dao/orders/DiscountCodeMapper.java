package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.infrastructure.dao.orders.po.DiscountCodePO;

import java.util.List;

/**
 * Mapper: discount_code
 *
 * <p>继承 {@link BaseMapper}, 提供折扣码表的通用 CRUD 能力</p>
 */
@Mapper
public interface DiscountCodeMapper extends BaseMapper<DiscountCodePO> {

    /**
     * 分页查询折扣码列表 (管理端)
     * <p>特殊语义:</p>
     * <ul>
     *     <li>当 {@code criteria.permanent=true} 时, 忽略 expiresFrom/expiresTo</li>
     *     <li>当 {@code criteria.permanent=null} 且存在 expiresFrom/expiresTo 时, permanent=true 的数据仍视为符合时间条件</li>
     *     <li>只有显式指定 {@code criteria.permanent=false} 时, permanent=true 的数据才会在“过期时间条件”下被排除</li>
     * </ul>
     *
     * @param criteria 折扣码筛选条件, 包括但不限于折扣码文本过滤, 策略ID, 适用范围模式, 过期时间起止, 是否永久有效等
     * @param offset   查询结果的偏移量, 用于分页
     * @param limit    每页显示的结果数量
     * @return 符合条件的折扣码列表
     */
    @NotNull
    List<DiscountCodePO> pageCodes(@Param("criteria") @NotNull DiscountCodeSearchCriteria criteria,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计折扣码数量 (管理端)
     *
     * @param criteria 折扣码筛选条件, 包括但不限于折扣码文本过滤, 策略ID, 适用范围模式, 过期时间起止, 是否永久有效等
     * @return 符合条件的折扣码总数
     */
    long countCodes(@Param("criteria") @NotNull DiscountCodeSearchCriteria criteria);
}

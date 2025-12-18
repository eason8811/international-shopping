package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.orders.po.OrderDiscountAppliedPO;
import shopping.international.infrastructure.dao.orders.po.OrderDiscountAppliedViewPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper: order_discount_applied
 *
 * <p>职责:</p>
 * <ul>
 *     <li>提供 {@link BaseMapper} 通用 CRUD</li>
 *     <li>提供管理侧流水查询 (联表回填 order_no)</li>
 * </ul>
 */
@Mapper
public interface OrderDiscountAppliedMapper extends BaseMapper<OrderDiscountAppliedPO> {

    /**
     * 分页查询折扣实际使用流水 (联表 orders 回填 order_no)
     *
     * @param orderNo        订单号 (可为空)
     * @param discountCodeId 折扣码 ID (可为空)
     * @param appliedScope   应用范围 (可为空)
     * @param from           时间起 (可为空)
     * @param to             时间止 (可为空)
     * @param offset         偏移量
     * @param limit          单页数量
     * @return 视图列表
     */
    List<OrderDiscountAppliedViewPO> selectViews(@Param("orderNo") String orderNo,
                                                 @Param("discountCodeId") Long discountCodeId,
                                                 @Param("appliedScope") String appliedScope,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    /**
     * 统计折扣实际使用流水数量
     *
     * @param orderNo        订单号 (可为空)
     * @param discountCodeId 折扣码 ID (可为空)
     * @param appliedScope   应用范围 (可为空)
     * @param from           时间起 (可为空)
     * @param to             时间止 (可为空)
     * @return 总数
     */
    long countViews(@Param("orderNo") String orderNo,
                    @Param("discountCodeId") Long discountCodeId,
                    @Param("appliedScope") String appliedScope,
                    @Param("from") LocalDateTime from,
                    @Param("to") LocalDateTime to);
}


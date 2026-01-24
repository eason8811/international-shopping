package shopping.international.infrastructure.dao.payment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper: payment_refund
 *
 * <p>说明: 除通用 CRUD 外, 本 Mapper 提供管理侧联表查询 (payment_refund + orders + payment_order) 能力,
 * 并在详情查询中通过联表一次性返回退款明细, 避免在 for 循环中访问数据库</p>
 */
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefundPO> {

    /**
     * 管理侧分页查询退款单列表 (联表返回 order_no / channel)
     */
    List<PaymentRefundPO> pageAdminRefunds(@Param("orderNo") String orderNo,
                                           @Param("externalId") String externalId,
                                           @Param("channel") String channel,
                                           @Param("status") String status,
                                           @Param("createdFrom") LocalDateTime createdFrom,
                                           @Param("createdTo") LocalDateTime createdTo,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 管理侧统计退款单列表总数 (与 {@link #pageAdminRefunds(String, String, String, String, LocalDateTime, LocalDateTime, int, int)} 同条件)
     */
    long countAdminRefunds(@Param("orderNo") String orderNo,
                           @Param("externalId") String externalId,
                           @Param("channel") String channel,
                           @Param("status") String status,
                           @Param("createdFrom") LocalDateTime createdFrom,
                           @Param("createdTo") LocalDateTime createdTo);

    /**
     * 管理侧查询退款单详情 (联表返回 order_no / channel, 并联表一次性返回退款明细)
     *
     * @param refundId 退款单 ID
     * @return 详情 (含 items)
     */
    PaymentRefundPO selectDetailWithItems(@Param("refundId") Long refundId);
}


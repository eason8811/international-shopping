package shopping.international.infrastructure.dao.payment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.payment.po.PaymentOrderPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper: payment_order
 *
 * <p>说明: 除通用 CRUD 外, 本 Mapper 提供管理侧联表查询 (payment_order + orders)能力</p>
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderPO> {

    /**
     * 管理侧分页查询支付单列表 (联表返回 order_no)
     *
     * @param orderNo     订单号 (可空)
     * @param externalId  外部单号 (可空)
     * @param channel     支付通道 (可空)
     * @param status      支付单状态 (可空)
     * @param createdFrom 创建时间起 (可空)
     * @param createdTo   创建时间止 (可空)
     * @param offset      偏移量
     * @param limit       单页大小
     * @return 列表
     */
    List<PaymentOrderPO> pageAdminPayments(@Param("orderNo") String orderNo,
                                           @Param("externalId") String externalId,
                                           @Param("channel") String channel,
                                           @Param("status") String status,
                                           @Param("createdFrom") LocalDateTime createdFrom,
                                           @Param("createdTo") LocalDateTime createdTo,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 管理侧统计支付单列表总数 (与 {@link #pageAdminPayments(String, String, String, String, LocalDateTime, LocalDateTime, int, int)} 同条件)
     */
    long countAdminPayments(@Param("orderNo") String orderNo,
                            @Param("externalId") String externalId,
                            @Param("channel") String channel,
                            @Param("status") String status,
                            @Param("createdFrom") LocalDateTime createdFrom,
                            @Param("createdTo") LocalDateTime createdTo);

    /**
     * 管理侧查询支付单详情 (联表返回 order_no)
     *
     * @param paymentId 支付单 ID
     * @return 详情
     */
    PaymentOrderPO selectDetail(@Param("paymentId") Long paymentId);
}


package shopping.international.infrastructure.dao.payment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundItemPO;

/**
 * Mapper: payment_refund_item
 */
@Mapper
public interface PaymentRefundItemMapper extends BaseMapper<PaymentRefundItemPO> {
}


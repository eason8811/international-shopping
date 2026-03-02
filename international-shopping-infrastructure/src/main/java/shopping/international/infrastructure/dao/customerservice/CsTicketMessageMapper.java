package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketMessagePO;

/**
 * Mapper, cs_ticket_message 表
 */
@Mapper
public interface CsTicketMessageMapper extends BaseMapper<CsTicketMessagePO> {
}


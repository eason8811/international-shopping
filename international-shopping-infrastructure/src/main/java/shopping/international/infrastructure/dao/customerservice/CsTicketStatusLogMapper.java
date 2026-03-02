package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketStatusLogPO;

/**
 * Mapper, cs_ticket_status_log 表
 */
@Mapper
public interface CsTicketStatusLogMapper extends BaseMapper<CsTicketStatusLogPO> {
}

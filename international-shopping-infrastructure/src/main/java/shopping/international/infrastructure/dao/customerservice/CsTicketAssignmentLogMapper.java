package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketAssignmentLogPO;

/**
 * Mapper, cs_ticket_assignment_log 表
 */
@Mapper
public interface CsTicketAssignmentLogMapper extends BaseMapper<CsTicketAssignmentLogPO> {
}

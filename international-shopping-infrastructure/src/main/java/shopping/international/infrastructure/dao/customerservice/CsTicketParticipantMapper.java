package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketParticipantPO;

/**
 * Mapper, cs_ticket_participant 表
 */
@Mapper
public interface CsTicketParticipantMapper extends BaseMapper<CsTicketParticipantPO> {
}

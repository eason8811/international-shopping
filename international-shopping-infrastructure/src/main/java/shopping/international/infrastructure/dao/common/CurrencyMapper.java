package shopping.international.infrastructure.dao.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.common.po.CurrencyPO;

/**
 * Mapper: currency
 */
@Mapper
public interface CurrencyMapper extends BaseMapper<CurrencyPO> {
}


package shopping.international.infrastructure.dao.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.common.po.FxRatePO;

import java.util.List;

/**
 * Mapper: fx_rate
 */
@Mapper
public interface FxRateMapper extends BaseMapper<FxRatePO> {

    /**
     * 批量写入历史记录 (忽略唯一键冲突)
     *
     * @param list 要批量插入的历史汇率对象列表, 每个对象包含如基准币种代码, 报价币种代码, 汇率等信息
     * @return 返回受影响的行数, 表示成功插入的记录数量
     */
    int insertIgnoreBatch(@Param("list") List<FxRatePO> list);
}


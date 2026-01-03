package shopping.international.infrastructure.dao.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.common.po.FxRateLatestPO;

import java.util.List;

/**
 * 最新汇率快照 Mapper
 */
@Mapper
public interface FxRateLatestMapper extends BaseMapper<FxRateLatestPO> {

    /**
     * 按 (base, quote) 查询最新的汇率快照信息
     *
     * @param baseCode  基准币种代码 如 USD
     * @param quoteCode 报价币种代码 如 EUR
     * @return 返回最新汇率快照信息 如果没有找到符合条件的数据 则返回 null
     */
    FxRateLatestPO selectOneLatest(@Param("baseCode") String baseCode, @Param("quoteCode") String quoteCode);

    /**
     * 根据 base + quoteCodes 列表批量查询最新的汇率快照信息
     *
     * @param baseCode   基准币种代码 如 USD
     * @param quoteCodes 报价币种代码列表 如 [EUR, GBP]
     * @return 返回最新汇率快照信息的列表 如果没有找到符合条件的数据 则返回空列表
     */
    List<FxRateLatestPO> selectLatestByQuotes(@Param("baseCode") String baseCode, @Param("quoteCodes") List<String> quoteCodes);

    /**
     * 批量 upsert 最新汇率快照信息
     *
     * @param list 要批量 upsert 的最新汇率快照对象列表
     * @return 受影响的行数, 表示成功插入或更新的记录数量
     */
    int upsertBatch(@Param("list") List<FxRateLatestPO> list);
}


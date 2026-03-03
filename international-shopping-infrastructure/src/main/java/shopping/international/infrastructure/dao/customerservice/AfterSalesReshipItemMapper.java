package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipItemPO;

import java.util.List;

/**
 * Mapper, aftersales_reship_item 表
 */
@Mapper
public interface AfterSalesReshipItemMapper extends BaseMapper<AfterSalesReshipItemPO> {

    /**
     * 批量插入补发明细
     *
     * @param items 补发明细列表
     * @return 影响行数
     */
    int batchInsert(@Param("items") List<AfterSalesReshipItemPO> items);
}

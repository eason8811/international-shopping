package shopping.international.infrastructure.dao.orders;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.orders.po.SkuSaleSnapshotPO;

import java.util.List;

/**
 * Mapper: 订单域商品快照查询
 *
 * <p>用于订单/购物车等场景读取 SKU 的价格、库存与标题快照</p>
 */
@Mapper
public interface OrderProductMapper {

    /**
     * 批量查询 SKU 可售快照
     *
     * @param skuIds   SKU ID 列表
     * @param locale   语言 (可为空)
     * @param currency 币种 (可为空)
     * @return 快照列表
     */
    List<SkuSaleSnapshotPO> selectSkuSaleSnapshots(@Param("skuIds") List<Long> skuIds,
                                                   @Param("locale") String locale,
                                                   @Param("currency") String currency);
}


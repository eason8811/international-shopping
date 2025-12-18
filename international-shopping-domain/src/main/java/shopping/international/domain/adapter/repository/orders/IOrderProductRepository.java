package shopping.international.domain.adapter.repository.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.service.orders.IOrderService;

import java.util.List;

/**
 * 订单域“商品快照”查询仓储接口
 *
 * <p>用于订单下单/试算/购物车展示等场景, 以 SKU 为输入读取:</p>
 * <ul>
 *     <li>商品/多语言标题快照</li>
 *     <li>SKU 属性快照</li>
 *     <li>价格 (按币种)</li>
 *     <li>库存 (用于校验)</li>
 * </ul>
 *
 * <p><b>说明:</b> 该接口是订单域的只读查询模型, 不替代商品域聚合仓储</p>
 */
public interface IOrderProductRepository {
    /**
     * 按 SKU ID 批量查询 SKU 可售快照
     *
     * @param skuIds   SKU ID 列表
     * @param locale   语言代码, 可为空
     * @param currency 币种代码, 可为空
     * @return 快照列表 (与 skuIds 不保证同序, 调用方应按 skuId 映射)
     */
    @NotNull
    List<IOrderService.SkuSaleSnapshot> listSkuSaleSnapshots(@NotNull List<Long> skuIds, @Nullable String locale, @Nullable String currency);
}

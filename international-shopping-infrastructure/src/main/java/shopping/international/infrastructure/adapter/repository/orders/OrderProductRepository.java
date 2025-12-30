package shopping.international.infrastructure.adapter.repository.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.orders.IOrderProductRepository;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.infrastructure.dao.orders.OrderProductMapper;
import shopping.international.infrastructure.dao.orders.po.SkuSaleSnapshotPO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 订单域商品快照查询仓储实现
 */
@Repository
@RequiredArgsConstructor
public class OrderProductRepository implements IOrderProductRepository {

    /**
     * 商品快照查询 Mapper
     */
    private final OrderProductMapper orderProductMapper;
    /**
     * JSON 序列化/反序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 按 SKU ID 批量查询 SKU 可售快照
     *
     * @param skuIds   SKU ID 列表
     * @param locale   语言代码
     * @param currency 币种代码
     * @return 快照列表
     */
    @Override
    public @NotNull List<IOrderService.SkuSaleSnapshot> listSkuSaleSnapshots(@NotNull List<Long> skuIds,
                                                                             @Nullable String locale,
                                                                             @Nullable String currency) {
        if (skuIds.isEmpty())
            return List.of();
        List<SkuSaleSnapshotPO> pos = orderProductMapper.selectSkuSaleSnapshots(skuIds, locale, currency);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream()
                .map(po -> new IOrderService.SkuSaleSnapshot(
                        po.getSkuId(),
                        po.getProductId(),
                        po.getTitle(),
                        po.getCoverImageUrl(),
                        parseSkuAttrs(po.getSkuAttrsJson()),
                        po.getUnitPrice(),
                        po.getStock()
                ))
                .toList();
    }

    /**
     * 解析 SKU 属性 JSON
     *
     * @param json JSON 文本
     * @return Map 或 null
     */
    private Map<String, Object> parseSkuAttrs(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }
}

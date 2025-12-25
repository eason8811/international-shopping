package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 领域服务接口
 *
 * <p>聚焦 SKU 的查询、增删改、价格/规格绑定与库存调整等用例编排。</p>
 */
public interface ISkuService {

    /**
     * 列出商品下的 SKU
     *
     * @param productId 商品 ID
     * @param status    状态过滤, 为空表示不过滤
     * @return SKU 聚合列表
     */
    @NotNull
    List<Sku> list(@NotNull Long productId, @Nullable SkuStatus status);

    /**
     * 创建 SKU
     *
     * @param productId 所属商品 ID
     * @param skuCode   SKU 编码
     * @param stock     库存
     * @param weight    重量
     * @param status    状态
     * @param isDefault 是否默认
     * @param barcode   条码
     * @param prices    价格列表
     * @param specs     规格绑定
     * @param images    图库
     * @return 新建的聚合
     */
    @NotNull
    Sku create(@NotNull Long productId, @Nullable String skuCode, @NotNull Integer stock,
               @Nullable BigDecimal weight, @NotNull SkuStatus status, boolean isDefault,
               @Nullable String barcode, @NotNull List<ProductPrice> prices,
               @NotNull List<SkuSpecRelation> specs, @NotNull List<ProductImage> images);

    /**
     * 增量更新 SKU 基础字段
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param skuCode   新编码, 可空
     * @param stock     新库存, 可空
     * @param weight    新重量, 可空
     * @param status    新状态, 可空
     * @param isDefault 默认标记, 可空
     * @param barcode   条码, 可空
     * @param images    新图库, 可空表示不改
     * @return 更新后的聚合
     */
    @NotNull
    Sku updateBasic(@NotNull Long productId, @NotNull Long skuId, @Nullable String skuCode,
                    @Nullable Integer stock, @Nullable BigDecimal weight, @Nullable SkuStatus status,
                    @Nullable Boolean isDefault, @Nullable String barcode, @Nullable List<ProductImage> images);

    /**
     * 增量 upsert 规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specs     规格绑定列表
     * @return 受影响的规格 ID
     */
    @NotNull
    List<Long> upsertSpecs(@NotNull Long productId, @NotNull Long skuId, @NotNull List<SkuSpecRelation> specs);

    /**
     * 解除规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    boolean deleteSpec(@NotNull Long productId, @NotNull Long skuId, @NotNull Long specId);

    /**
     * 增量 upsert 价格
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param prices    价格列表
     * @return 受影响的币种
     */
    @NotNull
    List<String> upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPrice> prices);

    /**
     * 调整库存
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param mode      调整模式
     * @param quantity  数量
     * @return 调整后的库存
     */
    int adjustStock(@NotNull Long productId, @NotNull Long skuId,
                    @NotNull StockAdjustMode mode, int quantity);

    /**
     * 删除指定商品下的 SKU
     *
     * @param productId 所属商品 ID
     * @param skuId     要删除的 SKU ID
     * @return 如果删除成功返回 <code>true</code>, 否则返回 <code>false</code>
     */
    boolean delete(Long productId, Long skuId);
}

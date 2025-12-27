package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;

import java.util.List;
import java.util.Optional;

/**
 * SKU 聚合仓储接口
 *
 * <p>负责从持久化层读写 {@link Sku} 聚合及其价格, 规格绑定, 图片等关联数据, 为领域服务提供一致的聚合装配能力</p>
 */
public interface ISkuRepository {

    /**
     * 按主键查询 SKU 聚合
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @return 聚合快照, 不存在则返回空
     */
    @NotNull
    Optional<Sku> findById(@NotNull Long productId, @NotNull Long skuId);

    /**
     * 按商品查询 SKU 列表
     *
     * @param productId 所属商品 ID
     * @param status    状态过滤, 为空则不过滤
     * @return SKU 聚合列表
     */
    @NotNull
    List<Sku> listByProductId(@NotNull Long productId, @Nullable SkuStatus status);

    /**
     * 新增 SKU 聚合
     *
     * @param sku 待保存的聚合, ID 为空
     * @return 携带持久化 ID 的聚合
     */
    @NotNull
    Sku save(@NotNull Sku sku);

    /**
     * 更新 SKU 基础字段及可选图库
     *
     * @param sku           聚合快照
     * @param replaceImages 是否替换图库
     * @return 更新后的聚合
     */
    @NotNull
    Sku updateBasic(@NotNull Sku sku, boolean replaceImages);

    /**
     * 按规格进行增量 upsert
     *
     * @param skuId     SKU ID
     * @param relations 规格绑定列表
     * @return 受影响的规格 ID 列表
     */
    @NotNull
    List<Long> upsertSpecs(@NotNull Long skuId, @NotNull List<SkuSpecRelation> relations);

    /**
     * 删除指定规格绑定
     *
     * @param skuId  SKU ID
     * @param specId 规格 ID
     * @return 是否删除成功
     */
    boolean deleteSpec(@NotNull Long skuId, @NotNull Long specId);

    /**
     * 按币种增量 upsert 价格
     *
     * @param skuId  SKU ID
     * @param prices 价格列表
     */
    void upsertPrices(@NotNull Long skuId, @NotNull List<ProductPrice> prices);

    /**
     * 覆盖更新库存
     *
     * @param skuId SKU ID
     * @param stock 新库存
     * @return 更新后的库存值
     */
    int updateStock(@NotNull Long skuId, int stock);

    /**
     * 删除指定商品下的特定 SKU 聚合
     *
     * @param productId 所属商品 ID
     * @param skuId     待删除的 SKU ID
     * @return 是否删除成功
     */
    boolean delete(Long productId, Long skuId);

    /**
     * 统一设置默认 SKU
     *
     * @param productId 商品 ID
     * @param skuId     目标默认 SKU, 为空表示清空默认
     */
    void markDefault(@NotNull Long productId, @Nullable Long skuId);

    /**
     * 汇总某商品下的库存总和
     *
     * @param productId 商品 ID
     * @return 库存合计
     */
    int sumStockByProduct(@NotNull Long productId);

    /**
     * 批量更新某商品下所有 SKU 的状态
     *
     * <p>典型用例: 商品下架/删除时同步禁用全部 SKU, 以避免“仅校验 SKU 状态”的下单窗口。</p>
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 受影响行数
     */
    int updateStatusByProductId(@NotNull Long productId, @NotNull SkuStatus status);

    /**
     * 判断某商品下是否存在指定状态的 SKU
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 是否存在
     */
    boolean existsByProductIdAndStatus(@NotNull Long productId, @NotNull SkuStatus status);
}

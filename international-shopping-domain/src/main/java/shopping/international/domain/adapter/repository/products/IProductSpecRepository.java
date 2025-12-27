package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;

import java.util.List;
import java.util.Optional;

/**
 * 商品规格聚合的仓储接口
 *
 * <p>负责对 {@code product_spec} 及其取值、多语言表的组合读写, 并提供 SKU 绑定校验等辅助能力。</p>
 */
public interface IProductSpecRepository {

    /**
     * 按 ID 查询规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格实体, 不存在返回空
     */
    @NotNull
    Optional<ProductSpec> findById(@NotNull Long productId, @NotNull Long specId);

    /**
     * 查询商品下的全部规格 (含规格值与多语言)
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    @NotNull
    List<ProductSpec> listByProductId(@NotNull Long productId);

    /**
     * 新增规格及可选的规格值、多语言
     *
     * @param spec 规格实体, ID 为空
     * @return 携带持久化 ID 的规格实体
     */
    @NotNull
    ProductSpec save(@NotNull ProductSpec spec);

    /**
     * 更新规格基础信息
     *
     * @param spec        规格实体
     * @param replaceI18n 是否覆盖 i18n 表
     * @return 更新后的规格实体
     */
    @NotNull
    ProductSpec update(@NotNull ProductSpec spec, boolean replaceI18n);

    /**
     * 删除规格及其规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    boolean deleteSpec(@NotNull Long productId, @NotNull Long specId);

    /**
     * 查询指定规格下的规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listValues(@NotNull Long productId, @NotNull Long specId);

    /**
     * 新增规格值
     *
     * @param value 规格值实体, ID 为空
     * @return 保存后的规格值
     */
    @NotNull
    ProductSpecValue saveValue(@NotNull ProductSpecValue value);

    /**
     * 更新规格值
     *
     * @param value       规格值实体
     * @param replaceI18n 是否覆盖 i18n 表
     * @return 更新后的规格值
     */
    @NotNull
    ProductSpecValue updateValue(@NotNull ProductSpecValue value, boolean replaceI18n);

    /**
     * 删除规格值
     *
     * @param specId  规格 ID
     * @param valueId 规格值 ID
     * @return 是否删除成功
     */
    boolean deleteValue(@NotNull Long specId, @NotNull Long valueId);

    /**
     * 判断规格是否被 SKU 绑定
     *
     * @param specId 规格 ID
     * @return 若有绑定则返回 {@code true}
     */
    boolean hasSkuBindingsForSpec(@NotNull Long specId);

    /**
     * 判断规格值是否被 SKU 绑定
     *
     * @param valueId 规格值 ID
     * @return 若有绑定则返回 {@code true}
     */
    boolean hasSkuBindingsForValue(@NotNull Long valueId);
}

package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;

import java.util.List;
import java.util.Map;

/**
 * 商品规格领域服务接口
 *
 * <p>负责管理侧规格与规格值的增删改查编排, 包含 SKU 绑定校验。</p>
 */
public interface IProductSpecService {

    /**
     * 列出商品下全部规格
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    @NotNull
    List<ProductSpec> list(@NotNull Long productId);

    /**
     * 创建规格
     *
     * @param productId 商品 ID
     * @param spec      规格实体
     * @return 新增的规格 ID
     */
    @NotNull
    Long create(@NotNull Long productId, @NotNull ProductSpec spec);

    /**
     * 更新规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param specCode  新规格代码, 可空
     * @param specName  新规格名, 可空
     * @param specType  新规格类型, 可空
     * @param required  是否必选, 可空
     * @param sortOrder 排序, 可空
     * @param enabled   是否启用, 可空
     * @param i18nList  多语言列表, 可空
     * @return 规格 ID
     */
    @NotNull
    Long update(@NotNull Long productId, @NotNull Long specId, @Nullable String specCode, @Nullable String specName,
                @Nullable SpecType specType, @Nullable Boolean required, @Nullable Integer sortOrder,
                @Nullable Boolean enabled, @Nullable List<ProductSpecI18n> i18nList);

    /**
     * 删除规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    boolean delete(@NotNull Long productId, @NotNull Long specId);

    /**
     * 列出规格取值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @NotNull
    List<ProductSpecValue> listValues(@NotNull Long productId, @NotNull Long specId);

    /**
     * 创建规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param value     规格值
     * @return 新增的规格值 ID
     */
    @NotNull
    Long createValue(@NotNull Long productId, @NotNull Long specId, @NotNull ProductSpecValue value);

    /**
     * 更新规格值
     *
     * @param productId  商品 ID
     * @param specId     规格 ID
     * @param valueId    规格值 ID
     * @param valueCode  新编码, 可空
     * @param valueName  新名称, 可空
     * @param attributes 新属性, 可空
     * @param sortOrder  新排序, 可空
     * @param enabled    是否启用, 可空
     * @param i18nList   多语言列表, 可空
     * @return 规格值 ID
     */
    @NotNull
    Long updateValue(@NotNull Long productId, @NotNull Long specId, @NotNull Long valueId,
                     @Nullable String valueCode, @Nullable String valueName, @Nullable Map<String, Object> attributes,
                     @Nullable Integer sortOrder, @Nullable Boolean enabled,
                     @Nullable List<ProductSpecValueI18n> i18nList);

    /**
     * 删除规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param valueId   规格值 ID
     * @return 是否删除成功
     */
    boolean deleteValue(@NotNull Long productId, @NotNull Long specId, @NotNull Long valueId);
}

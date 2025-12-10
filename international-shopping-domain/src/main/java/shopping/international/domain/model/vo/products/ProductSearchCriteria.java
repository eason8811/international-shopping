package shopping.international.domain.model.vo.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户侧商品检索条件
 *
 * <p>用于向领域/仓储层传递已规范化的过滤参数</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCriteria implements Verifiable {
    /**
     * 语言代码
     */
    @NotNull
    private String locale;
    /**
     * 价格币种
     */
    @NotNull
    private String currency;
    /**
     * 分类 slug, 可空
     */
    @Nullable
    private String categorySlug;
    /**
     * 关键词, 可空
     */
    @Nullable
    private String keyword;
    /**
     * 标签过滤列表, 允许为空列表
     */
    @NotNull
    private List<String> tags = Collections.emptyList();
    /**
     * 价格下限, 可空
     */
    @Nullable
    private BigDecimal priceMin;
    /**
     * 价格上限, 可空
     */
    @Nullable
    private BigDecimal priceMax;
    /**
     * 排序方式
     */
    @NotNull
    private ProductSort sort;

    /**
     * 基础校验, 确保必要字段存在
     */
    @Override
    public void validate() {
        requireNotNull(locale, "locale 不能为空");
        requireNotNull(currency, "currency 不能为空");
        requireNotNull(sort, "排序方式不能为空");
    }
}

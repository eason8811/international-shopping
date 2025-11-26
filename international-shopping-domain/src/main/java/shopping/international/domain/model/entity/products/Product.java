package shopping.international.domain.model.entity.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品实体 (SPU)
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Product {
    /**
     * 商品ID
     */
    private Long id;
    /**
     * slug (路由/SEO)
     */
    private String slug;
    /**
     * 标题
     */
    private String title;
    /**
     * 副标题
     */
    private String subtitle;
    /**
     * 描述
     */
    private String description;
    /**
     * 所属分类ID
     */
    private Long categoryId;
    /**
     * 品牌
     */
    private String brand;
    /**
     * 主图
     */
    private String coverImageUrl;
    /**
     * 库存总数
     */
    private int stockTotal;
    /**
     * 销量
     */
    private int saleCount;
    /**
     * SKU 类型
     */
    private SkuType skuType;
    /**
     * 状态
     */
    private ProductStatus status;
    /**
     * 默认 SKU
     */
    private Long defaultSkuId;
    /**
     * 标签 (JSON 数组解析后)
     */
    private List<String> tags;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    private Product() {
    }

    /**
     * 从持久层数据重建商品实体
     *
     * @param id            主键
     * @param slug          slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类ID
     * @param brand         品牌
     * @param coverImageUrl 主图
     * @param stockTotal    库存
     * @param saleCount     销量
     * @param skuType       SKU 类型
     * @param status        状态
     * @param defaultSkuId  默认SKU
     * @param tags          标签
     * @param updatedAt     更新时间
     * @return 商品实体
     */
    public static Product reconstitute(Long id,
                                       @NotNull String slug,
                                       @NotNull String title,
                                       String subtitle,
                                       String description,
                                       @NotNull Long categoryId,
                                       String brand,
                                       String coverImageUrl,
                                       Integer stockTotal,
                                       Integer saleCount,
                                       SkuType skuType,
                                       ProductStatus status,
                                       Long defaultSkuId,
                                       List<String> tags,
                                       LocalDateTime updatedAt) {
        requireNotBlank(slug, "商品 slug 不能为空");
        requireNotBlank(title, "商品标题不能为空");

        Product product = new Product();
        product.id = id;
        product.slug = slug;
        product.title = title;
        product.subtitle = subtitle;
        product.description = description;
        product.categoryId = categoryId;
        product.brand = brand;
        product.coverImageUrl = coverImageUrl;
        product.stockTotal = stockTotal == null ? 0 : stockTotal;
        product.saleCount = saleCount == null ? 0 : saleCount;
        product.skuType = skuType == null ? SkuType.SINGLE : skuType;
        product.status = status == null ? ProductStatus.DRAFT : status;
        product.defaultSkuId = defaultSkuId;
        product.tags = tags == null ? Collections.emptyList() : new ArrayList<>(tags);
        product.updatedAt = updatedAt;
        return product;
    }

    /**
     * 是否上架
     *
     * @return true 表示上架
     */
    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }
}

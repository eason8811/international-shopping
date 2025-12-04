package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.util.List;

/**
 * AbstractProductDetailRespond 用于封装商品详细信息的抽象响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractProductDetailRespond {
    /**
     * 商品 ID
     */
    private Long id;
    /**
     * 商品 slug
     */
    private String slug;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品副标题
     */
    private String subtitle;
    /**
     * 商品描述
     */
    private String description;
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 分类 slug
     */
    private String categorySlug;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 封面图地址
     */
    private String coverImageUrl;
    /**
     * 聚合库存
     */
    private Integer stockTotal;
    /**
     * 销量
     */
    private Integer saleCount;
    /**
     * SKU 类型
     */
    private SkuType skuType;
    /**
     * 商品状态
     */
    private ProductStatus status;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 默认 SKU ID
     */
    private Long defaultSkuId;
    /**
     * 商品图片
     */
    private List<ProductImageRespond> gallery;
    /**
     * 规格列表
     */
    private List<? extends AbstractSpecRespond> specs;
    /**
     * SKU 列表
     */
    private List<ProductSkuRespond> skus;
}

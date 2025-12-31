package shopping.international.infrastructure.dao.products.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧商品快照持久化载体
 *
 * <p>承载列表查询时的本地化字段与价格区间聚合</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProductSnapshotPO {

    /**
     * 商品 ID
     */
    private Long id;
    /**
     * 商品 slug(已按 locale 覆盖)
     */
    private String slug;
    /**
     * 商品标题(已按 locale 覆盖)
     */
    private String title;
    /**
     * 商品副标题(已按 locale 覆盖)
     */
    private String subtitle;
    /**
     * 商品描述(已按 locale 覆盖)
     */
    private String description;
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 分类 slug(已按 locale 覆盖)
     */
    private String categorySlug;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 封面图
     */
    private String coverImageUrl;
    /**
     * 聚合库存
     */
    private Integer stockTotal;
    /**
     * 聚合销量
     */
    private Integer saleCount;
    /**
     * SKU 类型
     */
    private String skuType;
    /**
     * 商品状态
     */
    private String status;
    /**
     * 标签 JSON
     */
    private String tags;
    /**
     * 标价最小值（最小货币单位）
     */
    private Long listPriceMin;
    /**
     * 标价最大值（最小货币单位）
     */
    private Long listPriceMax;
    /**
     * 促销价最小值（最小货币单位）
     */
    private Long salePriceMin;
    /**
     * 促销价最大值（最小货币单位）
     */
    private Long salePriceMax;
    /**
     * 商品图库
     */
    private List<ProductImagePO> gallery;
    /**
     * 点赞时间
     */
    private LocalDateTime likedAt;
}

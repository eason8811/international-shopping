package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.products.ProductImage;

/**
 * 商品图片响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRespond {
    /**
     * 图片 URL
     */
    private String url;
    /**
     * 是否主图
     */
    private Boolean isMain;
    /**
     * 图片排序
     */
    private Integer sortOrder;

    /**
     * 从 {@code ProductImage} 对象创建一个新的 {@code ProductImageRespond} 实例
     *
     * @param image 商品图片对象, 包含图片 URL, 是否主图, 以及排序信息
     * @return 新的 {@code ProductImageRespond} 实例, 其中包含了传入 {@code ProductImage} 对象中的相关信息
     */
    public static ProductImageRespond from(ProductImage image) {
        return new ProductImageRespond(image.getUrl(), image.isMain(), image.getSortOrder());
    }
}

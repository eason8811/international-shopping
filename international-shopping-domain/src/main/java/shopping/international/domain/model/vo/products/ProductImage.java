package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品图片值对象, 适用于 SPU 与 SKU 图库 ({@code product_image}/{@code product_sku_image}).
 */
@Getter
@ToString
@EqualsAndHashCode(of = "url")
public class ProductImage implements Verifiable {
    /**
     * 图片 URL
     */
    private final String url;
    /**
     * 是否主图
     */
    private final boolean main;
    /**
     * 排序 (小在前)
     */
    private final int sortOrder;

    /**
     * 构造函数
     *
     * @param url       图片地址
     * @param main      是否主图
     * @param sortOrder 排序值
     */
    private ProductImage(String url, boolean main, int sortOrder) {
        this.url = url;
        this.main = main;
        this.sortOrder = sortOrder;
    }

    /**
     * 创建图片值对象
     *
     * @param url       图片地址, 必填
     * @param main      是否主图
     * @param sortOrder 排序值
     * @return 规范化后的 {@link ProductImage}
     */
    public static ProductImage of(String url, boolean main, int sortOrder) {
        requireNotBlank(url, "图片 URL 不能为空");
        return new ProductImage(url.strip(), main, sortOrder);
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotBlank(url, "图片 URL 不能为空");
    }
}

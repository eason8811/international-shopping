package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品图片
 */
@Getter
@ToString
@EqualsAndHashCode(of = "url")
public class ProductImage {
    /**
     * 图片地址
     */
    private final String url;
    /**
     * 是否主图
     */
    private final boolean main;
    /**
     * 排序
     */
    private final int sortOrder;

    private ProductImage(String url, boolean main, int sortOrder) {
        this.url = url;
        this.main = main;
        this.sortOrder = sortOrder;
    }

    /**
     * 构建图片 VO
     *
     * @param url       图片地址
     * @param isMain    是否主图
     * @param sortOrder 排序
     * @return 图片 VO
     */
    public static ProductImage of(@NotNull String url, boolean isMain, Integer sortOrder) {
        requireNotBlank(url, "图片地址不能为空");
        return new ProductImage(url, isMain, sortOrder == null ? 0 : sortOrder);
    }
}

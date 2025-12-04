package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requirePatchField;

/**
 * 商品图片请求载荷 (ProductImagePayload)
 */
@Data
public class ProductImagePayload implements Verifiable {
    /**
     * 图片 URL, 长度不超过 500
     */
    private String url;
    /**
     * 是否主图
     */
    @Nullable
    private Boolean isMain;
    /**
     * 排序权重, 默认 0
     */
    @Nullable
    private Integer sortOrder;

    /**
     * 校验并规范化图片字段
     *
     * @throws IllegalParamException 当 URL 为空或超长时抛出
     */
    public void validate() {
        url = requirePatchField(url, "图片 URL 不能为空", url -> url.length() <= 500, "图片 URL 长度不能超过 500 个字符");
        isMain = isMain != null && isMain;
        sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}

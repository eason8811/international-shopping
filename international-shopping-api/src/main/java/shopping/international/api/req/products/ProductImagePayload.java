package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

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
     * 验证并规范化当前对象的字段
     *
     * <p>此方法用于确保 {@code isMain} 字段为布尔值, 如果其为空则设置为 false; 同时确保 {@code sortOrder} 字段存在一个默认值 0</p>
     */
    public void validate() {
        isMain = isMain != null && isMain;
        sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void createValidate() {
        url = normalizeNotNullField(url, "图片 URL 不能为空", url -> url.length() <= 500, "图片 URL 长度不能超过 500 个字符");
        validate();
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void updateValidate() {
        url = normalizeNullableField(url, "图片 URL 不能为空", url -> url.length() <= 500, "图片 URL 长度不能超过 500 个字符");
        validate();
    }
}

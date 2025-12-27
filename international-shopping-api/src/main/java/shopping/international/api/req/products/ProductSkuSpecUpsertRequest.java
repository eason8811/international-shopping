package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * SKU 规格绑定请求 ProductSkuSpecUpsertRequest
 */
@Data
public class ProductSkuSpecUpsertRequest implements Verifiable {
    /**
     * 规格 ID, 推荐传递确保精确绑定
     */
    private Long specId;
    /**
     * 规格编码
     */
    @Nullable
    private String specCode;
    /**
     * 规格名称, 用于冗余提示
     */
    @Nullable
    private String specName;
    /**
     * 规格值 ID
     */
    private Long valueId;
    /**
     * 规格值编码
     */
    @Nullable
    private String valueCode;
    /**
     * 规格值名称, 用于冗余提示
     */
    @Nullable
    private String valueName;

    /**
     * 校验 SKU 规格绑定字段
     *
     * @throws IllegalParamException 当规格或规格值信息缺失或长度超限时抛出 IllegalParamException
     */
    public void validate() {
        requireNotNull(specId, "规格 ID 不能为空");
        require(specId > 0, "规格 ID 非法");
        specCode = normalizeNullableField(specCode, "规格编码不能为空", s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        specName = normalizeNullableField(specName, "规格名称不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        valueCode = normalizeNullableField(valueCode, "规格值编码不能为空", s -> s.length() <= 64, "规格值编码长度不能超过 64 个字符");
        valueName = normalizeNullableField(valueName, "规格值名称不能为空", s -> s.length() <= 64, "规格值名称长度不能超过 64 个字符");
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void createValidate() {
        validate();
        requireNotNull(valueId, "规格值 ID 不能为空");
        require(valueId > 0, "规格值 ID 非法");
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void updateValidate() {
        validate();
        if (valueId != null)
            require(valueId > 0, "规格值 ID 非法");
    }
}

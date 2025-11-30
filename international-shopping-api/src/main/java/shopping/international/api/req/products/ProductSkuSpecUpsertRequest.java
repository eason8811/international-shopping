package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 规格绑定请求 ProductSkuSpecUpsertRequest
 */
@Data
public class ProductSkuSpecUpsertRequest {
    /**
     * 规格 ID, 推荐传递确保精确绑定
     */
    @Nullable
    private Long specId;
    /**
     * 规格编码
     */
    private String specCode;
    /**
     * 规格名称, 用于冗余提示
     */
    @Nullable
    private String specName;
    /**
     * 规格值 ID
     */
    @Nullable
    private Long valueId;
    /**
     * 规格值编码
     */
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
        if (specId != null && specId <= 0)
            throw new IllegalParamException("规格 ID 非法");
        if (valueId != null && valueId <= 0)
            throw new IllegalParamException("规格值 ID 非法");

        requireNotBlank(specCode, "规格编码不能为空");
        specCode = specCode.strip();
        if (specCode.length() > 64)
            throw new IllegalParamException("规格编码长度不能超过 64 个字符");

        if (specName != null) {
            specName = specName.strip();
            if (specName.length() > 64)
                throw new IllegalParamException("规格名称长度不能超过 64 个字符");
        }

        requireNotBlank(valueCode, "规格值编码不能为空");
        valueCode = valueCode.strip();
        if (valueCode.length() > 64)
            throw new IllegalParamException("规格值编码长度不能超过 64 个字符");

        if (valueName != null) {
            valueName = valueName.strip();
            if (valueName.length() > 64)
                throw new IllegalParamException("规格值名称长度不能超过 64 个字符");
        }
    }
}

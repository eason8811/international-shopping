package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * SKU 规格绑定请求 ProductSkuSpecUpsertRequest
 */
@Data
public class ProductSkuSpecUpsertRequest {
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
        requireNotNull(valueId, "规格值 ID 不能为空");
        require(valueId > 0, "规格值 ID 非法");
        requireNotBlank(specCode, "规格编码不能为空");
        requirePatchField(specCode, "specCode 不能为空", s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        requirePatchField(specName, "specName 不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        requirePatchField(valueCode, "规格值编码不能为空", s -> s.length() <= 64, "规格值编码长度不能超过 64 个字符");
        requirePatchField(valueName, "规格值名称不能为空", s -> s.length() <= 64, "规格值名称长度不能超过 64 个字符");
    }
}

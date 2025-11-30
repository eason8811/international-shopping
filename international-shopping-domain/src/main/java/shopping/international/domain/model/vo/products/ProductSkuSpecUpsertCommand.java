package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 规格绑定命令 ProductSkuSpecUpsertCommand
 *
 * <p>表示单个 SKU 与规格值的绑定关系, 用于确保每个规格类别仅绑定一个值</p>
 */
@Getter
@ToString
public class ProductSkuSpecUpsertCommand {
    /**
     * 规格 ID
     */
    @Nullable
    private final Long specId;
    /**
     * 规格编码
     */
    private final String specCode;
    /**
     * 规格值 ID
     */
    @Nullable
    private final Long valueId;
    /**
     * 规格值编码
     */
    private final String valueCode;

    private ProductSkuSpecUpsertCommand(@Nullable Long specId, String specCode, @Nullable Long valueId, String valueCode) {
        this.specId = specId;
        this.specCode = specCode;
        this.valueId = valueId;
        this.valueCode = valueCode;
    }

    /**
     * 构建 SKU 规格绑定命令
     *
     * @param specId    规格 ID
     * @param specCode  规格编码
     * @param valueId   规格值 ID
     * @param valueCode 规格值编码
     * @return SKU 规格绑定命令
     * @throws IllegalParamException 当编码为空时抛出 IllegalParamException
     */
    public static ProductSkuSpecUpsertCommand of(@Nullable Long specId,
                                                 String specCode,
                                                 @Nullable Long valueId,
                                                 String valueCode) {
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(valueCode, "规格值编码不能为空");
        return new ProductSkuSpecUpsertCommand(specId, specCode, valueId, valueCode);
    }
}

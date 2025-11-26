package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 关联的 规格 和 规格值
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"specId", "valueId"})
public class ProductSkuSpec {
    /**
     * 规格类别 ID
     */
    private final Long specId;
    /**
     * 规格编码
     */
    private final String specCode;
    /**
     * 规格名
     */
    private final String specName;
    /**
     * 规格值 ID
     */
    private final Long valueId;
    /**
     * 规格值编码
     */
    private final String valueCode;
    /**
     * 规格值名
     */
    private final String valueName;

    private ProductSkuSpec(Long specId, String specCode, String specName, Long valueId, String valueCode, String valueName) {
        this.specId = specId;
        this.specCode = specCode;
        this.specName = specName;
        this.valueId = valueId;
        this.valueCode = valueCode;
        this.valueName = valueName;
    }

    /**
     * 构建 SKU 规格值
     *
     * @param specId    规格 ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param valueId   规格值 ID
     * @param valueCode 规格值编码
     * @param valueName 规格值名称
     * @return SKU 规格值
     */
    public static ProductSkuSpec of(@NotNull Long specId,
                                    @NotNull String specCode,
                                    @NotNull String specName,
                                    @NotNull Long valueId,
                                    @NotNull String valueCode,
                                    @NotNull String valueName) {
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        requireNotBlank(valueCode, "规格值编码不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
        return new ProductSkuSpec(specId, specCode, specName, valueId, valueCode, valueName);
    }
}

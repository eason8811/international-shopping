package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 所选规格-规格值关系值对象, 对应 {@code product_sku_spec}.
 *
 * <p>以规格编码/ID 与规格值编码/ID 组合为自然键。</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"specCode", "valueCode"})
public class SkuSpecRelation implements Verifiable {
    /**
     * 规格类别 ID (可空, 与 specCode 二选一)
     */
    private final Long specId;
    /**
     * 规格类别编码
     */
    private final String specCode;
    /**
     * 规格类别名称 (冗余快照)
     */
    private final String specName;
    /**
     * 规格值 ID (可空, 与 valueCode 二选一)
     */
    private final Long valueId;
    /**
     * 规格值编码
     */
    private final String valueCode;
    /**
     * 规格值名称 (冗余快照)
     */
    private final String valueName;

    /**
     * 构造函数
     *
     * @param specId    规格 ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param valueId   规格值 ID
     * @param valueCode 规格值编码
     * @param valueName 规格值名称
     */
    private SkuSpecRelation(Long specId, String specCode, String specName,
                            Long valueId, String valueCode, String valueName) {
        this.specId = specId;
        this.specCode = specCode;
        this.specName = specName;
        this.valueId = valueId;
        this.valueCode = valueCode;
        this.valueName = valueName;
    }

    /**
     * 创建 SKU 规格值选择
     *
     * @param specId    规格 ID, 可空 (与 specCode 二选一)
     * @param specCode  规格编码, 可空 (与 specId 二选一)
     * @param specName  规格名称快照, 可空
     * @param valueId   规格值 ID, 可空 (与 valueCode 二选一)
     * @param valueCode 规格值编码, 可空 (与 valueId 二选一)
     * @param valueName 规格值名称快照, 可空
     * @return 规范化后的 {@link SkuSpecRelation}
     */
    public static SkuSpecRelation of(Long specId, String specCode, String specName,
                                     Long valueId, String valueCode, String valueName) {
        require(specId != null || specCode != null, "规格类别必须提供 ID 或编码");
        require(valueId != null || valueCode != null, "规格值必须提供 ID 或编码");
        if (specCode != null)
            requireNotBlank(specCode, "规格编码不能为空");
        if (valueCode != null)
            requireNotBlank(valueCode, "规格值编码不能为空");
        return new SkuSpecRelation(specId, specCode == null ? null : specCode.strip(),
                specName == null ? null : specName.strip(),
                valueId, valueCode == null ? null : valueCode.strip(),
                valueName == null ? null : valueName.strip());
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        require(specId != null || specCode != null, "规格类别必须提供 ID 或编码");
        require(valueId != null || valueCode != null, "规格值必须提供 ID 或编码");
    }
}

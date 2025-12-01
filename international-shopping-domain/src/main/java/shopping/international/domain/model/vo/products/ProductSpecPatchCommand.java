package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 规格基础信息增量更新命令 ProductSpecPatchCommand
 */
@Getter
@ToString
public class ProductSpecPatchCommand {
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
     * 规格名称
     */
    private final String specName;
    /**
     * 规格类型
     */
    private final SpecType specType;
    /**
     * 是否必选
     */
    private final boolean required;
    /**
     * 多语言列表
     */
    private final List<ProductSpecI18n> i18nList;

    private ProductSpecPatchCommand(@Nullable Long specId,
                                    String specCode,
                                    String specName,
                                    SpecType specType,
                                    boolean required,
                                    List<ProductSpecI18n> i18nList) {
        this.specId = specId;
        this.specCode = specCode;
        this.specName = specName;
        this.specType = specType;
        this.required = required;
        this.i18nList = i18nList;
    }

    /**
     * 创建规格基础信息更新命令
     *
     * @param specId   规格 ID
     * @param specCode 规格编码
     * @param specName 规格名称
     * @param specType 规格类型
     * @param required 是否必选
     * @param i18nList 多语言列表
     * @return 命令对象
     */
    public static ProductSpecPatchCommand of(@Nullable Long specId,
                                             String specCode,
                                             String specName,
                                             @Nullable SpecType specType,
                                             boolean required,
                                             @Nullable List<ProductSpecI18n> i18nList) {
        if (specId != null && specId <= 0)
            throw new IllegalArgumentException("规格 ID 非法");
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        requireNotNull(specType == null ? SpecType.OTHER : specType, "规格类型不能为空");
        List<ProductSpecI18n> safeI18n = i18nList == null ? List.of() : List.copyOf(i18nList);
        return new ProductSpecPatchCommand(specId, specCode, specName, specType == null ? SpecType.OTHER : specType, required, safeI18n);
    }
}

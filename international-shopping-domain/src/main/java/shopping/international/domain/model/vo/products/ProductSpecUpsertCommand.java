package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格维护命令 ProductSpecUpsertCommand
 *
 * <p>封装规格类别的基础属性、排序、启用状态及其规格值列表</p>
 */
@Getter
@ToString
public class ProductSpecUpsertCommand {
    /**
     * 规格 ID, 更新时传入
     */
    @Nullable
    private final Long specId;
    /**
     * 规格编码, 同一商品内唯一
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
     * 是否启用
     */
    private final boolean enabled;
    /**
     * 排序权重
     */
    private final int sortOrder;
    /**
     * 多语言列表
     */
    private final List<ProductSpecI18n> i18nList;
    /**
     * 规格值列表
     */
    private final List<ProductSpecValueUpsertCommand> values;

    private ProductSpecUpsertCommand(@Nullable Long specId,
                                     String specCode,
                                     String specName,
                                     SpecType specType,
                                     boolean required,
                                     boolean enabled,
                                     int sortOrder,
                                     List<ProductSpecI18n> i18nList,
                                     List<ProductSpecValueUpsertCommand> values) {
        this.specId = specId;
        this.specCode = specCode;
        this.specName = specName;
        this.specType = specType;
        this.required = required;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.i18nList = i18nList;
        this.values = values;
    }

    /**
     * 构建规格维护命令
     *
     * @param specId    规格 ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param specType  规格类型
     * @param required  是否必选
     * @param enabled   是否启用
     * @param sortOrder 排序权重
     * @param i18nList  多语言列表
     * @param values    规格值列表
     * @return 规格维护命令
     * @throws IllegalParamException 当编码或名称为空时抛出 IllegalParamException
     */
    public static ProductSpecUpsertCommand of(Long specId,
                                              String specCode,
                                              String specName,
                                              @Nullable SpecType specType,
                                              boolean required,
                                              boolean enabled,
                                              int sortOrder,
                                              @Nullable List<ProductSpecI18n> i18nList,
                                              @Nullable List<ProductSpecValueUpsertCommand> values) {
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        List<ProductSpecI18n> safeI18n = i18nList == null ? List.of() : List.copyOf(i18nList);
        List<ProductSpecValueUpsertCommand> safeValues = values == null ? List.of() : new ArrayList<>(values);
        return new ProductSpecUpsertCommand(specId, specCode, specName,
                specType == null ? SpecType.OTHER : specType,
                required, enabled, sortOrder, safeI18n, safeValues);
    }
}

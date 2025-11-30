package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格值维护命令 ProductSpecValueUpsertCommand
 *
 * <p>封装规格值的基础信息、启用状态与多语言数据, 供领域服务持久化</p>
 */
@Getter
@ToString
public class ProductSpecValueUpsertCommand {
    /**
     * 规格值 ID, 更新时填充
     */
    @Nullable
    private final Long valueId;
    /**
     * 规格 ID, 指向所属规格类别
     */
    private final Long specId;
    /**
     * 规格值编码, 同一规格下唯一
     */
    private final String valueCode;
    /**
     * 规格值名称
     */
    private final String valueName;
    /**
     * 扩展属性, 以 JSON 持久化
     */
    private final Map<String, Object> attributes;
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
    private final List<ProductSpecValueI18n> i18nList;

    private ProductSpecValueUpsertCommand(@Nullable Long valueId,
                                          Long specId,
                                          String valueCode,
                                          String valueName,
                                          Map<String, Object> attributes,
                                          boolean enabled,
                                          int sortOrder,
                                          List<ProductSpecValueI18n> i18nList) {
        this.valueId = valueId;
        this.specId = specId;
        this.valueCode = valueCode;
        this.valueName = valueName;
        this.attributes = attributes;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.i18nList = i18nList;
    }

    /**
     * 构建规格值维护命令
     *
     * @param valueId    规格值 ID, 更新时必填
     * @param specId     规格 ID
     * @param valueCode  规格值编码
     * @param valueName  规格值名称
     * @param attributes 扩展属性
     * @param enabled    是否启用
     * @param sortOrder  排序权重
     * @param i18nList   多语言列表
     * @return 规格值维护命令
     * @throws IllegalParamException 当编码或名称为空时抛出 IllegalParamException
     */
    public static ProductSpecValueUpsertCommand of(Long valueId,
                                                   Long specId,
                                                   String valueCode,
                                                   String valueName,
                                                   @Nullable Map<String, Object> attributes,
                                                   boolean enabled,
                                                   int sortOrder,
                                                   @Nullable List<ProductSpecValueI18n> i18nList) {
        requireNotBlank(valueCode, "规格值编码不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
        Map<String, Object> safeAttributes = attributes == null ? Collections.emptyMap() : new LinkedHashMap<>(attributes);
        List<ProductSpecValueI18n> safeI18n = i18nList == null ? List.of() : List.copyOf(i18nList);
        return new ProductSpecValueUpsertCommand(valueId, specId, valueCode, valueName, safeAttributes, enabled, sortOrder, safeI18n);
    }
}

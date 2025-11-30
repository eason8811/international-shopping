package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格增量维护请求 ProductSpecUpsertRequest
 */
@Data
public class ProductSpecUpsertRequest {
    /**
     * 规格 ID, 更新时传递
     */
    @Nullable
    private Long specId;
    /**
     * 规格编码, 同一商品内唯一
     */
    private String specCode;
    /**
     * 规格名称
     */
    private String specName;
    /**
     * 规格类型
     */
    @Nullable
    private SpecType specType;
    /**
     * 是否必选
     */
    @Nullable
    private Boolean isRequired;
    /**
     * 规格多语言列表
     */
    @Nullable
    private List<ProductSpecI18nPayload> i18nList;
    /**
     * 规格值列表
     */
    @Nullable
    private List<ProductSpecValueUpsertRequest> values;

    /**
     * 校验并规范化规格字段
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出 IllegalParamException
     */
    public void validate() {
        if (specId != null && specId <= 0)
            throw new IllegalParamException("规格 ID 非法");

        requireNotBlank(specCode, "规格编码不能为空");
        specCode = specCode.strip();
        if (specCode.length() > 64)
            throw new IllegalParamException("规格编码长度不能超过 64 个字符");

        requireNotBlank(specName, "规格名称不能为空");
        specName = specName.strip();
        if (specName.length() > 64)
            throw new IllegalParamException("规格名称长度不能超过 64 个字符");

        if (specType == null)
            specType = SpecType.OTHER;
        if (isRequired == null)
            isRequired = true;

        if (i18nList == null)
            i18nList = List.of();
        else {
            List<ProductSpecI18nPayload> normalized = new ArrayList<>();
            Set<String> locales = new LinkedHashSet<>();
            for (ProductSpecI18nPayload payload : i18nList) {
                if (payload == null)
                    continue;
                payload.validate();
                if (!locales.add(payload.getLocale()))
                    throw new IllegalParamException("规格多语言 locale 不可重复");
                normalized.add(payload);
            }
            i18nList = normalized;
        }

        if (values == null)
            values = List.of();
        else {
            List<ProductSpecValueUpsertRequest> normalized = new ArrayList<>();
            Set<String> valueCodes = new LinkedHashSet<>();
            for (ProductSpecValueUpsertRequest value : values) {
                if (value == null)
                    continue;
                value.validate();
                if (!valueCodes.add(value.getValueCode()))
                    throw new IllegalParamException("规格值编码不可重复");
                normalized.add(value);
            }
            values = normalized;
        }
    }
}

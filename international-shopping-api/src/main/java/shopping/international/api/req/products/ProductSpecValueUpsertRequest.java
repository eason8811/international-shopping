package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格值增量维护请求 ProductSpecValueUpsertRequest
 */
@Data
public class ProductSpecValueUpsertRequest {
    /**
     * 规格值 ID, 仅更新时填写
     */
    @Nullable
    private Long valueId;
    /**
     * 规格值编码, 要求唯一且稳定
     */
    private String valueCode;
    /**
     * 规格值名称
     */
    private String valueName;
    /**
     * 规格值扩展属性, 将以 JSON 形式入库
     */
    @Nullable
    private Map<String, Object> attributes;
    /**
     * 是否启用规格值
     */
    @Nullable
    private Boolean isEnabled;
    /**
     * 规格值多语言列表
     */
    @Nullable
    private List<ProductSpecValueI18nPayload> i18nList;

    /**
     * 校验并规范化规格值字段
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出 IllegalParamException
     */
    public void validate() {
        if (valueId != null && valueId <= 0)
            throw new IllegalParamException("规格值 ID 非法");

        requireNotBlank(valueCode, "规格值编码不能为空");
        valueCode = valueCode.strip();
        if (valueCode.length() > 64)
            throw new IllegalParamException("规格值编码长度不能超过 64 个字符");

        requireNotBlank(valueName, "规格值名称不能为空");
        valueName = valueName.strip();
        if (valueName.length() > 64)
            throw new IllegalParamException("规格值名称长度不能超过 64 个字符");

        if (isEnabled == null)
            isEnabled = true;

        if (i18nList == null) {
            i18nList = List.of();
            return;
        }
        List<ProductSpecValueI18nPayload> normalized = new ArrayList<>();
        Set<String> locales = new LinkedHashSet<>();
        for (ProductSpecValueI18nPayload payload : i18nList) {
            if (payload == null)
                continue;
            payload.validate();
            if (!locales.add(payload.getLocale()))
                throw new IllegalParamException("规格值多语言 locale 不可重复");
            normalized.add(payload);
        }
        i18nList = normalized;
    }
}

package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格值增量维护请求 ProductSpecValueUpsertRequest
 */
@Data
public class ProductSpecValueUpsertRequest {
    /**
     * 规格值 ID, 仅更新时填写
     */
    private Long valueId;
    /**
     * 规格值编码, 要求唯一且稳定
     */
    @Nullable
    private String valueCode;
    /**
     * 规格值名称
     */
    @Nullable
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
     * 校验并规范化新增规格值请求, 确保所有必填字段存在且格式正确
     *
     * <p>该方法会执行以下验证和处理步骤:</p>
     * <ul>
     *     <li>确保 {@code valueId} 为 null, 因为在新增时不应指定 ID</li>
     *     <li>校验并规范化 {@code valueCode}, 确保其非空且长度不超过 64 个字符</li>
     *     <li>校验并规范化 {@code valueName}, 确保其非空且长度不超过 64 个字符</li>
     *     <li>确保 {@code isEnabled} 字段已设置, 即不为 null</li>
     *     <li>调用 {@link #normalizeI18nList()} 方法来校验和规范化 {@code i18nList}</li>
     * </ul>
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出
     */
    public void createValidate() {
        require(valueId == null, "新增时规格值 ID 不能指定");
        valueCode = requireCreateField(valueCode, "规格值编码不能为空", v -> v.length() <= 64, "规格值编码长度不能超过 64 个字符");
        valueName = requireCreateField(valueName, "规格值名称不能为空", v -> v.length() <= 64, "规格值名称长度不能超过 64 个字符");
        requireNotNull(isEnabled, "是否启用规格值不能为空");
        normalizeI18nList();
    }

    /**
     * 校验并规范化规格值字段, 用于更新操作
     *
     * <p>该方法会执行以下验证和处理步骤:</p>
     * <ul>
     *     <li>确保 {@code valueId} 不为空且大于 0</li>
     *     <li>校验并规范化 {@code valueCode}, 确保其长度不超过 64 个字符</li>
     *     <li>校验并规范化 {@code valueName}, 确保其长度不超过 64 个字符</li>
     *     <li>如果 {@code isEnabled} 为 null, 则默认设置为 true</li>
     *     <li>调用 {@link #normalizeI18nList()} 方法来校验和规范化 {@code i18nList}</li>
     * </ul>
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出
     */
    public void updateValidate() {
        requireNotNull(valueId, "更新时规格值 ID 不能为空");
        require(valueId > 0, "更新时规格值 ID 必须大于 0");
        valueCode = requirePatchField(valueCode, v -> v.length() <= 64, "规格值编码长度不能超过 64 个字符");
        valueName = requirePatchField(valueName, v -> v.length() <= 64, "规格值名称长度不能超过 64 个字符");
        isEnabled = isEnabled == null || isEnabled;
        normalizeI18nList();
    }

    /**
     * 校验并规范化规格值多语言列表
     *
     * <p>此方法用于校验和规范化 {@link ProductSpecValueUpsertRequest#i18nList} 字段, 确保其内容符合预期格式,
     * 包括去除空元素, 检查每个 {@code ProductSpecValueI18nPayload} 实例的有效性, 并确保 locale 的唯一性</p>
     *
     * @throws IllegalParamException 当存在重复的 locale 或者任何 {@code ProductSpecValueI18nPayload} 实例无效时抛出
     */
    private void normalizeI18nList() {
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

package shopping.international.api.req.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格值增量维护请求 ProductSpecValueUpsertRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecValueUpsertRequest implements Verifiable {
    /**
     * 规格值 ID, 仅更新时填写
     */
    @Nullable
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
     * 验证当前对象是否符合预定义的规则或条件
     *
     * <p>此方法用于确保对象的状态或属性满足特定的要求, 如果验证失败, 则可能抛出异常来指示问题所在</p>
     *
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    @Override
    public void validate() {
        i18nList = normalizeDistinctList(i18nList, ProductSpecValueI18nPayload::getLocale, "规格多语言 locale 不可重复");
    }

    /**
     * 校验并规范化新增规格值请求, 确保所有必填字段存在且格式正确
     *
     * <p>该方法会执行以下验证和处理步骤:</p>
     * <ul>
     *     <li>确保 {@code valueId} 为 null, 因为在新增时不应指定规格值 ID</li>
     *     <li>校验并规范化 {@code valueCode}, 确保其不为空且长度不超过 64 个字符</li>
     *     <li>校验并规范化 {@code valueName}, 确保其不为空且长度不超过 64 个字符</li>
     *     <li>确保 {@code isEnabled} 字段不为 null, 即是否启用规格值必须被指定</li>
     *     <li>调用 {@link #validate()} 方法来进一步验证对象的其他属性</li>
     * </ul>
     *
     * @throws IllegalParamException 如果必填字段缺失或格式非法时抛出
     */
    public void createValidate() {
        require(valueId == null, "新增时规格值 ID 不能指定");
        valueCode = normalizeNotNullField(valueCode, "规格值编码不能为空", v -> v.length() <= 64, "规格值编码长度不能超过 64 个字符");
        valueName = normalizeNotNullField(valueName, "规格值名称不能为空", v -> v.length() <= 64, "规格值名称长度不能超过 64 个字符");
        requireNotNull(isEnabled, "是否启用规格值不能为空");
        validate();
    }

    /**
     * 校验并规范化更新规格值请求, 确保所有必填字段存在且格式正确
     *
     * <p>该方法会执行以下验证和处理步骤:</p>
     * <ul>
     *     <li>确保 {@code valueId} 不为空, 且大于 0</li>
     *     <li>校验并规范化 {@code valueCode}, 确保其长度不超过 64 个字符</li>
     *     <li>校验并规范化 {@code valueName}, 确保其长度不超过 64 个字符</li>
     *     <li>调用 {@link #validate()} 方法来进一步验证对象的其他属性</li>
     * </ul>
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出
     */
    public void updateValidate() {
        requireNotNull(valueId, "更新时规格值 ID 不能为空");
        require(valueId > 0, "更新时规格值 ID 必须大于 0");
        valueCode = normalizeNullableField(valueCode, "valueCode 不能为空", v -> v.length() <= 64, "规格值编码长度不能超过 64 个字符");
        valueName = normalizeNullableField(valueName, "valueName 不能为空", v -> v.length() <= 64, "规格值名称长度不能超过 64 个字符");
        validate();
    }
}

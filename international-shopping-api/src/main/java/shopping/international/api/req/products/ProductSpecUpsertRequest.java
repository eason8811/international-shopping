package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格增量更新请求 ProductSpecPatchRequest
 *
 * <p>仅更新规格基础信息, 规格值通过专用接口维护</p>
 */
@Data
public class ProductSpecUpsertRequest implements Verifiable {
    /**
     * 规格 ID, 更新时可选
     */
    @Nullable
    private Long specId;
    /**
     * 规格编码
     */
    @Nullable
    private String specCode;
    /**
     * 规格名称
     */
    @Nullable
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
     * 排序值 (小在前)
     */
    @Nullable
    private Integer sortOrder;
    /**
     * 是否启用
     */
    @Nullable
    private Boolean enabled;
    /**
     * 规格多语言列表
     */
    @Nullable
    private List<ProductSpecI18nPayload> i18nList;

    /**
     * 验证当前对象是否符合预定义的规则或条件
     *
     * <p>此方法用于确保对象的状态或属性满足特定的要求, 如果验证失败, 则可能抛出异常来指示问题所在</p>
     *
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    @Override
    public void validate() {
        i18nList = normalizeDistinctList(i18nList, ProductSpecI18nPayload::getLocale, "规格多语言 locale 不可重复");
    }

    /**
     * 在创建新规格时执行验证, 确保所有必要的字段符合业务规则
     *
     * <p>此方法用于在新增规格信息时进行数据校验. 它会检查以下几点:
     * <ul>
     *     <li>规格 ID 不能被指定</li>
     *     <li>规格类型不能为空</li>
     *     <li>是否必选字段不能为空</li>
     * </ul>
     * 之后调用 {@link #validate()} 方法来进一步验证其他字段.
     *
     * @throws IllegalParamException    如果上述条件未满足, 将抛出此异常
     * @throws IllegalArgumentException 如果其他字段不符合要求, 该异常包含了具体的错误信息
     */
    @Override
    public void createValidate() {
        require(specId == null, "新增时规格 ID 不能指定");
        specCode = normalizeNotNullField(specCode, "规格编码不能为空", s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        specName = normalizeNotNullField(specName, "规格名称不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        requireNotNull(specType, "规格类型不能为空");
        requireNotNull(isRequired, "是否必选不能为空");
        sortOrder = sortOrder == null || sortOrder < 0 ? 0 : sortOrder;
        enabled = enabled == null || enabled;
        validate();
    }

    /**
     * 在更新规格时执行验证, 确保所有必要的字段符合业务规则
     *
     * <p>此方法用于在更新规格信息时进行数据校验. 它会检查以下几点:
     * <ul>
     *     <li>规格 ID 必须被指定且不为空</li>
     *     <li>规格 ID 必须大于 0</li>
     * </ul>
     * 之后调用 {@link #validate()} 方法来进一步验证其他字段.
     *
     * @throws IllegalParamException    如果规格 ID 未满足上述条件, 将抛出此异常
     * @throws IllegalArgumentException 如果其他字段不符合要求, 该异常包含了具体的错误信息
     */
    @Override
    public void updateValidate() {
        requireNotNull(specId, "规格 ID 不能为空");
        require(specId > 0, "规格 ID 非法");
        specCode = normalizeNullableField(specCode, "规格编码不能为空", s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        specName = normalizeNullableField(specName, "规格名称不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        if (sortOrder != null)
            sortOrder = sortOrder < 0 ? 0 : sortOrder;
        validate();
    }
}

package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格增量更新请求 ProductSpecPatchRequest
 *
 * <p>仅更新规格基础信息, 规格值通过专用接口维护</p>
 */
@Data
public class ProductSpecUpsertRequest {
    /**
     * 规格 ID, 更新时可选
     */
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
     * 规格多语言列表
     */
    @Nullable
    private List<ProductSpecI18nPayload> i18nList;

    /**
     * 执行新增规格时的验证, 确保所有必填字段均被正确设置且符合业务规则
     *
     * <p>此方法用于在创建新的规格时进行数据校验. 它会检查以下几点:
     * <ul>
     *     <li>规格 ID 不能被指定</li>
     *     <li>规格编码和名称不能为空, 并且长度不能超过 64 个字符</li>
     *     <li>规格类型和是否必选字段必须被设置</li>
     *     <li>规格多语言列表需要通过 {@link #normalizeI18nList()} 方法进行规范化</li>
     * </ul>
     *
     * @throws IllegalParamException 如果上述任一条件未满足, 将抛出此异常
     */
    public void createValidate() {
        require(specId == null, "新增时规格 ID 不能指定");
        specCode = requireCreateField(specCode, "规格编码不能为空", s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        specName = requireCreateField(specName, "规格名称不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        require(specType != null, "规格类型不能为空");
        require(isRequired != null, "是否必选不能为空");
        normalizeI18nList();
    }

    /**
     * 执行规格更新时的验证, 确保所有必填字段均被正确设置且符合业务规则
     *
     * <p>此方法用于在更新现有规格时进行数据校验. 它会检查以下几点:
     * <ul>
     *     <li>规格 ID 必须存在且为正数</li>
     *     <li>规格编码和名称如果提供, 则长度不能超过 64 个字符</li>
     *     <li>规格类型如果未指定, 则默认为 {@link SpecType#OTHER}</li>
     *     <li>是否必选字段如果没有指定, 则默认为 <code>true</code></li>
     *     <li>规格多语言列表需要通过 {@link #normalizeI18nList()} 方法进行规范化</li>
     * </ul>
     *
     * @throws IllegalParamException 如果上述任一条件未满足, 将抛出此异常
     */
    public void updateValidate() {
        requireNotNull(specId, "规格 ID 不能为空");
        require(specId > 0, "规格 ID 非法");
        specCode = requirePatchField(specCode, s -> s.length() <= 64, "规格编码长度不能超过 64 个字符");
        specName = requirePatchField(specName, s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
        specType = specType == null ? SpecType.OTHER : specType;
        isRequired = isRequired == null || isRequired;
        normalizeI18nList();
    }

    /**
     * 校验并规范化规格多语言列表
     *
     * <p>此方法确保 {@code i18nList} 中的每个元素都经过了验证, 并且所有 locale 值都是唯一的. 如果 {@code i18nList} 为 null, 则将其初始化为空列表. 方法内部会遍历给定的多语言列表, 对每个元素调用其 validate 方法进行校验, 同时检查是否存在重复的 locale 值, 存在则抛出异常.</p>
     *
     * @throws IllegalParamException 当存在重复的 locale 或者某个 ProductSpecI18nPayload 实例无法通过其自身的校验时抛出
     */
    private void normalizeI18nList() {
        if (i18nList == null) {
            i18nList = List.of();
            return;
        }
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
}

package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣码创建/更新请求体 (DiscountCodeUpsertRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeUpsertRequest implements Verifiable {
    /**
     * 折扣码 (建议大写字母数字, 最小长度 4, 最大长度 32)
     */
    @Nullable
    private String code;
    /**
     * 折扣策略 ID
     */
    @Nullable
    private Long policyId;
    /**
     * 折扣码名称
     */
    @Nullable
    private String name;
    /**
     * 适用范围模式
     */
    @Nullable
    private DiscountScopeMode scopeMode;
    /**
     * 过期时间
     */
    @Nullable
    private LocalDateTime expiresAt;

    /**
     * 通用字段校验
     *
     * <p>该方法不会强制字段必填, 用于兼容 PATCH 更新场景</p>
     */
    @Override
    public void validate() {
        if (policyId != null)
            require(policyId >= 1, "policyId 必须大于等于 1");
    }

    /**
     * 创建场景校验
     *
     * <p>创建时要求必要字段完整</p>
     */
    @Override
    public void createValidate() {
        validate();
        code = normalizeNotNullField(code, "code 不能为空", s -> s.length() >= 4 && s.length() <= 32, "code 长度需在 4~32 之间");
        requireNotNull(policyId, "policyId 不能为空");
        name = normalizeNotNullField(name, "name 不能为空", s -> s.length() <= 120, "name 长度不能超过 120 个字符");
        requireNotNull(scopeMode, "scopeMode 不能为空");
        requireNotNull(expiresAt, "expiresAt 不能为空");
    }

    /**
     * 更新场景校验
     *
     * <p>更新时为 null 的字段表示不更新, 至少需要提供一个要更新的字段</p>
     */
    @Override
    public void updateValidate() {
        validate();
        code = normalizeNullableField(code, "code 不能为空", s -> s.length() >= 4 && s.length() <= 32, "code 长度需在 4~32 之间");
        name = normalizeNullableField(name, "name 不能为空", s -> s.length() <= 120, "name 长度不能超过 120 个字符");
        require(code != null || policyId != null || name != null || scopeMode != null || expiresAt != null,
                "更新折扣码时至少需要提供一个要更新的字段");
    }
}


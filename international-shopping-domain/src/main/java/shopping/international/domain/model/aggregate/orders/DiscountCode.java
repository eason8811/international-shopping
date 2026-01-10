package shopping.international.domain.model.aggregate.orders;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.domain.model.vo.orders.DiscountCodeText;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.time.Clock;
import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣码聚合根, 对应表 discount_code
 */
@Getter
@ToString
@Accessors(chain = true)
public class DiscountCode implements Verifiable {
    /**
     * 折扣码主键
     */
    private final Long id;
    /**
     * 折扣码文本内容
     */
    private final DiscountCodeText code;
    /**
     * 折扣策略 ID
     */
    private Long policyId;
    /**
     * 折扣码名称 (运营标识)
     */
    private String name;
    /**
     * 折扣范围模式
     */
    private DiscountScopeMode scopeMode;
    /**
     * 折扣码过期时间
     */
    private LocalDateTime expiresAt;
    /**
     * 是否永久有效
     */
    private Boolean permanent;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;

    /**
     * 构造一个折扣码实例
     *
     * @param id        折扣码主键, 必须非空
     * @param code      折扣码文本内容, 必须符合指定格式要求
     * @param policyId  折扣策略 ID, 必须非空
     * @param name      折扣码名称 (运营标识), 必须非空且长度不超过 120 个字符
     * @param scopeMode 折扣范围模式, 必须非空
     * @param expiresAt 折扣码过期时间
     * @param permanent 是否永久有效, 必须非空
     * @param createdAt 创建时间, 必须非空
     * @param updatedAt 更新时间, 必须非空
     */
    private DiscountCode(Long id, DiscountCodeText code, Long policyId, String name, DiscountScopeMode scopeMode,
                         @Nullable LocalDateTime expiresAt, Boolean permanent, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.policyId = policyId;
        this.name = name;
        this.scopeMode = scopeMode;
        this.expiresAt = expiresAt;
        this.permanent = permanent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validate();
    }

    /**
     * 创建一个新的折扣码实例
     *
     * @param code      折扣码文本内容, 必须符合指定格式要求
     * @param policyId  折扣策略 ID, 必须非空
     * @param name      折扣码名称 (运营标识), 必须非空且长度不超过 120 个字符
     * @param scopeMode 折扣范围模式, 必须非空
     * @param expiresAt 折扣码过期时间
     * @param permanent 是否永久有效, 必须非空
     * @return 新创建的 {@link DiscountCode} 实例
     */
    public static DiscountCode create(DiscountCodeText code, Long policyId, String name, DiscountScopeMode scopeMode, @Nullable LocalDateTime expiresAt, Boolean permanent) {
        return new DiscountCode(null, code, policyId, name, scopeMode, expiresAt, permanent, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 从给定的属性值重新构建一个 {@link DiscountCode} 实例
     *
     * @param id        折扣码主键, 必须非空
     * @param code      折扣码文本内容, 必须符合指定格式要求
     * @param policyId  折扣策略 ID, 必须非空
     * @param name      折扣码名称 (运营标识), 必须非空且长度不超过 120 个字符
     * @param scopeMode 折扣范围模式, 必须非空
     * @param expiresAt 折扣码过期时间
     * @param permanent 是否永久有效, 必须非空
     * @param createdAt 创建时间, 必须非空
     * @param updatedAt 更新时间, 必须非空
     * @return 新构建的 {@link DiscountCode} 实例
     */
    public static DiscountCode reconstitute(Long id, DiscountCodeText code, Long policyId, String name, DiscountScopeMode scopeMode,
                                            @Nullable LocalDateTime expiresAt, Boolean permanent, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DiscountCode(id, code, policyId, name, scopeMode, expiresAt, permanent, createdAt, updatedAt);
    }

    /**
     * 更新折扣码的部分属性。此方法允许更新策略 ID、名称、范围模式以及过期时间, 但只有当提供的参数非空时才会进行更新。
     *
     * @param policyId  可选的新的折扣策略 ID, 如果为 null 则不更新
     * @param name      可选的新名称, 如果为 null 或者仅包含空白字符则不更新
     * @param scopeMode 可选的新折扣范围模式, 如果为 null 刑不更新
     * @param expiresAt 可选的新过期时间, 如果为 null 则不更新
     * @param permanent 可选的新永久性标志, 如果为 null 则不更新
     */
    public void update(@Nullable Long policyId, @Nullable String name, @Nullable DiscountScopeMode scopeMode,
                       @Nullable LocalDateTime expiresAt, @Nullable Boolean permanent) {
        if (policyId != null)
            this.policyId = policyId;
        if (name != null)
            this.name = name.strip();
        if (scopeMode != null)
            this.scopeMode = scopeMode;
        if (expiresAt != null)
            this.expiresAt = expiresAt;
        if (permanent != null) {
            this.expiresAt = permanent ? null : this.expiresAt;
            this.permanent = permanent;
        }
        validate();
    }

    /**
     * 检查当前折扣码是否已经过期
     *
     * @param clock 用于获取当前时间的时钟, 必须非空
     * @return 如果折扣码已过期则返回 <code>true</code>, 否则返回 <code>false</code>
     */
    public boolean isExpired(Clock clock) {
        if (permanent)
            return false;
        requireNotNull(clock, "clock 不能为空");
        return expiresAt.isBefore(LocalDateTime.now(clock));
    }

    /**
     * 验证折扣码实例的有效性, 此方法会检查所有必填字段是否已正确设置, 包括折扣码、策略 ID、名称等
     * 如果任何必需的字段为空或不符合要求, 则抛出 {@link IllegalParamException} 异常
     *
     * <p>具体验证规则如下:
     * <ul>
     *     <li>折扣码不能为空</li>
     *     <li>策略 ID 不能为空</li>
     *     <li>折扣码名称不能为空且长度不得超过 120 个字符(不包括空白字符)</li>
     *     <li>scopeMode 不能为空</li>
     *     <li>expiresAt 不能为空</li>
     * </ul>
     *
     * @throws IllegalParamException 当任何一个必需字段为空或不满足特定条件时抛出
     */
    @Override
    public void validate() {
        requireNotNull(code, "折扣码不能为空");
        requireNotNull(policyId, "策略 ID 不能为空");
        requireNotBlank(name, "折扣码名称不能为空");
        require(name.strip().length() <= 120, "折扣码名称最长 120 个字符");
        requireNotNull(scopeMode, "scopeMode 不能为空");
        requireNotNull(permanent, "permanent 不能为空");
        if (!permanent)
            requireNotNull(expiresAt, "expiresAt 不能为空");
    }
}


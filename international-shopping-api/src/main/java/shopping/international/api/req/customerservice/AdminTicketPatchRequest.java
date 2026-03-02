package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧工单元数据更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketPatchRequest implements Verifiable {
    /**
     * 工单优先级
     */
    @Nullable
    private TicketPriority priority;
    /**
     * 工单标签列表
     */
    @Nullable
    private List<String> tags;
    /**
     * 申请退款金额 (分)
     */
    @Nullable
    private Long requestedRefundAmount;
    /**
     * 币种代码
     */
    @Nullable
    private String currency;
    /**
     * 承运商理赔外部编号
     */
    @Nullable
    private String claimExternalId;
    /**
     * SLA 到期时间
     */
    @Nullable
    private LocalDateTime slaDueAt;

    /**
     * 对管理侧工单更新参数进行校验与规范化
     */
    @Override
    public void validate() {
        boolean tagsProvided = tags != null;
        if (tagsProvided)
            tags = normalizeTags(tags);

        if (requestedRefundAmount != null)
            require(requestedRefundAmount >= 1, "requestedRefundAmount 必须大于等于 1");

        currency = normalizeNullableField(currency, "currency 不能为空",
                value -> CURRENCY_PATTERN.matcher(value.strip().toUpperCase(Locale.ROOT)).matches(),
                "currency 需为 3 位字母代码");
        if (currency != null)
            currency = currency.toUpperCase(Locale.ROOT);

        claimExternalId = normalizeNullableField(claimExternalId, "claimExternalId 不能为空",
                value -> value.length() <= 128,
                "claimExternalId 长度不能超过 128 个字符");

        require(priority != null || tagsProvided || requestedRefundAmount != null || currency != null || claimExternalId != null || slaDueAt != null,
                "至少需要提供一个可更新字段");
    }

    /**
     * 规范化标签列表并去重
     *
     * @param values 原始标签列表
     * @return 规范化后的标签列表
     */
    private static List<String> normalizeTags(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 50, "tags 元素数量不能超过 50");

        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, "tags 元素不能为空",
                    item -> item.length() <= 64,
                    "tags 元素长度不能超过 64 个字符");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }
}

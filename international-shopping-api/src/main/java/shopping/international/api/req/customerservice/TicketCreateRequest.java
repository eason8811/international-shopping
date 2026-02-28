package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户侧创建工单请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateRequest implements Verifiable {
    /**
     * 关联订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 关联订单明细 ID
     */
    @Nullable
    private Long orderItemId;
    /**
     * 关联物流单 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 问题类型
     */
    @Nullable
    private String issueType;
    /**
     * 工单标题
     */
    @Nullable
    private String title;
    /**
     * 问题描述
     */
    @Nullable
    private String description;
    /**
     * 附件链接列表
     */
    @Nullable
    private List<String> attachments;
    /**
     * 证据链接列表
     */
    @Nullable
    private List<String> evidence;
    /**
     * 申请退款金额（分）
     */
    @Nullable
    private Long requestedRefundAmount;
    /**
     * 币种代码
     */
    @Nullable
    private String currency;

    /**
     * 对创建工单参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "orderId 不能为空");
        require(orderId >= 1, "orderId 必须大于等于 1");

        if (orderItemId != null)
            require(orderItemId >= 1, "orderItemId 必须大于等于 1");
        if (shipmentId != null)
            require(shipmentId >= 1, "shipmentId 必须大于等于 1");

        issueType = normalizeNotNullField(issueType, "issueType 不能为空",
                TicketCreateRequest::isIssueType,
                "issueType 不支持").toUpperCase(Locale.ROOT);

        title = normalizeNotNullField(title, "title 不能为空",
                value -> value.length() <= 200,
                "title 长度不能超过 200 个字符");

        description = normalizeNullableField(description, "description 不能为空",
                value -> value.length() <= 2000,
                "description 长度不能超过 2000 个字符");

        attachments = normalizeLinkList(attachments, "attachments");
        evidence = normalizeLinkList(evidence, "evidence");

        if (requestedRefundAmount != null)
            require(requestedRefundAmount >= 1, "requestedRefundAmount 必须大于等于 1");

        currency = normalizeNotNullField(currency, "currency 不能为空",
                value -> CURRENCY_PATTERN.matcher(value.strip().toUpperCase(Locale.ROOT)).matches(),
                "currency 需为 3 位字母代码").toUpperCase(Locale.ROOT);
    }

    /**
     * 判断是否为受支持的问题类型
     *
     * @param value 原始问题类型
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isIssueType(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REFUND", "RESHIP", "CLAIM", "DELIVERY", "ADDRESS", "PRODUCT", "PAYMENT", "OTHER" -> true;
            default -> false;
        };
    }

    /**
     * 规范化链接列表并去重
     *
     * @param values 原始链接列表
     * @param field  字段名
     * @return 规范化后的链接列表
     */
    private static List<String> normalizeLinkList(@Nullable List<String> values, String field) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 10, field + " 元素数量不能超过 10");

        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, field + " 元素不能为空",
                    item -> item.length() <= 2048,
                    field + " 元素长度不能超过 2048 个字符");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }
}

package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.enums.orders.OrderRefundReasonCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 退款原因值对象
 */
@Getter
@ToString
@EqualsAndHashCode
public final class OrderRefundReason {
    /**
     * 订单退款原因最大长度
     */
    private static final int MAX_TEXT_LEN = 255;
    /**
     * 订单退款原因代码
     */
    private final OrderRefundReasonCode reasonCode;
    /**
     * 订单退款原因描述
     */
    private final String reasonText;
    /**
     * 订单退款原因附件 URL
     */
    private final List<String> attachments;

    /**
     * 构造一个订单退款原因值对象
     *
     * @param reasonCode  订单退款原因代码, 不能为空
     * @param reasonText  订单退款原因描述, 可为空, 但若非空则长度不能超过 {@code MAX_TEXT_LEN} 个字符
     * @param attachments 订单退款原因附件 URL 列表, 可为空, 非空时列表中的每个元素都必须是非空白字符串
     */
    private OrderRefundReason(OrderRefundReasonCode reasonCode, String reasonText, List<String> attachments) {
        this.reasonCode = reasonCode;
        this.reasonText = reasonText;
        this.attachments = attachments;
    }

    /**
     * 创建一个订单退款原因实例
     *
     * @param reasonCode  订单退款原因代码, 不能为空
     * @param reasonText  订单退款原因描述, 可为空, 但若非空则长度不能超过 {@code MAX_TEXT_LEN} 个字符
     * @param attachments 订单退款原因附件 URL 列表, 可为空, 非空时列表中的每个元素都必须是非空白字符串
     * @return 新创建的 <code>OrderRefundReason</code> 实例
     * @throws IllegalParamException 如果 <code>reasonCode</code> 为 <code>null</code>, 或者 <code>reasonText</code> 的长度超过了最大限制
     */
    public static OrderRefundReason of(OrderRefundReasonCode reasonCode, String reasonText, List<String> attachments) {
        requireNotNull(reasonCode, "退款原因不能为空");
        String normalizedText = null;
        if (reasonText != null) {
            String trimmed = reasonText.strip();
            if (!trimmed.isEmpty()) {
                require(trimmed.length() <= MAX_TEXT_LEN, "退款原因描述不能超过 " + MAX_TEXT_LEN + " 个字符");
                normalizedText = trimmed;
            }
        }
        List<String> normalizedAttachments = attachments == null ? List.of() :
                attachments.stream().filter(s -> s != null && !s.isBlank()).map(String::strip).toList();
        return new OrderRefundReason(reasonCode, normalizedText, normalizedAttachments);
    }
}


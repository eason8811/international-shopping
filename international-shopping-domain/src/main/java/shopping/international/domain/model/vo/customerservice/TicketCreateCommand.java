package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;

import java.util.List;

/**
 * 用户侧创建工单命令值对象
 *
 * @param orderId                关联订单 ID
 * @param orderItemId            关联订单明细 ID
 * @param shipmentId             关联物流单 ID
 * @param issueType              问题类型
 * @param title                  工单标题
 * @param description            工单描述
 * @param attachments            附件链接列表
 * @param evidence               证据链接列表
 * @param requestedRefundAmount  申请退款金额, 最小货币单位
 * @param currency               币种
 */
public record TicketCreateCommand(Long orderId,
                                  @Nullable Long orderItemId,
                                  @Nullable Long shipmentId,
                                  TicketIssueType issueType,
                                  String title,
                                  @Nullable String description,
                                  @Nullable List<String> attachments,
                                  @Nullable List<String> evidence,
                                  @Nullable Long requestedRefundAmount,
                                  String currency) {

    /**
     * 规范化构造, 兜底处理可选集合字段
     */
    public TicketCreateCommand {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}

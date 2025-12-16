package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderRefundReasonCode;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 订单退款申请请求体 (OrderRefundRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundRequest implements Verifiable {
    /**
     * 退款原因码
     */
    private OrderRefundReasonCode reasonCode;
    /**
     * 退款原因补充说明 (可选, 最大长度 255)
     */
    @Nullable
    private String reasonText;
    /**
     * 证据附件 URL 列表 (可选)
     */
    @Nullable
    private List<String> attachments;

    /**
     * 校验并规范化字段
     *
     * <p>该方法会过滤空白附件 URL, 并对单个 URL 做长度校验</p>
     */
    @Override
    public void validate() {
        requireNotNull(reasonCode, "reasonCode 不能为空");
        reasonText = normalizeNullableField(reasonText, "reasonText 不能为空", s -> s.length() <= 255, "reasonText 长度不能超过 255 个字符");

        if (attachments == null || attachments.isEmpty())
            return;

        List<String> normalized = new ArrayList<>();
        for (String url : attachments) {
            if (url == null)
                continue;
            String trimmed = url.strip();
            if (trimmed.isEmpty())
                continue;
            require(trimmed.length() <= 500, "attachments 中的 URL 长度不能超过 500 个字符");
            normalized.add(trimmed);
        }
        attachments = List.copyOf(normalized);
    }
}


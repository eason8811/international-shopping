package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 工单消息创建请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageCreateRequest implements Verifiable {
    /**
     * 消息类型
     */
    @Nullable
    private TicketMessageType messageType;
    /**
     * 消息内容
     */
    @Nullable
    private String content;
    /**
     * 附件链接列表
     */
    @Nullable
    private List<String> attachments;
    /**
     * 客户端消息幂等键
     */
    @Nullable
    private String clientMessageId;

    /**
     * 对工单消息创建参数进行校验与规范化
     */
    @Override
    public void validate() {
        content = normalizeNullableField(content, "content 不能为空",
                value -> value.length() <= 4000,
                "content 长度不能超过 4000 个字符");

        attachments = normalizeLinkList(attachments);

        clientMessageId = normalizeNotNullField(clientMessageId, "clientMessageId 不能为空",
                value -> value.length() <= 64,
                "clientMessageId 长度不能超过 64 个字符");

        require(content != null || !attachments.isEmpty(), "content 与 attachments 不能同时为空");
    }

    /**
     * 规范化链接列表并去重
     *
     * @param values 原始链接列表
     * @return 规范化后的链接列表
     */
    private static List<String> normalizeLinkList(@Nullable List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();
        require(values.size() <= 5, "attachments 元素数量不能超过 " + 5);

        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeNotNullField(value, "attachments 元素不能为空",
                    item -> item.length() <= 2048,
                    "attachments 元素长度不能超过 2048 个字符");
            dedup.add(normalized);
        }
        return new ArrayList<>(dedup);
    }
}

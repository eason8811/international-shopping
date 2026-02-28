package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单消息已读位点更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReadRequest implements Verifiable {
    /**
     * 最后已读消息 ID
     */
    @Nullable
    private Long lastReadMessageId;

    /**
     * 对工单已读位点参数进行校验
     */
    @Override
    public void validate() {
        requireNotNull(lastReadMessageId, "lastReadMessageId 不能为空");
        require(lastReadMessageId >= 1, "lastReadMessageId 必须大于等于 1");
    }
}

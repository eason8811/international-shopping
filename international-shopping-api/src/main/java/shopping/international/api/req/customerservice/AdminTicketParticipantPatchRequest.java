package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧工单参与方角色更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketParticipantPatchRequest implements Verifiable {
    /**
     * 参与方角色
     */
    @Nullable
    private TicketParticipantRole role;

    /**
     * 对工单参与方角色更新参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(role, "role 不能为空");
    }
}

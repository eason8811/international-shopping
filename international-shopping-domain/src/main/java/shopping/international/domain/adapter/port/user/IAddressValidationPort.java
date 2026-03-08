package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.user.AddressValidationCommand;
import shopping.international.domain.model.vo.user.AddressValidationResult;

/**
 * 地址校验端口
 */
public interface IAddressValidationPort {

    /**
     * 调用外部地址校验服务
     *
     * @param command 地址校验命令
     * @return 校验结果
     */
    @NotNull
    AddressValidationResult validate(@NotNull AddressValidationCommand command);
}

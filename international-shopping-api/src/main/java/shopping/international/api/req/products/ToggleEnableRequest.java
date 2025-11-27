package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 启用或停用请求体 ( ToggleEnableRequest )
 *
 * <p>用于管理端分类或其他资源的启用状态切换</p>
 */
@Data
public class ToggleEnableRequest {
    /**
     * 目标启用状态
     */
    private Boolean isEnabled;

    /**
     * 校验必填字段
     *
     * @throws IllegalParamException 当 isEnabled 为空时抛出
     */
    public void validate() {
        requireNotNull(isEnabled, "启用状态不能为空");
    }
}

package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.user.AddressChangeCommand;

/**
 * 收货地址领域服务接口
 */
public interface IAddressService {

    /**
     * 获取用户的收货地址列表, 支持分页查询
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param pageQuery 分页查询参数值对象
     * @return 返回一个 {@link PageResult} 对象, 包含了当前页的地址列表以及符合条件的地址总数
     */
    @NotNull
    PageResult<UserAddress> list(@NotNull Long userId, PageQuery pageQuery);


    /**
     * 根据用户ID和地址ID获取指定用户的特定收货地址信息
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     * @return 返回一个 {@link UserAddress} 对象, 包含了指定地址的所有详细信息
     */
    @NotNull
    UserAddress get(@NotNull Long userId, @NotNull Long addressId);

    /**
     * 为指定用户创建一个新的收货地址
     *
     * @param userId         用户的唯一标识符, 用于关联新创建的地址到该用户, 必须提供且非空
     * @param command        包含了新地址详细信息的命令对象, 必须提供且非空
     * @param idempotencyKey 用于确保请求幂等性的唯一键, 在重复请求时保证操作的一致性, 必须提供且非空
     * @return 返回一个新创建的 {@link UserAddress} 对象, 包括了系统生成的ID和其他详细信息
     */
    @NotNull
    UserAddress create(@NotNull Long userId, @NotNull AddressChangeCommand command, @NotNull String idempotencyKey);

    /**
     * 修改指定用户的收货地址信息
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     * @param command   地址修改命令
     * @return 返回一个 {@link UserAddress} 对象, 包含了更新后的地址详细信息
     */
    @NotNull
    UserAddress update(@NotNull Long userId,
                       @NotNull Long addressId,
                       @NotNull AddressChangeCommand command);

    /**
     * 删除指定用户的特定收货地址
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     */
    void delete(@NotNull Long userId, @NotNull Long addressId);

    /**
     * 将指定用户的某个地址设为默认收货地址
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     */
    void setDefault(@NotNull Long userId, @NotNull Long addressId);
}

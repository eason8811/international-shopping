package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

/**
 * 收货地址领域服务接口
 */
public interface IAddressService {

    /**
     * 简单分页返回
     */
    record PageResult(List<UserAddress> items, long total) {
    }

    /**
     * 获取用户的收货地址列表, 使用默认分页参数
     *
     * @param userId 用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @return 返回一个 {@link PageResult} 对象, 包含了当前页的地址列表以及符合条件的地址总数, 默认情况下展示第一页, 每页5条记录
     */
    @NotNull
    default PageResult list(@NotNull Long userId) {
        return list(userId, 1, 5);
    }

    /**
     * 获取用户的收货地址列表, 使用默认分页参数, 每页展示5条记录
     *
     * @param userId 用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param page   请求的页码, 从1开始计数, 用于分页逻辑中定位具体的页
     * @return 返回一个 {@link PageResult} 对象, 包含了当前页的地址列表以及符合条件的地址总数
     */
    @NotNull
    default PageResult list(@NotNull Long userId, int page) {
        return list(userId, page, 5);
    }

    /**
     * 获取用户的收货地址列表, 支持分页查询
     *
     * @param userId 用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param page   请求的页码, 从1开始计数, 用于分页逻辑中定位具体的页
     * @param size   每页展示的地址条目数量, 用于控制单次请求返回的数据量
     * @return 返回一个 {@link PageResult} 对象, 包含了当前页的地址列表以及符合条件的地址总数
     */
    @NotNull
    PageResult list(@NotNull Long userId, int page, int size);


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
     * @param address        包含了新地址详细信息的 {@link UserAddress} 对象, 必须提供且非空
     * @param idempotencyKey 用于确保请求幂等性的唯一键, 在重复请求时保证操作的一致性, 必须提供且非空
     * @return 返回一个新创建的 {@link UserAddress} 对象, 包括了系统生成的ID和其他详细信息
     */
    @NotNull
    UserAddress create(@NotNull Long userId, @NotNull UserAddress address, @NotNull String idempotencyKey);

    /**
     * 修改指定用户的收货地址信息
     *
     * @param userId       用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId    地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     * @param receiverName 收货人姓名, 可为空, 若不为空则更新该字段
     * @param phone        联系电话, 值对象, 可为空, 若不为空则更新该字段
     * @param country      国家, 可为空, 若不为空则更新该字段
     * @param province     省份, 可为空, 若不为空则更新该字段
     * @param city         城市, 可为空, 若不为空则更新该字段
     * @param district     区县, 可为空, 若不为空则更新该字段
     * @param addressLine1 地址行1, 可为空, 若不为空则更新该字段
     * @param addressLine2 地址行2, 可为空, 若不为空则更新该字段
     * @param zipcode      邮编, 可为空, 若不为空则更新该字段
     * @param makeDefault  是否设为默认地址, 可为空, 若不为空则根据其值设置当前地址是否为默认
     * @return 返回一个 {@link UserAddress} 对象, 包含了更新后的地址详细信息
     * @throws IllegalParamException 如果提供的 <code>receiverName</code> 或 <code>addressLine1</code> 为空白但不为 null 时抛出
     */
    @NotNull
    UserAddress update(@NotNull Long userId,
                       @NotNull Long addressId,
                       @Nullable String receiverName,
                       @Nullable PhoneNumber phone,
                       @Nullable String country,
                       @Nullable String province,
                       @Nullable String city,
                       @Nullable String district,
                       @Nullable String addressLine1,
                       @Nullable String addressLine2,
                       @Nullable String zipcode,
                       @Nullable Boolean makeDefault);

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

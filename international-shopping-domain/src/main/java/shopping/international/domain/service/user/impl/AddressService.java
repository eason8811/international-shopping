package shopping.international.domain.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.user.IAddressIdempotencyPort;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.service.user.IAddressService;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.IdempotencyException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 收货地址领域服务默认实现
 *
 * <p>职责:
 * <ul>
 *   <li>为应用层提供按"当前登录用户"为维度的地址增删改查能力</li>
 *   <li>协调 {@link User} 聚合根与 {@link IUserRepository} 仓储, 保证地址存在性与字段合法性</li>
 *   <li>将持久化操作下沉到基础设施层, 在仓储中通过事务控制默认地址唯一性</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AddressService implements IAddressService {

    /**
     * 用户聚合仓储端口
     *
     * <p>用于加载/校验用户聚合, 以及执行地址相关的持久化操作</p>
     */
    private final IUserRepository userRepository;
    /**
     * 地址创建幂等性端口
     *
     * <p>基于 Redis 记录 {@code (userId, Idempotency-Key)} 对应的创建状态</p>
     */
    private final IAddressIdempotencyPort addressIdempotencyPort;
    /**
     * PENDING 状态在 Redis 中的默认 TTL
     *
     * <p>在该时间窗口内, 重复请求会看到 {@code IN_PROGRESS} 或 {@code SUCCEEDED} 的结果, 避免重复插入</p>
     */
    private static final Duration IDEMPOTENCY_PENDING_TTL = Duration.ofMinutes(5);
    /**
     * 成功状态在 Redis 中的默认 TTL
     *
     * <p>在该时间窗口内, 相同幂等键的重复请求可直接返回已创建地址</p>
     */
    private static final Duration IDEMPOTENCY_SUCCESS_TTL = Duration.ofHours(24);

    /**
     * 获取用户的收货地址列表, 支持分页查询
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param pageQuery 分页查询参数值对象
     * @return 返回一个 {@link PageResult} 对象, 包含了当前页的地址列表以及符合条件的地址总数
     */
    @Override
    public @NotNull PageResult<UserAddress> list(@NotNull Long userId, PageQuery pageQuery) {

        List<UserAddress> items = userRepository.listAddresses(userId, pageQuery.offset(), pageQuery.size());
        long total = userRepository.countAddresses(userId);

        return new PageResult<>(items, total);
    }

    /**
     * 根据用户ID和地址ID获取指定用户的特定收货地址信息
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     * @return 返回一个 {@link UserAddress} 对象, 包含了指定地址的所有详细信息
     */
    @Override
    public @NotNull UserAddress get(@NotNull Long userId, @NotNull Long addressId) {
        return userRepository.findAddressById(userId, addressId)
                .orElseThrow(() -> new IllegalParamException("ID 为: " + addressId + " 的地址不存在"));
    }

    /**
     * 为指定用户创建一个新的收货地址
     * <p>创建逻辑具备幂等性:</p>
     * <ul>
     *     <li>首个携带某幂等键的请求获得创建权, 插入新地址并在 Redis 中标记为 {@code OK:{addressId}}</li>
     *     <li>后续携带相同幂等键的请求, 若状态为 OK, 直接回读该地址并返回</li>
     *     <li>若状态为 PENDING, 则忽略</li>
     * </ul>
     *
     * @param userId         用户的唯一标识符, 用于关联新创建的地址到该用户, 必须提供且非空
     * @param address        包含了新地址详细信息的 {@link UserAddress} 对象, 必须提供且非空
     * @param idempotencyKey 用于确保请求幂等性的唯一键, 在重复请求时保证操作的一致性, 必须提供且非空
     * @return 返回一个新创建的 {@link UserAddress} 对象, 包括了系统生成的ID和其他详细信息
     */
    @Override
    public @NotNull UserAddress create(@NotNull Long userId, @NotNull UserAddress address, @NotNull String idempotencyKey) {
        // 1) 装载用户聚合, 防止为不存在/已删除用户创建地址
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));

        // 2) 基于 Redis 注册幂等 Token 或获取已绑定结果
        IAddressIdempotencyPort.TokenStatus tokenStatus =
                addressIdempotencyPort.registerOrGet(userId, idempotencyKey, IDEMPOTENCY_PENDING_TTL);

        // 2.1 已成功: 直接回读地址并返回
        if (tokenStatus.isSucceeded()) {
            Long addressId = tokenStatus.addressId();
            if (addressId == null)
                throw new AppException("幂等键状态为 SUCCEEDED 但未绑定地址ID");

            return userRepository.findAddressById(userId, addressId)
                    .orElseThrow(() -> new AppException("幂等键已绑定地址, 但地址不存在, id=" + addressId));
        }

        // 2.2 正在处理: 说明已有请求抢到了创建权, 当前请求不再重复创建
        if (tokenStatus.status() == IAddressIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new IdempotencyException("相同幂等键的地址创建请求正在处理中, 请稍后重试");

        // 2.3 NEW: 当前请求获得创建权, 先在聚合中应用“新增地址”的领域规则 (含默认地址唯一性) ==
        user.addAddress(address);

        // 按聚合快照同步 DB (仓储不懂业务, 只执行同步)
        userRepository.saveAddresses(userId, user.getAddressesSnapshot());
        Long createdId = address.getId();
        if (createdId == null)
            throw new AppException("地址已创建但未获得ID, 请检查 saveAddresses 实现");
        addressIdempotencyPort.markSucceeded(userId, idempotencyKey, createdId, IDEMPOTENCY_SUCCESS_TTL);

        List<UserAddress> snapshot = user.getAddressesSnapshot();
        return snapshot.get(snapshot.size() - 1);
    }

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
    @Override
    public @NotNull UserAddress update(@NotNull Long userId,
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
                                       @Nullable Boolean makeDefault) {
        // 1) 装载聚合, 让聚合负责"是否存在该地址"和字段合法性检查
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));

        // 2) 在聚合中完成业务语义 (不存在会抛 IllegalParamException; receiverName/addressLine1 为空白会抛错)
        user.updateAddress(addressId, receiverName, phone, country, province, city, district,
                addressLine1, addressLine2, zipcode, makeDefault);

        // 3) 从聚合快照中找到更新后的地址实体
        UserAddress updatedAddressInAggregate = user.getAddressesSnapshot().stream()
                .filter(address -> Objects.equals(address.getId(), addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("更新后的地址在聚合中不存在, id=" + addressId));

        // 按聚合快照同步 DB (仓储不懂业务, 只执行同步)
        userRepository.saveAddresses(userId, user.getAddressesSnapshot());

        return updatedAddressInAggregate;
    }

    /**
     * 删除指定用户的特定收货地址
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     */
    @Override
    public void delete(@NotNull Long userId, @NotNull Long addressId) {
        // 1) 聚合内先做"地址是否存在"的检查
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));
        // 若地址不存在, 将抛 IllegalParamException
        user.removeAddress(addressId);

        // 2) 再删除持久化记录
        userRepository.deleteAddress(userId, addressId);
    }

    /**
     * 将指定用户的某个地址设为默认收货地址
     *
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     */
    @Override
    public void setDefault(@NotNull Long userId, @NotNull Long addressId) {
        // 1) 聚合内先检查"地址是否存在"
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));
        user.setDefaultAddress(addressId);

        // 按聚合快照同步 DB (仓储不懂业务, 只执行同步)
        userRepository.saveAddresses(userId, user.getAddressesSnapshot());
    }
}

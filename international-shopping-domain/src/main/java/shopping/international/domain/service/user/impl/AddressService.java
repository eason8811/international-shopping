package shopping.international.domain.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.user.IAddressIdempotencyPort;
import shopping.international.domain.adapter.port.user.IAddressValidationPort;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.enums.user.AddressSource;
import shopping.international.domain.model.enums.user.AddressValidationStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.user.AddressChangeCommand;
import shopping.international.domain.model.vo.user.AddressValidationCommand;
import shopping.international.domain.model.vo.user.AddressValidationResult;
import shopping.international.domain.service.user.IAddressService;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.IdempotencyException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

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
     * 地址校验端口
     */
    private final IAddressValidationPort addressValidationPort;
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
     * @param command        包地址创建/修改命令
     * @param idempotencyKey 用于确保请求幂等性的唯一键, 在重复请求时保证操作的一致性, 必须提供且非空
     * @return 返回一个新创建的 {@link UserAddress} 对象, 包括了系统生成的ID和其他详细信息
     */
    @Override
    public @NotNull UserAddress create(@NotNull Long userId, @NotNull AddressChangeCommand command, @NotNull String idempotencyKey) {
        // 1) 装载用户聚合, 防止为不存在/已删除用户创建地址
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));

        // 2) 先完成地址校验与规范化, 避免校验失败把幂等键留在 PENDING
        UserAddress address = buildValidatedAddressForCreate(command);

        // 3) 基于 Redis 注册幂等 Token 或获取已绑定结果
        IAddressIdempotencyPort.TokenStatus tokenStatus =
                addressIdempotencyPort.registerOrGet(userId, idempotencyKey, IDEMPOTENCY_PENDING_TTL);

        // 3.1 已成功: 直接回读地址并返回
        if (tokenStatus.isSucceeded()) {
            Long addressId = tokenStatus.addressId();
            if (addressId == null)
                throw new AppException("幂等键状态为 SUCCEEDED 但未绑定地址ID");

            return userRepository.findAddressById(userId, addressId)
                    .orElseThrow(() -> new AppException("幂等键已绑定地址, 但地址不存在, id=" + addressId));
        }

        // 3.2 正在处理: 说明已有请求抢到了创建权, 当前请求不再重复创建
        if (tokenStatus.status() == IAddressIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new IdempotencyException("相同幂等键的地址创建请求正在处理中, 请稍后重试");

        // 3.3 NEW: 当前请求获得创建权, 先在聚合中应用“新增地址”的领域规则 (含默认地址唯一性) ==
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
     * @param userId    用户的唯一标识符, 用于筛选属于该用户的地址记录, 必须提供且非空
     * @param addressId 地址的唯一标识符, 用于定位具体的收货地址, 必须提供且非空
     * @param command   地址创建/修改命令
     * @return 返回一个 {@link UserAddress} 对象, 包含了更新后的地址详细信息
     * @throws IllegalParamException 如果提供的 <code>receiverName</code> 或 <code>addressLine1</code> 为空白但不为 null 时抛出
     */
    @Override
    public @NotNull UserAddress update(@NotNull Long userId, @NotNull Long addressId, @NotNull AddressChangeCommand command) {
        requireNotNull(command, "地址修改命令不能为空");

        // 1) 先按 user_id + address_id 查旧数据并校验归属
        UserAddress oldAddress = userRepository.findAddressById(userId, addressId)
                .orElseThrow(() -> new IllegalParamException("ID 为: " + addressId + " 的地址不存在"));

        boolean coreChanged = hasCoreFieldChanged(oldAddress, command);

        // 2) 装载聚合, 让聚合负责默认地址唯一性
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalParamException("用户不存在"));
        UserAddress currentAddress = user.getAddressesSnapshot().stream()
                .filter(address -> Objects.equals(address.getId(), addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("待更新地址在聚合中不存在, id: " + addressId));

        AddressSource finalAddressSource = resolveAddressSource(command.addressSource(), oldAddress.getAddressSource());
        UserAddress nextSnapshot = currentAddress.toBuilder()
                .receiverName(resolve(command.receiverName(), currentAddress.getReceiverName()))
                .phone(resolve(command.phone(), currentAddress.getPhone()))
                .countryCode(resolve(command.countryCode(), currentAddress.getCountryCode()))
                .country(resolve(command.country(), currentAddress.getCountry()))
                .province(resolve(command.province(), currentAddress.getProvince()))
                .city(resolve(command.city(), currentAddress.getCity()))
                .district(resolve(command.district(), currentAddress.getDistrict()))
                .addressLine1(resolve(command.addressLine1(), currentAddress.getAddressLine1()))
                .addressLine2(resolve(command.addressLine2(), currentAddress.getAddressLine2()))
                .zipcode(resolve(command.zipcode(), currentAddress.getZipcode()))
                .languageCode(resolve(command.languageCode(), currentAddress.getLanguageCode()))
                .addressSource(finalAddressSource)
                .validationStatus(currentAddress.getValidationStatus())
                .validatedAt(currentAddress.getValidatedAt())
                .extension(currentAddress.getExtension())
                .build();

        if (coreChanged) {
            AddressValidationResult validationResult = validateAddress(buildValidationCommand(oldAddress, command));
            nextSnapshot = nextSnapshot.toBuilder()
                    .countryCode(validationResult.countryCode())
                    .country(validationResult.country())
                    .province(validationResult.province())
                    .city(validationResult.city())
                    .district(validationResult.district())
                    .addressLine1(validationResult.addressLine1())
                    .addressLine2(validationResult.addressLine2())
                    .zipcode(validationResult.zipcode())
                    .languageCode(validationResult.languageCode())
                    .validationStatus(validationResult.validationStatus())
                    .validatedAt(LocalDateTime.now())
                    .extension(validationResult.extension())
                    .build();
        }

        // 3) 在聚合中完成业务语义 (不存在会抛 IllegalParamException)
        user.updateAddress(addressId, nextSnapshot, command.makeDefault());

        // 4) 从聚合快照中找到更新后的地址实体
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

    /**
     * 构建并验证用于创建的地址信息
     *
     * @param command 包含了地址变更所需的所有信息的 <code>AddressChangeCommand</code> 对象, 不能为空
     * @return 返回一个经过验证的 <code>UserAddress</code> 对象, 该对象包含了所有必要的地址信息以及是否设为默认地址等属性
     */
    private @NotNull UserAddress buildValidatedAddressForCreate(@NotNull AddressChangeCommand command) {
        AddressSource addressSource = normalizeNotNull(command.addressSource(), "地址来源不能为空");
        AddressValidationResult validationResult = validateAddress(buildValidationCommand(null, command));
        return UserAddress.of(
                normalizeNotNull(command.receiverName(), "收货人不能为空"),
                normalizeNotNull(command.phone(), "联系电话不能为空"),
                validationResult.countryCode(),
                validationResult.country(),
                validationResult.province(),
                validationResult.city(),
                validationResult.district(),
                validationResult.addressLine1(),
                validationResult.addressLine2(),
                validationResult.zipcode(),
                validationResult.languageCode(),
                addressSource,
                Boolean.TRUE.equals(command.makeDefault()),
                validationResult.validationStatus(),
                LocalDateTime.now(),
                validationResult.extension()
        );
    }

    /**
     * 校验地址的有效性
     *
     * @param command 包含待校验地址信息的命令对象, 不能为空
     * @return 地址校验结果, 如果地址需要修正或被拒绝, 则抛出异常
     * @throws IllegalParamException 如果地址校验状态为 FIX 或 REJECT, 表示地址存在问题, 需要根据返回的 action 进行相应处理
     */
    private @NotNull AddressValidationResult validateAddress(@NotNull AddressValidationCommand command) {
        AddressValidationResult validationResult = addressValidationPort.validate(command);
        if (validationResult.validationStatus() == AddressValidationStatus.FIX
                || validationResult.validationStatus() == AddressValidationStatus.REJECT) {
            String suffix = validationResult.possibleNextAction() == null
                    ? validationResult.validationStatus().name()
                    : validationResult.possibleNextAction();
            throw new IllegalParamException("地址校验未通过, 请修改后重试, action:" + suffix);
        }
        return validationResult;
    }

    /**
     * 构建一个地址验证命令, 用于进一步处理用户地址信息的验证
     *
     * @param oldAddress 用户原有的地址信息, 可以为 null
     * @param command 包含用户新输入或修改后的地址信息的命令对象
     * @return 返回构建好的 <code>AddressValidationCommand</code> 对象, 该对象包含了所有必要的信息来执行地址验证
     */
    private @NotNull AddressValidationCommand buildValidationCommand(UserAddress oldAddress, AddressChangeCommand command) {
        String countryCode = normalizeNotNull(resolve(command.countryCode(), oldAddress == null ? null : oldAddress.getCountryCode()), "国家编码不能为空");
        String country = normalizeNotNull(resolve(command.country(), oldAddress == null ? null : oldAddress.getCountry()), "国家不能为空");
        String addressLine1 = normalizeNotNull(resolve(command.addressLine1(), oldAddress == null ? null : oldAddress.getAddressLine1()), "地址行1不能为空");
        String previousResponseId = null;
        if (oldAddress != null && oldAddress.getExtension() != null)
            previousResponseId = oldAddress.getExtension().getValidationResponseId();
        return new AddressValidationCommand(
                countryCode,
                country,
                resolve(command.province(), oldAddress == null ? null : oldAddress.getProvince()),
                resolve(command.city(), oldAddress == null ? null : oldAddress.getCity()),
                resolve(command.district(), oldAddress == null ? null : oldAddress.getDistrict()),
                addressLine1,
                resolve(command.addressLine2(), oldAddress == null ? null : oldAddress.getAddressLine2()),
                resolve(command.zipcode(), oldAddress == null ? null : oldAddress.getZipcode()),
                resolve(command.languageCode(), oldAddress == null ? null : oldAddress.getLanguageCode()),
                command.rawInput(),
                command.googlePlaceId(),
                command.placeResponse(),
                previousResponseId
        );
    }

    /**
     * 检查核心字段是否已更改
     *
     * <p>此方法用于比较旧地址与命令中的新值, 以确定任何核心字段(如国家代码, 省份等)是否有变化
     *
     * @param oldAddress 旧的用户地址信息
     * @param command 包含新的地址信息的命令对象
     * @return 如果至少有一个核心字段发生了改变, 则返回 true; 否则返回 false
     */
    private boolean hasCoreFieldChanged(@NotNull UserAddress oldAddress, @NotNull AddressChangeCommand command) {
        return isChanged(command.countryCode(), oldAddress.getCountryCode())
                || isChanged(command.country(), oldAddress.getCountry())
                || isChanged(command.province(), oldAddress.getProvince())
                || isChanged(command.city(), oldAddress.getCity())
                || isChanged(command.district(), oldAddress.getDistrict())
                || isChanged(command.addressLine1(), oldAddress.getAddressLine1())
                || isChanged(command.addressLine2(), oldAddress.getAddressLine2())
                || isChanged(command.zipcode(), oldAddress.getZipcode())
                || isChanged(command.languageCode(), oldAddress.getLanguageCode());
    }

    /**
     * 判断给定的新值和旧值是否不同
     *
     * @param newValue 新值 用于与旧值比较
     * @param oldValue 旧值 作为比较的基准
     * @return 如果新值与旧值不相同则返回 true, 否则返回 false
     */
    private boolean isChanged(Object newValue, Object oldValue) {
        return newValue != null && !Objects.equals(newValue, oldValue);
    }

    /**
     * 根据给定的请求地址源和当前地址源, 解析出最终使用的地址源
     * <p>
     * 如果请求的地址源不为空, 则直接返回该请求地址源作为解析结果
     * 否则, 检查当前地址源是否为空, 若为空, 则返回 <code>AddressSource.MANUAL</code>; 否则返回当前地址源
     *
     * @param requested 请求时指定的地址源
     * @param current 当前已设置的地址源
     * @return 解析后的地址源, 优先级为: 请求地址源 > 当前地址源 > 手动设置
     */
    private static AddressSource resolveAddressSource(AddressSource requested, AddressSource current) {
        if (requested != null)
            return requested;
        return current == null ? AddressSource.MANUAL : current;
    }

    /**
     * <p>此方法用于解析请求值和当前值之间的最佳选择, 如果请求的值不为空, 则返回请求的值, 否则, 返回当前值</p>
     *
     * @param <T> 泛型 T 代表了 requested 和 current 参数的类型, 可以是任何对象类型
     * @param requested 请求的值, 优先级高于 current 值
     * @param current 当前存在的值, 当 requested 为 null 时作为备选
     * @return 返回非空的 requested 或者 current 值
     */
    private static <T> T resolve(T requested, T current) {
        return requested != null ? requested : current;
    }
}

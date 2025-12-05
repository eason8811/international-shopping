package shopping.international.infrastructure.adapter.repository.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.enums.user.AccountStatus;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.enums.user.Gender;
import shopping.international.domain.model.vo.user.*;
import shopping.international.infrastructure.dao.user.UserAccountMapper;
import shopping.international.infrastructure.dao.user.UserAddressMapper;
import shopping.international.infrastructure.dao.user.UserAuthMapper;
import shopping.international.infrastructure.dao.user.UserProfileMapper;
import shopping.international.infrastructure.dao.user.po.UserAccountPO;
import shopping.international.infrastructure.dao.user.po.UserAddressPO;
import shopping.international.infrastructure.dao.user.po.UserAuthPO;
import shopping.international.infrastructure.dao.user.po.UserProfilePO;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Flex 的用户聚合仓储实现
 * <p>职责: 按聚合粒度对 {@code user_account / user_auth / user_profile / user_address} 进行组合读写</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    /**
     * user_account Mapper
     */
    private final UserAccountMapper accountMapper;
    /**
     * user_auth Mapper
     */
    private final UserAuthMapper authMapper;
    /**
     * user_profile Mapper
     */
    private final UserProfileMapper profileMapper;
    /**
     * user_address Mapper
     */
    private final UserAddressMapper addressMapper;
    /**
     * JSON 序列化/反序列化工具
     */
    private final ObjectMapper mapper;

    // ========================= 查询 =========================

    /**
     * 按登录账号查询用户 (账号为用户名 / 邮箱 / 手机号), 返回完整聚合快照 (含绑定)
     *
     * @param account 用户名 / 邮箱 / 手机号
     * @return 用户聚合, 可为空
     */
    @Override
    public Optional<User> findByLoginAccount(@NotNull String account) {
        QueryWrapper query = QueryWrapper.create()
                .and(q -> q.where(UserAccountPO::getUsername).eq(account)
                        .or(UserAccountPO::getEmail).eq(account)
                        .or(UserAccountPO::getPhone).eq(account))
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE)
                .limit(1);
        UserAccountPO po = accountMapper.selectOneByQuery(query);
        return assembleOptional(po);
    }

    /**
     * 按邮箱精确查询用户 (用于激活等流程), 返回完整聚合快照
     *
     * @param email 邮箱
     * @return 用户聚合, 可为空
     */
    @Override
    public Optional<User> findByEmail(@NotNull EmailAddress email) {
        QueryWrapper query = QueryWrapper.create()
                .where(UserAccountPO::getEmail).eq(email.getValue())
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE)
                .limit(1);
        UserAccountPO po = accountMapper.selectOneByQuery(query);
        return assembleOptional(po);
    }

    /**
     * 按主键查询用户
     *
     * @param userId 用户 ID
     * @return 用户聚合, 可为空
     */
    @Override
    public Optional<User> findById(@NotNull Long userId) {
        UserAccountPO po = accountMapper.selectOneById(userId);
        if (po == null || Boolean.TRUE.equals(po.getIsDeleted()))
            return Optional.empty();
        return assembleOptional(po);
    }

    /**
     * 检查用户名是否已存在 (幂等注册前置唯一性校验)
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByUsername(@NotNull Username username) {
        long n = accountMapper.selectCountByQuery(QueryWrapper.create()
                .where(UserAccountPO::getUsername).eq(username.getValue())
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE));
        return n > 0;
    }

    /**
     * 检查邮箱是否已存在 (幂等注册前置唯一性校验)
     *
     * @param email 邮箱
     * @return 是否存在 (忽略 null)
     */
    @Override
    public boolean existsByEmail(@NotNull EmailAddress email) {
        long n = accountMapper.selectCountByQuery(QueryWrapper.create()
                .where(UserAccountPO::getEmail).eq(email.getValue())
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE));
        return n > 0;
    }

    /**
     * 检查手机号是否已存在 (幂等注册前置唯一性校验)
     *
     * @param phone 手机号
     * @return 是否存在 (忽略 null)
     */
    @Override
    public boolean existsByPhone(@NotNull PhoneNumber phone) {
        long n = accountMapper.selectCountByQuery(QueryWrapper.create()
                .where(UserAccountPO::getPhone).eq(phone.getValue())
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE));
        return n > 0;
    }

    /**
     * 按第三方身份 (issuer + provider_uid/sub) 查询用户
     *
     * @param issuer      OIDC iss (或等价发行方标识)
     * @param providerUid OIDC sub (发行方内用户唯一 ID)
     * @return 用户聚合
     */
    @Override
    public @NotNull Optional<User> findByProviderUid(@NotNull String issuer, @NotNull String providerUid) {
        UserAuthPO auth = authMapper.selectOneByQuery(QueryWrapper.create()
                .where(UserAuthPO::getIssuer).eq(issuer)
                .and(UserAuthPO::getProviderUid).eq(providerUid)
                .limit(1));
        if (auth == null)
            return Optional.empty();
        UserAccountPO account = accountMapper.selectOneById(auth.getUserId());
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted()))
            return Optional.empty();
        return assembleOptional(account);
    }

    // ========================= 写入 =========================

    /**
     * 持久化一个全新的用户聚合 (账户 + 绑定 + 资料 + 地址)
     *
     * <p>要求 {@code user.id == null}, 至少存在一种登录方式
     * 若存在 LOCAL 绑定, 则其 passwordHash 必须非空
     * 成功后返回带持久化主键的快照 (含各子实体主键)</p>
     *
     * @param user 待保存的新用户对象, 包含用户账户, 绑定信息, 资料及地址等
     * @return 保存后的用户对象, 包括由数据库生成的主键和其他相关字段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull User saveNewUserWithBindings(User user) {
        // 1. 入库 user_account
        UserAccountPO acc = UserAccountPO.builder()
                .username(user.getUsername().getValue())
                .nickname(user.getNickname().getValue())
                .email(user.getEmail().getValue())
                .phone(user.getPhone() == null ? null : user.getPhone().getValue())
                .status(user.getStatus() == null ? AccountStatus.DISABLED.name() : user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .isDeleted(Boolean.FALSE)
                .build();
        accountMapper.insertSelective(acc); // 自增主键回填, 忽略 null 以保留 DB 默认值
        Long userId = acc.getId();

        // 2. 入库 user_profile
        UserProfilePO profile = new UserProfilePO(
                userId,
                user.getProfile() == null ? null : user.getProfile().getDisplayName(),
                user.getProfile() == null ? null : user.getProfile().getAvatarUrl(),
                user.getProfile() == null ? Gender.UNKNOWN.name() : user.getProfile().getGender().name(),   // Gender → String
                user.getProfile() == null ? null : user.getProfile().getBirthday(),
                user.getProfile() == null ? null : user.getProfile().getCountry(),
                user.getProfile() == null ? null : user.getProfile().getProvince(),
                user.getProfile() == null ? null : user.getProfile().getCity(),
                user.getProfile() == null ? null : user.getProfile().getAddressLine(),
                user.getProfile() == null ? null : user.getProfile().getZipcode(),
                user.getProfile() == null ? null : toJsonOrNull(user.getProfile().getExtra()), // Map → JSON
                null,
                null
        );
        profileMapper.insertSelective(profile);

        // 3. 入库 user_auth (遍历绑定)
        for (AuthBinding binding : user.getBindingsSnapshot()) {
            UserAuthPO authPO = UserAuthPO.builder()
                    .userId(userId)
                    .provider(binding.getProvider().name())
                    .issuer(binding.getIssuer())
                    .providerUid(binding.getProvider() == AuthProvider.LOCAL ? userId.toString() : binding.getProviderUid())
                    .passwordHash(binding.getPasswordHash())
                    .accessToken(binding.getAccessToken() == null ? null : binding.getAccessToken().getBytes())   // EncryptedSecret → bytes()
                    .refreshToken(binding.getRefreshToken() == null ? null : binding.getRefreshToken().getBytes())
                    .expiresAt(binding.getExpiresAt())
                    .scope(binding.getScope())
                    .role(binding.getRole())
                    .lastLoginAt(binding.getLastLoginAt())
                    .build();
            authMapper.insertSelective(authPO);
        }

        // 4 入库 user_address
        if (user.getAddressesSnapshot() != null && !user.getAddressesSnapshot().isEmpty()) {
            for (UserAddress a : user.getAddressesSnapshot()) {
                UserAddressPO addressPO = UserAddressPO.builder()
                        .userId(userId)
                        .receiverName(a.getReceiverName())
                        .phone(a.getPhone() == null ? null : a.getPhone().getValue())
                        .country(a.getCountry())
                        .province(a.getProvince())
                        .city(a.getCity())
                        .district(a.getDistrict())
                        .addressLine1(a.getAddressLine1())
                        .addressLine2(a.getAddressLine2())
                        .zipcode(a.getZipcode())
                        .isDefault(a.isDefaultAddress())
                        .build();
                addressMapper.insertSelective(addressPO);
            }
        }

        // 5. 回读聚合 (含各子表主键)
        return findById(userId).orElseThrow(() -> new IllegalStateException("保存后回读用户失败"));
    }

    /**
     * 更新账户状态 (如 DISABLED → ACTIVE)
     *
     * @param userId 用户 ID
     * @param status 新状态
     */
    @Override
    public void updateStatus(@NotNull Long userId, @NotNull AccountStatus status) {
        UpdateChain.of(accountMapper)
                .set(UserAccountPO::getStatus, status.name())
                .where(UserAccountPO::getId).eq(userId)
                .update();
    }

    /**
     * 记录登录时间戳与通道最近登录时间 (如 provider=LOCAL)
     *
     * @param userId    用户 ID
     * @param provider  认证提供方
     * @param loginTime 登录时间
     */
    @Override
    public void recordLogin(@NotNull Long userId, @NotNull AuthProvider provider, @NotNull LocalDateTime loginTime) {
        // 1) 更新账户最近登录
        UpdateChain.of(accountMapper)
                .set(UserAccountPO::getLastLoginAt, loginTime)
                .where(UserAccountPO::getId).eq(userId)
                .update();
        // 2) 更新对应通道最近登录
        UpdateChain.of(authMapper)
                .set(UserAuthPO::getLastLoginAt, loginTime)
                .where(UserAuthPO::getUserId).eq(userId)
                .and(UserAuthPO::getProvider).eq(provider.name())
                .update();
    }

    // ========================= 增量写入 =========================

    /**
     * 检查是否存在手机号相同的其他用户, 排除指定的用户 ID
     *
     * @param userId 用户 ID 需要排除的用户 ID, 用于避免查询到当前用户自己
     * @param phone  手机号 要检查的手机号, 必须是有效的 {@link PhoneNumber} 对象
     * @return 是否存在 如果存在其他用户的手机号与给定的手机号相同, 返回 true; 否则返回 false
     */
    @Override
    public boolean existsByPhoneExceptUser(@NotNull Long userId, @NotNull PhoneNumber phone) {
        long count = accountMapper.selectCountByQuery(QueryWrapper.create()
                .where(UserAccountPO::getPhone).eq(phone.getValue())
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE)
                .and(UserAccountPO::getId).ne(userId));
        return count > 0;
    }


    /**
     * 更新指定用户ID的昵称和手机号
     *
     * @param userId   用户ID, 必须提供
     * @param nickname 新的昵称, 可以是 {@code null}, 如果为 {@code null} 则不更新昵称
     * @param phone    新的手机号, 可以是 {@code null}, 如果为 {@code null} 则不更新手机号
     * @throws IllegalStateException 当尝试更新不存在或已删除的用户时抛出
     */
    @Override
    public void updateNicknameAndPhone(@NotNull Long userId, @Nullable Nickname nickname, @Nullable PhoneNumber phone) {
        UpdateChain<UserAccountPO> chain = UpdateChain.of(accountMapper)
                .where(UserAccountPO::getId).eq(userId)
                .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE);

        boolean needUpdate = false;
        if (nickname != null) {
            chain.set(UserAccountPO::getNickname, nickname.getValue());
            needUpdate = true;
        }
        if (phone != null) {
            chain.set(UserAccountPO::getPhone, phone.getValue());
            needUpdate = true;
        }
        if (!needUpdate)
            // 无字段需要更新, 直接返回
            return;

        if (!chain.update())
            throw new IllegalStateException("更新失败, 用户不存在或已删除");
    }


    /**
     * 更新指定用户的邮箱地址
     *
     * @param userId   用户ID, 必须提供
     * @param newEmail 新的邮箱地址, 必须提供
     * @throws IllegalStateException 当尝试更新不存在或已删除的用户时抛出
     */
    @Override
    public void updateEmail(@NotNull Long userId, @NotNull EmailAddress newEmail) {
        boolean updated;
        try {
            updated = UpdateChain.of(accountMapper)
                    .set(UserAccountPO::getEmail, newEmail.getValue())
                    .where(UserAccountPO::getId).eq(userId)
                    .and(UserAccountPO::getIsDeleted).eq(Boolean.FALSE)
                    .update();
        } catch (DataIntegrityViolationException ex) {
            // 与并发竞争或唯一约束冲突对齐
            throw new IllegalParamException("邮箱已被使用");
        }
        if (!updated)
            throw new IllegalStateException("更新失败: 用户不存在或已删除");
    }

    /**
     * 更新本地密码哈希
     *
     * @param userId          用户ID
     * @param newPasswordHash 新的密码哈希
     */
    @Override
    public void updateLocalPassword(@NotNull Long userId, @NotNull String newPasswordHash) {
        boolean updated = UpdateChain.of(authMapper)
                .set(UserAuthPO::getPasswordHash, newPasswordHash)
                .where(UserAuthPO::getUserId).eq(userId)
                .and(UserAuthPO::getProvider).eq(AuthProvider.LOCAL.name())
                .update();
        if (!updated)
            throw new IllegalStateException("更新失败: 不存在 LOCAL 绑定或用户不存在");
    }

    /**
     * 插入或更新用户资料信息. 如果指定的用户ID不存在, 则插入新的用户资料, 如果存在, 则更新现有资料
     *
     * @param userId  用户ID, 用于识别用户
     * @param profile 用户资料对象, 包含了用户的显示名称, 头像URL, 性别, 生日, 国家, 省份, 城市, 地址行, 邮政编码以及额外信息等
     * @throws IllegalArgumentException 如果传入的参数不符合要求 (例如, userId 为 null, 或者 profile 为 null)
     */
    @Override
    public void upsertProfile(@NotNull Long userId, @NotNull UserProfile profile) {
        // 先取一次, 判断是否存在
        UserProfilePO existed = profileMapper.selectOneById(userId);

        String gender = profile.getGender().name();
        if (existed == null) {
            // 插入
            UserProfilePO toInsert = new UserProfilePO(
                    userId,
                    profile.getDisplayName(),
                    profile.getAvatarUrl(),
                    gender,
                    profile.getBirthday(),
                    profile.getCountry(),
                    profile.getProvince(),
                    profile.getCity(),
                    profile.getAddressLine(),
                    profile.getZipcode(),
                    toJsonOrNull(profile.getExtra()),
                    null, // created_at 交由数据库默认
                    null  // updated_at 交由数据库默认
            );
            profileMapper.insertSelective(toInsert);
        } else {
            // 更新 (仅设置可变字段, created_at/updated_at 交由 DB 维护)
            UpdateChain.of(profileMapper)
                    .set(UserProfilePO::getDisplayName, profile.getDisplayName())
                    .set(UserProfilePO::getAvatarUrl, profile.getAvatarUrl())
                    .set(UserProfilePO::getGender, gender)
                    .set(UserProfilePO::getBirthday, profile.getBirthday())
                    .set(UserProfilePO::getCountry, profile.getCountry())
                    .set(UserProfilePO::getProvince, profile.getProvince())
                    .set(UserProfilePO::getCity, profile.getCity())
                    .set(UserProfilePO::getAddressLine, profile.getAddressLine())
                    .set(UserProfilePO::getZipcode, profile.getZipcode())
                    .set(UserProfilePO::getExtra, toJsonOrNull(profile.getExtra()))
                    .where(UserProfilePO::getUserId).eq(userId)
                    .update();
        }
    }

    // ========================= 授权绑定 =========================

    /**
     * 根据用户 ID 列出所有绑定的授权信息
     *
     * @param userId 用户 ID, 用于查询特定用户的授权绑定信息
     * @return 返回一个包含 {@link AuthBinding} 对象的列表, 每个对象代表一条授权绑定记录
     */
    @Override
    public @NotNull List<AuthBinding> listBindingsByUserId(@NotNull Long userId) {
        List<UserAuthPO> list = authMapper.selectListByQuery(QueryWrapper.create()
                .where(UserAuthPO::getUserId).eq(userId)
                .orderBy(UserAuthPO::getId, true));
        return list.stream().map(this::toDomainAuth).toList();
    }

    /**
     * 检查是否存在由特定 issuer 和 providerUid 定义的身份验证绑定, 并且该绑定不属于指定的用户
     *
     * @param issuer        发行者标识符 不能为 null
     * @param providerUid   提供者的唯一标识符 不能为 null
     * @param excludeUserId 需要排除的用户 ID, 即检查时会忽略该用户的绑定 不能为 null
     * @return 如果存在符合条件的绑定则返回 true, 否则返回 false
     */
    @Override
    public boolean existsBindingByIssuerAndUidExcludingUser(@NotNull String issuer, @NotNull String providerUid,
                                                            @NotNull Long excludeUserId) {
        UserAuthPO po = authMapper.selectOneByQuery(QueryWrapper.create()
                .where(UserAuthPO::getIssuer).eq(issuer)
                .and(UserAuthPO::getProviderUid).eq(providerUid)
                .limit(1));
        if (po == null)
            return false;
        return !excludeUserId.equals(po.getUserId());
    }

    /**
     * <p>插入或更新用户的授权绑定信息, 如果用户对于指定的提供商已经存在授权绑定, 则更新该绑定, 否则, 插入新的授权绑定</p>
     *
     * @param userId  用户ID 必填
     * @param binding 授权绑定信息, 包含提供商(provider), 发行者(issuer), 提供商唯一标识(providerUid), 访问令牌(accessToken), 刷新令牌(refreshToken), 过期时间(expiresAt)和权限范围(scope)等 必填
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertAuthBinding(@NotNull Long userId, @NotNull AuthBinding binding) {
        AuthProvider provider = binding.getProvider();
        String issuer = binding.getIssuer();
        String providerUid = binding.getProviderUid();
        EncryptedSecret accessToken = binding.getAccessToken();
        EncryptedSecret refreshToken = binding.getRefreshToken();
        LocalDateTime expiresAt = binding.getExpiresAt();
        String scope = binding.getScope();

        UserAuthPO existed = authMapper.selectOneByQuery(QueryWrapper.create()
                .where(UserAuthPO::getUserId).eq(userId)
                .and(UserAuthPO::getProvider).eq(provider.name())
                .limit(1));

        LocalDateTime now = LocalDateTime.now();

        if (existed == null) {
            UserAuthPO toInsert = UserAuthPO.builder()
                    .userId(userId)
                    .provider(provider.name())
                    .issuer(issuer)
                    .providerUid(providerUid)
                    .passwordHash(null)
                    .accessToken(accessToken == null ? null : accessToken.getBytes())
                    .refreshToken(refreshToken == null ? null : refreshToken.getBytes())
                    .expiresAt(expiresAt)
                    .scope(scope)
                    .role(null)
                    .lastLoginAt(now)
                    .build();
            authMapper.insertSelective(toInsert);
        } else {
            UpdateChain.of(authMapper)
                    .set(UserAuthPO::getIssuer, issuer)
                    .set(UserAuthPO::getProviderUid, providerUid)
                    .set(UserAuthPO::getAccessToken, accessToken == null ? null : accessToken.getBytes())
                    .set(UserAuthPO::getRefreshToken, refreshToken == null ? null : refreshToken.getBytes())
                    .set(UserAuthPO::getExpiresAt, expiresAt)
                    .set(UserAuthPO::getScope, scope)
                    .set(UserAuthPO::getLastLoginAt, now)
                    .where(UserAuthPO::getId).eq(existed.getId())
                    .update();
        }
    }

    /**
     * 删除指定用户与特定身份验证提供者之间的绑定关系
     *
     * @param userId   用户的唯一标识符, 不能为空
     * @param provider 身份验证提供者的枚举值, 不能为空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBinding(@NotNull Long userId, @NotNull AuthProvider provider) {
        authMapper.deleteByQuery(QueryWrapper.create()
                .where(UserAuthPO::getUserId).eq(userId)
                .and(UserAuthPO::getProvider).eq(provider.name()));
    }

    // ========================= 地址管理 =========================

    /**
     * 按用户分页查询收货地址列表
     *
     * @param userId 用户 ID
     * @param offset 偏移量, 从 0 开始
     * @param limit  单页数量
     * @return 当前页的地址列表
     */
    public @NotNull List<UserAddress> listAddresses(@NotNull Long userId, int offset, int limit) {
        List<UserAddressPO> list = addressMapper.selectListByQuery(QueryWrapper.create()
                .where(UserAddressPO::getUserId).eq(userId)
                .orderBy(UserAddressPO::getIsDefault, false)
                .orderBy(UserAddressPO::getId, false)
                .limit(offset, limit));
        return list.stream().map(this::toDomainAddress).toList();
    }

    /**
     * 统计指定用户的收货地址数量
     *
     * @param userId 用户 ID
     * @return 地址总数
     */
    public long countAddresses(@NotNull Long userId) {
        return addressMapper.selectCountByQuery(QueryWrapper.create()
                .where(UserAddressPO::getUserId).eq(userId));
    }

    /**
     * 按用户 + 地址 ID 查询收货地址
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @return 若存在则返回 Optional, 否则为 empty
     */
    public @NotNull Optional<UserAddress> findAddressById(@NotNull Long userId, @NotNull Long addressId) {
        UserAddressPO po = addressMapper.selectOneByQuery(QueryWrapper.create()
                .where(UserAddressPO::getUserId).eq(userId)
                .and(UserAddressPO::getId).eq(addressId)
                .limit(1));
        if (po == null)
            return Optional.empty();
        return Optional.of(toDomainAddress(po));
    }

    /**
     * 保存用户的地址信息, 包括新增 地址 更新 地址 以及 删除 不再需要的地址
     * 此方法会根据传入的用户 ID 和地址列表来更新数据库中的记录, 确保数据库中的地址信息与传入的地址列表一致
     *
     * @param userId      用户的唯一标识符 必须非空
     * @param addressList 用户地址列表 每个元素都是 {@link UserAddress} 类型的对象 必须非空
     *                    列表中的每个 <code>UserAddress</code> 对象可以包含或不包含 id 字段 如果包含 id 字段 则尝试更新该 id 对应的已有记录
     *                    如果 id 字段为空或不存在于数据库中 则创建新记录
     * @throws IllegalArgumentException 如果 userId 或 addressList 为 null
     */
    @Transactional
    public void saveAddresses(@NotNull Long userId, @NotNull List<UserAddress> addressList) {
        // 1. 查当前 DB 中的地址
        List<UserAddressPO> existing = addressMapper.selectListByQuery(
                QueryWrapper.create().where(UserAddressPO::getUserId).eq(userId));

        // 2. 以 ID 为 key 做 diff, 决定 insert / update / delete
        Map<Long, UserAddressPO> existingMap = existing.stream()
                .collect(Collectors.toMap(UserAddressPO::getId, Function.identity()));

        // 创建映射 (key: UserAddress 实体的 HashCode, value: UserAddress)
        Map<Integer, UserAddressPO> toInsertMap = new HashMap<>();
        Map<Integer, UserAddressPO> toUpdateMap = new HashMap<>();
        Map<Integer, Long> toDeleteMap = new HashMap<>();
        for (UserAddress address : addressList) {
            if (address.getId() == null) {
                // 新增
                toInsertMap.put(address.hashCode(), toAddressPO(userId, address));
                continue;
            }

            UserAddressPO addressPO = existingMap.remove(address.getId());
            if (addressPO != null) {
                // 更新
                UserAddressPO updatedAddressPO = toAddressPO(userId, address);
                updatedAddressPO.setId(addressPO.getId());
                toUpdateMap.put(address.hashCode(), updatedAddressPO);
                continue;
            }
            // 理论上不该出现, 防御性处理
            toInsertMap.put(address.hashCode(), toAddressPO(userId, address));
        }


        // 3. 余下 existingMap 中的是 domain 里已删除而 DB 里还存在的, 按需 delete
        for (UserAddressPO toDelete : existingMap.values())
            toDeleteMap.put(toDelete.hashCode(), toDelete.getId());

        if (!toInsertMap.isEmpty())
            addressMapper.insertBatchSelective(toInsertMap.values());
        if (!toUpdateMap.isEmpty())
            toUpdateMap.values().forEach(addressMapper::update);
        if (!toDeleteMap.isEmpty())
            addressMapper.deleteBatchByIds(toDeleteMap.values());

        for (UserAddress userAddress : addressList)
            if (userAddress.getId() == null)
                userAddress.assignId(toInsertMap.get(userAddress.hashCode()).getId());
    }

    /**
     * 删除指定用户的某个地址信息
     *
     * @param userId    用户的唯一标识符, 不能为空
     * @param addressId 地址的唯一标识符, 不能为空
     * @throws IllegalParamException 如果根据提供的用户ID和地址ID未找到对应的地址记录, 则抛出此异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(@NotNull Long userId, @NotNull Long addressId) {
        int rows = addressMapper.deleteByQuery(QueryWrapper.create()
                .where(UserAddressPO::getUserId).eq(userId)
                .and(UserAddressPO::getId).eq(addressId));
        if (rows == 0)
            throw new IllegalParamException("ID 为: " + addressId + " 的地址不存在");
    }

    // ========================= 装配工具 =========================

    /**
     * 根据给定的用户账户持久化对象 <code>UserAccountPO</code>, 组装并返回一个完整的 <code>User</code> 对象封装在 <code>Optional</code> 中
     * 如果传入的 <code>UserAccountPO</code> 为 null, 则直接返回 <code>Optional.empty()</code>
     *
     * @param account 用户账户持久化对象, 包含用户的基本信息
     * @return 封装了完整用户信息 (包括但不限于基本资料, 认证绑定, 地址等) 的 <code>Optional<User></code> 实例,
     * 若无法组装或输入为空, 则返回 <code>Optional.empty()</code>
     */
    private Optional<User> assembleOptional(UserAccountPO account) {
        if (account == null)
            return Optional.empty();

        // 子表装载
        List<UserAuthPO> authList = authMapper.selectListByQuery(QueryWrapper.create()
                .where(UserAuthPO::getUserId).eq(account.getId()));
        List<UserAddressPO> addrList = addressMapper.selectListByQuery(QueryWrapper.create()
                .where(UserAddressPO::getUserId).eq(account.getId()));
        UserProfilePO profile = profileMapper.selectOneById(account.getId());

        // 映射子实体
        List<AuthBinding> bindings = authList.stream().map(this::toDomainAuth).toList();
        List<UserAddress> addresses = addrList.stream().map(this::toDomainAddress).toList();

        // Profile → 值对象 (你的 UserProfile API 若不同, 可在此适配)
        UserProfile profileVO = (profile == null) ? UserProfile.empty() : UserProfile.of(
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                parseGender(profile.getGender()),
                profile.getBirthday(),
                profile.getCountry(),
                profile.getProvince(),
                profile.getCity(),
                profile.getAddressLine(),
                profile.getZipcode(),
                parseJsonToMap(profile.getExtra())
        );

        // 还原聚合
        User user = User.reconstitute(
                account.getId(),
                Username.of(account.getUsername()),
                Nickname.of(account.getNickname()),
                EmailAddress.ofNullable(account.getEmail()),
                PhoneNumber.nullableOf(account.getPhone()),
                AccountStatus.valueOf(account.getStatus()),
                account.getLastLoginAt(),
                Boolean.TRUE.equals(account.getIsDeleted()),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                profileVO,
                bindings,
                addresses
        );
        return Optional.of(user);
    }

    /**
     * 将用户认证持久化对象转换为领域模型中的认证绑定实体
     *
     * @param authPO 用户认证持久化对象, 包含了用户认证所需的所有信息, 如提供商, 访问令牌等
     * @return 转换后的认证绑定实体, 代表了用户的某一种认证方式的详细信息
     */
    private AuthBinding toDomainAuth(UserAuthPO authPO) {
        return new AuthBinding(
                authPO.getId(),
                AuthProvider.valueOf(authPO.getProvider()),
                authPO.getIssuer(),
                authPO.getProviderUid(),
                authPO.getPasswordHash(),
                authPO.getAccessToken() == null ? null : EncryptedSecret.of(authPO.getAccessToken()),
                authPO.getRefreshToken() == null ? null : EncryptedSecret.of(authPO.getRefreshToken()),
                authPO.getExpiresAt(),
                authPO.getScope(),
                authPO.getRole(),
                authPO.getLastLoginAt(),
                authPO.getCreatedAt(),
                authPO.getUpdatedAt()
        );
    }

    /**
     * 将用户地址持久化对象转换为领域模型中的用户地址实体
     *
     * @param addressPO 用户地址持久化对象, 包含了用户地址的所有信息
     * @return 转换后的 {@link UserAddress} 实体, 代表用户的某个收货地址的详细信息
     */
    private UserAddress toDomainAddress(UserAddressPO addressPO) {
        return new UserAddress(
                addressPO.getId(),
                addressPO.getReceiverName(),
                addressPO.getPhone() == null ? null : PhoneNumber.nullableOf(addressPO.getPhone()),
                addressPO.getCountry(),
                addressPO.getProvince(),
                addressPO.getCity(),
                addressPO.getDistrict(),
                addressPO.getAddressLine1(),
                addressPO.getAddressLine2(),
                addressPO.getZipcode(),
                Boolean.TRUE.equals(addressPO.getIsDefault()),
                addressPO.getCreatedAt(),
                addressPO.getUpdatedAt()
        );
    }

    /**
     * 将 <code>UserAddress</code> 对象转换为 <code>UserAddressPO</code> 对象
     *
     * @param userId  用户的唯一标识符
     * @param address 需要被转换的 <code>UserAddress</code> 对象
     * @return 转换后的 <code>UserAddressPO</code> 对象
     */
    private UserAddressPO toAddressPO(Long userId, UserAddress address) {
        return UserAddressPO.builder()
                .userId(userId)
                .receiverName(address.getReceiverName())
                .phone(address.getPhone() == null ? null : address.getPhone().getValue())
                .country(address.getCountry())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .zipcode(address.getZipcode())
                .isDefault(address.isDefaultAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    /**
     * 将原始字符串解析为性别枚举值
     *
     * @param raw 原始字符串, 代表性别的英文表示, 如 "MALE", "FEMALE" 或 "UNKNOWN"
     * @return 解析后的 {@link Gender} 枚举值, 如果输入为空或无法匹配任何已定义的枚举, 则返回 {@link Gender#UNKNOWN UNKNOWN}
     */
    private static Gender parseGender(String raw) {
        if (raw == null || raw.isBlank())
            return Gender.UNKNOWN;
        try {
            return Gender.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Gender.UNKNOWN;
        }
    }

    /**
     * 将 JSON 字符串解析为键值对映射
     *
     * @param json 待解析的 JSON 字符串, 可以是任何有效的 JSON 格式字符串, 如果为空或空白, 则返回空映射
     * @return 解析后的键值对映射, 其中键为字符串, 值可以是任意类型, 如果解析失败, 也返回空映射
     */
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank())
            return Map.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("解析 JSON 字符串失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 将给定的键值对映射转换为 JSON 字符串, 如果映射为空或转换过程中发生异常, 则返回 null.
     *
     * @param map 待转换的键值对映射, 其中键为字符串类型, 值可以是任意对象. 如果 <code>map</code> 为 null 或空, 方法将直接返回 null.
     * @return 转换后的 JSON 字符串, 如果输入映射为空, 或在转换过程中遇到任何异常, 则返回 null.
     */
    private String toJsonOrNull(java.util.Map<String, Object> map) {
        if (map == null || map.isEmpty())
            return null;
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            // 失败时可以选择返回 null 或抛出受检异常, 这里回退为 null
            log.error("转换 JSON 字符串失败: {}", e.getMessage());
            return null;
        }
    }
}

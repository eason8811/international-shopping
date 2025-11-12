package shopping.international.domain.adapter.repository.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.enums.user.AccountStatus;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户聚合的仓储接口 (面向领域层), 负责对 {@code user_account / user_auth / user_profile / user_address} 等
 * 多表的组合装配与持久化, 具体 ORM/SQL 由基础设施层实现
 *
 * <p><b>注意: </b>该接口设计为“一站式”聚合仓储, 屏蔽聚合内多实体 (含绑定) 的物理落地细节</p>
 */
public interface IUserRepository {

    /**
     * 按登录账号查询用户 (账号为用户名 / 邮箱 / 手机号), 返回完整聚合快照 (含绑定)
     *
     * @param account 用户名 / 邮箱 / 手机号
     * @return 用户聚合, 可为空
     */
    Optional<User> findByLoginAccount(@NotNull String account);

    /**
     * 按邮箱精确查询用户 (用于激活等流程), 返回完整聚合快照
     *
     * @param email 邮箱
     * @return 用户聚合, 可为空
     */
    Optional<User> findByEmail(@NotNull EmailAddress email);

    /**
     * 按主键查询用户
     *
     * @param userId 用户 ID
     * @return 用户聚合, 可为空
     */
    Optional<User> findById(@NotNull Long userId);

    /**
     * 检查用户名是否已存在 (幂等注册前置唯一性校验)
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(@NotNull Username username);

    /**
     * 检查邮箱是否已存在 (幂等注册前置唯一性校验)
     *
     * @param email 邮箱
     * @return 是否存在 (忽略 null)
     */
    boolean existsByEmail(@NotNull EmailAddress email);

    /**
     * 检查手机号是否已存在 (幂等注册前置唯一性校验)
     *
     * @param phone 手机号
     * @return 是否存在 (忽略 null)
     */
    boolean existsByPhone(@NotNull PhoneNumber phone);

    /**
     * 按第三方身份 (issuer + provider_uid/sub) 查询用户
     *
     * @param issuer      OIDC iss (或等价发行方标识)
     * @param providerUid OIDC sub (发行方内用户唯一 ID)
     * @return 用户聚合
     */
    @NotNull Optional<User> findByProviderUid(@NotNull String issuer, @NotNull String providerUid);

    /**
     * 持久化一个全新的用户聚合 (账户 + 绑定 + 资料 + 地址)
     *
     * <p>要求 {@code user.id == null}, 至少存在一种登录方式
     * 若存在 LOCAL 绑定, 则其 passwordHash 必须非空
     * 成功后返回带持久化主键的快照 (含各子实体主键)</p>
     *
     * @param user 待保存的新用户对象, 包含用户账户、绑定信息、资料及地址等
     * @return 保存后的用户对象, 包括由数据库生成的主键和其他相关字段
     */
    @NotNull User saveNewUserWithBindings(User user);

    /**
     * 更新账户状态 (如 DISABLED → ACTIVE)
     *
     * @param userId 用户 ID
     * @param status 新状态
     */
    void updateStatus(@NotNull Long userId, @NotNull AccountStatus status);

    /**
     * 记录登录时间戳与通道最近登录时间 (如 provider=LOCAL)
     *
     * @param userId    用户 ID
     * @param provider  认证提供方
     * @param loginTime 登录时间
     */
    void recordLogin(@NotNull Long userId, @NotNull AuthProvider provider, @NotNull LocalDateTime loginTime);

    /**
     * 检查是否存在手机号相同的其他用户, 排除指定的用户 ID
     *
     * @param userId 用户 ID 需要排除的用户 ID, 用于避免查询到当前用户自己
     * @param phone  手机号 要检查的手机号, 必须是有效的 {@link PhoneNumber} 对象
     * @return 是否存在 如果存在其他用户的手机号与给定的手机号相同, 返回 true; 否则返回 false
     */
    boolean existsByPhoneExceptUser(@NotNull Long userId, @NotNull PhoneNumber phone);

    /**
     * 更新指定用户ID的昵称和手机号
     *
     * @param userId   用户ID, 必须提供
     * @param nickname 新的昵称, 可以是 {@code null}, 如果为 {@code null} 则不更新昵称
     * @param phone    新的手机号, 可以是 {@code null}, 如果为 {@code null} 则不更新手机号
     * @throws IllegalStateException 当尝试更新不存在或已删除的用户时抛出
     */
    void updateNicknameAndPhone(@NotNull Long userId, @Nullable Nickname nickname, @Nullable PhoneNumber phone);

    /**
     * 更新指定用户的邮箱地址
     *
     * @param userId   用户ID, 必须提供
     * @param newEmail 新的邮箱地址, 必须提供
     * @throws IllegalStateException 当尝试更新不存在或已删除的用户时抛出
     */
    void updateEmail(@NotNull Long userId, @NotNull EmailAddress newEmail);

    /**
     * 插入或更新用户资料信息. 如果指定的用户ID不存在, 则插入新的用户资料, 如果存在, 则更新现有资料
     *
     * @param userId  用户ID, 用于识别用户
     * @param profile 用户资料对象, 包含了用户的显示名称, 头像URL, 性别, 生日, 国家, 省份, 城市, 地址行, 邮政编码以及额外信息等
     * @throws IllegalArgumentException 如果传入的参数不符合要求 (例如, userId 为 null, 或者 profile 为 null)
     */
    void upsertProfile(@NotNull Long userId, @NotNull UserProfile profile);
}
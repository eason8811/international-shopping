package shopping.international.domain.model.aggregate.user;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.enums.user.AccountStatus;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户聚合根, 对应 {@code user_account} 为主表, 并聚合:
 * <ul>
 *     <li>认证映射 {@code user_auth, 1:N}</li>
 *     <li>资料 {@code user_profile, 1:1, 建模为值对象}</li>
 *     <li>收货地址 {@code user_address, 1:N}</li>
 * </ul>
 *
 * <p>聚合职责: </p>
 * <ul>
 *     <li>维护账户不变式: 用户名、昵称、邮箱/手机格式, 状态切换 (激活/禁用)</li>
 *     <li>维护绑定不变式: 同一用户至少有一种登录方式；同 provider 唯一；issuer+providerUid 唯一</li>
 *     <li>维护地址不变式: 同一用户仅允许一个默认地址</li>
 * </ul>
 */
@Getter
@ToString
@Accessors(chain = true)
public class User {
    // ========== 标识与账户主信息 ==========
    /**
     * 主键ID (可为 null 表示未持久化)
     */
    private Long id;
    /**
     * 用户名 (登录名, 唯一)
     */
    private Username username;
    /**
     * 昵称/显示名
     */
    private Nickname nickname;
    /**
     * 邮箱 (可空)
     */
    private EmailAddress email;
    /**
     * 手机号 (可空)
     */
    private PhoneNumber phone;
    /**
     * 账户状态 (默认 {@link AccountStatus#DISABLED DISABLED})
     */
    private AccountStatus status;
    /**
     * 最近登录时间 (可空)
     */
    private LocalDateTime lastLoginAt;
    /**
     * 软删除标记
     */
    private boolean deleted;
    /**
     * 创建时间 (快照)
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间 (快照)
     */
    private LocalDateTime updatedAt;

    // ========== 聚合内对象 ==========
    /**
     * 认证映射列表 (LOCAL/OAuth2)
     */
    private final List<AuthBinding> bindingList = new ArrayList<>();
    /**
     * 用户资料 (值对象, 1:1)
     */
    private UserProfile profile;
    /**
     * 收货地址 (1:N)
     */
    private final List<UserAddress> addressList = new ArrayList<>();

    // ========== 构造与工厂 ==========

    /**
     * 用户实体的私有构造方法, 用于防止外部直接实例化
     * <p>用户实体应当通过工厂方法或静态方法创建</p>
     */
    private User() {
    }

    /**
     * 用户实体的私有构造方法, 用于防止外部直接实例化
     * <p>用户实体应当通过工厂方法或静态方法创建</p>
     *
     * @param id          用户唯一标识符
     * @param username    用户名, 必须非空
     * @param nickname    昵称, 必须非空
     * @param email       邮箱地址, 可为空
     * @param phone       手机号码, 可为空
     * @param status      账户状态, 如果为 null, 默认设置为 {@link AccountStatus#DISABLED DISABLED}
     * @param lastLoginAt 上次登录时间, 可为空
     * @param deleted     是否已删除 (软删除标记), 默认为 false
     * @param createdAt   创建时间, 可为空
     * @param updatedAt   更新时间, 可为空
     * @param profile     用户资料, 如果为 null, 默认设置为 UserProfile.empty()
     */
    private User(Long id, Username username, Nickname nickname, EmailAddress email, PhoneNumber phone,
                 AccountStatus status, LocalDateTime lastLoginAt, boolean deleted, LocalDateTime createdAt,
                 LocalDateTime updatedAt, UserProfile profile) {
        requireNotNull(username, "用户名不能为空");
        requireNotNull(nickname, "昵称不能为空");
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.status = status == null ? AccountStatus.DISABLED : status;
        this.lastLoginAt = lastLoginAt;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.profile = profile == null ? UserProfile.empty() : profile;
    }

    /**
     * 注册工厂, 创建一个新用户 (DISABLED), 并附带本地账号绑定
     *
     * @param username          用户名
     * @param nickname          昵称
     * @param email             邮箱 (可空)
     * @param phone             手机 (可空)
     * @param localPasswordHash 本地登录密码哈希
     * @return 新的聚合根实例
     */
    public static User register(Username username, Nickname nickname, EmailAddress email, PhoneNumber phone, String localPasswordHash) {
        // 创建新用户
        User user = new User(null, username, nickname, email, phone, AccountStatus.DISABLED, null,
                false, LocalDateTime.now(), LocalDateTime.now(), UserProfile.empty());
        // 绑定 LOCAL 登录方式
        user.addBinding(AuthBinding.local(localPasswordHash));
        return user;
    }

    /**
     * 从持久化层重建 {@link  User} 聚合 (供仓储使用)
     *
     * @param id          用户唯一标识符
     * @param username    用户名, 必须非空
     * @param nickname    昵称, 必须非空
     * @param email       邮箱地址, 可为空
     * @param phone       手机号码, 可为空
     * @param status      账户状态, 如果为 null, 默认设置为 {@link AccountStatus#DISABLED DISABLED}
     * @param lastLoginAt 上次登录时间, 可为空
     * @param deleted     是否已删除 (软删除标记), 默认为 false
     * @param createdAt   创建时间, 可为空
     * @param updatedAt   更新时间, 可为空
     * @param profile     用户资料, 如果为 null, 默认设置为空资料 {@link UserProfile#empty()}
     * @param bindings    认证绑定列表, 可为空
     * @param addresses   用户地址列表, 可为空
     * @return 重建后的 User 对象
     */
    public static User reconstitute(Long id, Username username, Nickname nickname, EmailAddress email, PhoneNumber phone,
                                    AccountStatus status, LocalDateTime lastLoginAt, boolean deleted,
                                    LocalDateTime createdAt, LocalDateTime updatedAt, UserProfile profile,
                                    List<AuthBinding> bindings, List<UserAddress> addresses) {
        User user = new User(id, username, nickname, email, phone, status, lastLoginAt, deleted, createdAt, updatedAt, profile);
        if (bindings != null)
            user.bindingList.addAll(bindings);
        if (addresses != null) {
            user.addressList.addAll(addresses);
            user.ensureSingleDefaultAddress();
        }
        user.ensureHasAtLeastOneLoginMethod();
        return user;
    }

    // ========== 账户行为 ==========

    /**
     * 幂等激活账户 (从 DISABLED → ACTIVE)
     */
    public void activate() {
        if (this.status == AccountStatus.ACTIVE)
            return;
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * 幂等禁用账户 (从 ACTIVE → DISABLED)
     */
    public void disable() {
        if (this.status == AccountStatus.DISABLED)
            return;
        this.status = AccountStatus.DISABLED;
    }

    /**
     * 修改用户的昵称
     *
     * @param newNickname 新的昵称, 必须非空
     * @throws IllegalParamException 如果 <code>newNickname</code> 为 <code>null</code>
     */
    public void changeNickname(Nickname newNickname) {
        requireNotNull(newNickname, "昵称不能为空");
        this.nickname = newNickname;
    }

    /**
     * 修改用户的邮箱地址 (需由应用层完成验证码校验后再调用)
     *
     * <p>允许将邮箱设置为 <code>null</code>, 以清空邮箱信息</p>
     *
     * @param newEmail 新的邮箱地址, 可以为 <code>null</code>
     */
    public void changeEmail(EmailAddress newEmail) {
        this.email = newEmail; // 可 null → 允许清空
    }

    /**
     * 修改用户的手机号码 (需由应用层完成验证码校验后再调用)
     *
     * @param newPhone 新的 {@link PhoneNumber} 实例, 用于更新用户当前的手机号码
     */
    public void changePhone(PhoneNumber newPhone) {
        this.phone = newPhone;
    }

    /**
     * 记录用户的登录信息, 更新最后一次登录时间, 并根据认证提供者更新相应的认证绑定的登录时间
     *
     * @param authProvider 用于此次登录的认证提供者, 可以为 null
     * @param loginTime    用户登录的具体时间, 如果为 null 则使用当前时间
     */
    public void recordLogin(AuthProvider authProvider, LocalDateTime loginTime) {
        LocalDateTime ts = loginTime == null ? LocalDateTime.now() : loginTime;
        this.lastLoginAt = ts;
        if (authProvider != null) {
            this.bindingList.stream()
                    .filter(binding -> binding.getProvider() == authProvider)
                    .findFirst()
                    .ifPresent(binding -> binding.recordLogin(ts));
        }
    }

    // ========== 绑定行为 ==========

    /**
     * 增加一个认证绑定
     * <ul>
     *     <li>同 provider 不得重复 (LOCAL 唯一)</li>
     *     <li>OAuth2: issuer + providerUid 在本聚合内唯一</li>
     * </ul>
     */
    public void addBinding(AuthBinding binding) {
        requireNotNull(binding, "绑定信息不能为空");
        // 从认证绑定列表中查找同 provider 的绑定
        boolean providerExists = bindingList.stream()
                .anyMatch(b -> b.getProvider() == binding.getProvider());
        // 如果需要添加的认证绑定为 LOCAL, 且已存在 LOCAL 绑定, 则抛出异常
        if (providerExists && binding.getProvider() == AuthProvider.LOCAL)
            throw new IllegalParamException("LOCAL 认证绑定已经存在");

        // 如果需要添加的认证绑定为 OAuth2, 且已存在相同 issuer+providerUid 绑定, 则抛出异常
        if (binding.getProvider() != AuthProvider.LOCAL) {
            boolean isDuplicate = bindingList.stream()
                    .anyMatch(b -> Objects.equals(b.getIssuer(), binding.getIssuer()) && Objects.equals(b.getProviderUid(), binding.getProviderUid()));
            if (isDuplicate)
                throw new IllegalParamException("OAuth2 认证绑定已存在, (issuer, providerUid) 重复");
        }
        this.bindingList.add(binding);
        ensureHasAtLeastOneLoginMethod();
    }

    /**
     * 从用户认证绑定列表中移除指定的认证提供者绑定
     *
     * <p>不允许解绑唯一的登录方式</p>
     *
     * @param provider 要移除的认证提供者的类型, 如 {@code LOCAL, GOOGLE} 等
     * @throws IllegalParamException 如果移除所有给定类型的认证绑定之后, 用户没有任何可用的登录方式
     */
    public void removeBinding(AuthProvider provider) {
        long remainIfRemoved = bindingList.stream()
                .filter(binding -> binding.getProvider() != provider)
                .count();
        if (remainIfRemoved == 0)
            throw new IllegalParamException("不允许解绑最后一个登录方式");
        bindingList.removeIf(binding -> binding.getProvider() == provider);
    }


    /**
     * 修改用户的本地密码哈希
     *
     * @param newPasswordHash 新的密码哈希, 必须非空
     * @throws IllegalParamException 如果找不到 LOCAL 绑定
     */
    public void changeLocalPassword(String newPasswordHash) {
        AuthBinding localBinding = bindingList.stream()
                .filter(b -> b.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("不存在 LOCAL 绑定"));
        localBinding.changeLocalPassword(newPasswordHash);
    }

    // ========== 地址行为 ==========

    /**
     * 向用户的地址列表中添加一个新的地址
     *
     * @param address 待添加的 {@link UserAddress} 实例, 必须非空 如果该地址被设置为默认地址, 则会将用户现有的所有其他默认地址取消
     */
    public void addAddress(UserAddress address) {
        requireNotNull(address, "地址信息不能为空");
        if (address.isDefaultAddress())
            // 置为唯一默认
            addressList.forEach(a -> a.setDefault(false));
        addressList.add(address);
        ensureSingleDefaultAddress();
    }

    /**
     * 更新用户的指定地址信息 (按 id 定位), 包括收件人姓名、电话号码、国家、省份、城市、区县、详细地址以及邮政编码
     * 如果设置了 <code>makeDefault</code> 为 <code>true</code>, 则会将该地址设为默认地址, 并取消其他所有地址的默认状态
     *
     * @param addressId    地址的唯一标识符
     * @param receiverName 收件人姓名
     * @param phone        手机号, 必须符合 E.164 格式
     * @param country      国家名称
     * @param province     省份或州名
     * @param city         城市名
     * @param district     区县名
     * @param addressLine1 第一行地址
     * @param addressLine2 第二行地址 (可选)
     * @param zipcode      邮政编码
     * @param makeDefault  是否将此地址设为默认地址
     * @throws IllegalParamException 如果根据提供的ID找不到任何地址, 或手机号格式不正确时抛出
     */
    public void updateAddress(Long addressId, String receiverName, PhoneNumber phone, String country, String province,
                              String city, String district, String addressLine1, String addressLine2, String zipcode,
                              Boolean makeDefault) {
        UserAddress address = findAddress(addressId);
        address.update(receiverName, phone, country, province, city, district, addressLine1, addressLine2, zipcode);
        if (Boolean.TRUE.equals(makeDefault)) {
            addressList.forEach(a -> a.setDefault(false));
            address.setDefault(true);
        }
        ensureSingleDefaultAddress();
    }

    /**
     * 从用户的地址列表中移除指定的地址 (按 id 定位)
     *
     * @param addressId 待移除地址的唯一标识符
     * @throws IllegalParamException 如果根据提供的ID找不到任何地址, 则抛出此异常
     */
    public void removeAddress(Long addressId) {
        boolean removed = addressList.removeIf(address -> Objects.equals(address.getId(), addressId));
        if (!removed)
            throw new IllegalParamException("ID 为: " + addressId + " 的地址不存在");
        // 默认地址唯一性仍成立
        ensureSingleDefaultAddress();
    }

    /**
     * 将指定的地址设置为用户的默认地址
     *
     * <p>该方法首先查找给定ID对应的地址, 然后将用户的所有其他地址的默认状态取消, 最后将查找到的地址设为默认</p>
     *
     * @param addressId 待设置为默认地址的唯一标识符
     * @throws IllegalParamException 如果根据提供的ID找不到任何地址, 则抛出此异常
     */
    public void setDefaultAddress(Long addressId) {
        UserAddress address = findAddress(addressId);
        addressList.forEach(a -> a.setDefault(false));
        address.setDefault(true);
        ensureSingleDefaultAddress();
    }

    /**
     * 获取用户的默认地址
     *
     * @return 返回找到的默认 {@link UserAddress} 对象, 如果没有默认地址则返回 null
     */
    public UserAddress getDefaultAddress() {
        return addressList.stream().filter(UserAddress::isDefaultAddress).findFirst().orElse(null);
    }

    /**
     * 获取用户地址列表的只读快照
     *
     * @return 返回一个不可修改的 {@link UserAddress} 列表, 代表当前用户的地址信息快照
     */
    public List<UserAddress> getAddressesSnapshot() {
        return Collections.unmodifiableList(addressList);
    }

    /**
     * 获取用户认证绑定列表的只读快照
     *
     * @return 返回一个不可修改的 {@link AuthBinding} 列表, 代表当前用户的认证绑定信息快照
     */
    public List<AuthBinding> getBindingsSnapshot() {
        return Collections.unmodifiableList(bindingList);
    }

    // ========== 资料行为 ==========

    /**
     * 更新用户资料信息
     *
     * <p>此方法允许更新用户的个人资料, 包括但不限于头像、简介等. 如果传入的 {@code newProfile} 为 {@code null},
     * 则会将用户的资料设置为空资料 ({@link UserProfile#empty()})</p>
     *
     * @param newProfile 新的用户资料实例, 可以为 {@code null}
     */
    public void updateProfile(UserProfile newProfile) {
        this.profile = Objects.requireNonNullElseGet(newProfile, UserProfile::empty);
    }

    /**
     * 修改用户的显示名称
     *
     * <p>此方法允许用户更新其个人资料中的显示名称, 如果新的显示名称为空字符串, 则会抛出 {@link IllegalParamException} 异常</p>
     *
     * @param name 新的显示名称
     * @throws IllegalParamException 如果新的显示名称为空字符串
     */
    public void changeDisplayName(String name) {
        this.profile = this.profile.withDisplayName(name);
    }

    // ========== 内部工具 ==========

    /**
     * 根据给定的地址ID查找用户的特定地址信息
     *
     * @param id 地址的唯一标识符, 用于定位用户地址列表中的特定地址
     * @return 返回找到的 {@link UserAddress} 对象 如果没有找到对应的地址, 则抛出异常
     * @throws IllegalParamException 如果根据提供的ID找不到任何地址, 则抛出此异常
     */
    private UserAddress findAddress(Long id) {
        return addressList.stream()
                .filter(address -> Objects.equals(address.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("ID 为: " + id + " 的地址不存在"));
    }

    /**
     * 确保存在至少一种登录方式
     */
    private void ensureHasAtLeastOneLoginMethod() {
        if (bindingList.isEmpty())
            throw new IllegalParamException("用户必须绑定至少一种登录方式");
    }

    /**
     * 确保用户地址列表中只有一个默认地址
     * <p>
     * 如果发现存在多个默认地址, 则保留第一个默认地址, 并将其他地址的默认状态取消
     * 若没有或只有一个默认地址, 则直接返回不做任何修改
     */
    private void ensureSingleDefaultAddress() {
        List<UserAddress> defaultAddressList = addressList.stream()
                .filter(UserAddress::isDefaultAddress)
                .toList();
        if (defaultAddressList.size() <= 1)
            return;
        // 保留最早的那个默认, 其他置为非默认
        boolean first = true;
        for (UserAddress address : defaultAddressList) {
            if (first) {
                first = false;
                continue;
            }
            address.setDefault(false);
        }
    }

    // ========== 便捷方法 (可供仓储等调用) ==========

    /**
     * 禁止删除: 使用软删除标记
     */
    public void softDelete() {
        this.deleted = true;
        this.status = AccountStatus.DISABLED;
    }

    /**
     * 恢复软删除
     */
    public void restore() {
        this.deleted = false;
    }
}

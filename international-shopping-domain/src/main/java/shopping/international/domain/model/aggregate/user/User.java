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

/**
 * 用户聚合根，对应 user_account 为主表，并聚合：
 * <ul>
 *     <li>认证映射（user_auth，1:N）</li>
 *     <li>资料（user_profile，1:1，建模为值对象）</li>
 *     <li>收货地址（user_address，1:N）</li>
 * </ul>
 *
 * <p>聚合职责：</p>
 * <ul>
 *     <li>维护账户不变式：用户名、昵称、邮箱/手机格式，状态切换（激活/禁用）</li>
 *     <li>维护绑定不变式：同一用户至少有一种登录方式；同 provider 唯一；issuer+providerUid 唯一</li>
 *     <li>维护地址不变式：同一用户仅允许一个默认地址</li>
 * </ul>
 *
 * <p>持久化说明：ORM/Mapper 在重建时可使用包可见构造器；本类提供行为方法用于业务修改。</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class User {

    // ========== 标识与账户主信息 ==========
    /**
     * 主键ID（可为 null 表示未持久化）
     */
    private Long id;

    /**
     * 用户名（登录名，唯一）
     */
    private Username username;

    /**
     * 昵称/显示名
     */
    private Nickname nickname;

    /**
     * 邮箱（可空）
     */
    private EmailAddress email;

    /**
     * 手机号（可空）
     */
    private PhoneNumber phone;

    /**
     * 账户状态（默认 DISABLED）
     */
    private AccountStatus status;

    /**
     * 最近登录时间（可空）
     */
    private LocalDateTime lastLoginAt;

    /**
     * 软删除标记
     */
    private boolean deleted;

    /**
     * 创建与更新时间（快照）
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== 聚合内对象 ==========
    /**
     * 认证映射列表（LOCAL/OAuth2）
     */
    private final List<AuthBinding> bindings = new ArrayList<>();

    /**
     * 用户资料（值对象，1:1）
     */
    private UserProfile profile;

    /**
     * 收货地址（1:N）
     */
    private final List<UserAddress> addresses = new ArrayList<>();

    // ========== 构造与工厂 ==========
    private User() {
    }

    private User(Long id, Username username, Nickname nickname,
                 EmailAddress email, PhoneNumber phone,
                 AccountStatus status, LocalDateTime lastLoginAt,
                 boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt,
                 UserProfile profile) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.nickname = Objects.requireNonNull(nickname, "nickname");
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
     * 注册工厂：创建一个新用户（DISABLED），并附带本地账号绑定。
     *
     * @param username          用户名
     * @param nickname          昵称
     * @param email             邮箱（可空）
     * @param phone             手机（可空）
     * @param localPasswordHash 本地登录密码哈希
     * @return 新的聚合根实例
     */
    public static User register(Username username, Nickname nickname,
                                EmailAddress email, PhoneNumber phone,
                                String localPasswordHash) {
        User u = new User(null, username, nickname, email, phone,
                AccountStatus.DISABLED, null, false,
                LocalDateTime.now(), LocalDateTime.now(), UserProfile.empty());
        // 绑定 LOCAL 登录方式
        u.addBinding(AuthBinding.local(localPasswordHash));
        return u;
    }

    /**
     * 从持久化层重建聚合（供仓储使用）。
     */
    public static User reconstitute(Long id, Username username, Nickname nickname,
                                    EmailAddress email, PhoneNumber phone, AccountStatus status,
                                    LocalDateTime lastLoginAt, boolean deleted,
                                    LocalDateTime createdAt, LocalDateTime updatedAt,
                                    UserProfile profile,
                                    List<AuthBinding> bindings,
                                    List<UserAddress> addresses) {
        User u = new User(id, username, nickname, email, phone, status,
                lastLoginAt, deleted, createdAt, updatedAt, profile);
        if (bindings != null) u.bindings.addAll(bindings);
        if (addresses != null) {
            u.addresses.addAll(addresses);
            u.ensureSingleDefaultAddress();
        }
        u.ensureHasAtLeastOneLoginMethod();
        return u;
    }

    // ========== 账户行为 ==========

    /**
     * 激活账户（从 DISABLED → ACTIVE）。
     */
    public void activate() {
        if (this.status == AccountStatus.ACTIVE) return;
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * 禁用账户（从 ACTIVE → DISABLED）。
     */
    public void disable() {
        if (this.status == AccountStatus.DISABLED) return;
        this.status = AccountStatus.DISABLED;
    }

    /**
     * 修改昵称。
     */
    public void changeNickname(Nickname newNickname) {
        this.nickname = Objects.requireNonNull(newNickname, "nickname");
    }

    /**
     * 修改邮箱（需由应用层完成验证码校验后再调用）。
     */
    public void changeEmail(EmailAddress newEmail) {
        this.email = newEmail; // 可 null → 允许清空
    }

    /**
     * 修改手机号（可空）。
     */
    public void changePhone(PhoneNumber newPhone) {
        this.phone = newPhone;
    }

    /**
     * 记录一次登录成功（更新最后登录时间，并让指定通道记录登录）。
     *
     * @param viaProvider 登录通道（可为 null 表示未知）
     * @param when        时间（null 则取 now）
     */
    public void recordLogin(AuthProvider viaProvider, LocalDateTime when) {
        LocalDateTime ts = when == null ? LocalDateTime.now() : when;
        this.lastLoginAt = ts;
        if (viaProvider != null) {
            this.bindings.stream()
                    .filter(b -> b.getProvider() == viaProvider)
                    .findFirst()
                    .ifPresent(b -> b.recordLogin(ts));
        }
    }

    // ========== 绑定行为 ==========

    /**
     * 增加一个认证绑定。
     * <ul>
     *     <li>同 provider 不得重复（LOCAL 唯一）</li>
     *     <li>OAuth2：issuer + providerUid 在本聚合内唯一</li>
     * </ul>
     */
    public void addBinding(AuthBinding binding) {
        Objects.requireNonNull(binding, "binding");
        boolean providerExists = bindings.stream()
                .anyMatch(b -> b.getProvider() == binding.getProvider());
        if (providerExists && binding.getProvider() == AuthProvider.LOCAL) {
            throw new IllegalParamException("LOCAL binding already exists");
        }
        if (binding.getProvider() != AuthProvider.LOCAL) {
            boolean dup = bindings.stream().anyMatch(b ->
                    Objects.equals(b.getIssuer(), binding.getIssuer())
                            && Objects.equals(b.getProviderUid(), binding.getProviderUid()));
            if (dup) {
                throw new IllegalParamException("OAuth2 binding already exists (issuer+providerUid)");
            }
        }
        this.bindings.add(binding);
        ensureHasAtLeastOneLoginMethod();
    }

    /**
     * 解绑一个认证通道。
     * <p>不允许解绑唯一的登录方式。</p>
     */
    public void removeBinding(AuthProvider provider) {
        int before = bindings.size();
        bindings.removeIf(b -> b.getProvider() == provider);
        if (before == bindings.size()) {
            // 未删除任何东西
            return;
        }
        ensureHasAtLeastOneLoginMethod();
    }

    /**
     * 修改本地密码。
     */
    public void changeLocalPassword(String newPasswordHash) {
        AuthBinding local = bindings.stream()
                .filter(b -> b.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("LOCAL binding not found"));
        local.changeLocalPassword(newPasswordHash);
    }

    // ========== 地址行为 ==========

    /**
     * 新增地址。若新增地址标记为默认，则清除其他默认。
     */
    public void addAddress(UserAddress addr) {
        Objects.requireNonNull(addr, "address");
        if (addr.isDefaultAddress()) {
            // 置为唯一默认
            addresses.forEach(a -> a.setDefault(false));
        }
        addresses.add(addr);
        ensureSingleDefaultAddress();
    }

    /**
     * 修改地址明细（按 id 定位）。若修改为默认，清除其他默认。
     */
    public void updateAddress(Long addressId, String receiverName, PhoneNumber phone,
                              String country, String province, String city, String district,
                              String addressLine1, String addressLine2, String zipcode,
                              Boolean makeDefault) {
        UserAddress addr = findAddress(addressId);
        addr.update(receiverName, phone, country, province, city, district, addressLine1, addressLine2, zipcode);
        if (Boolean.TRUE.equals(makeDefault)) {
            addresses.forEach(a -> a.setDefault(false));
            addr.setDefault(true);
        }
        ensureSingleDefaultAddress();
    }

    /**
     * 删除地址（按 id）。若删除的是默认地址则不做强制迁移，交给前端或调用方另行设置。
     */
    public void removeAddress(Long addressId) {
        boolean removed = addresses.removeIf(a -> Objects.equals(a.getId(), addressId));
        if (!removed) {
            throw new IllegalParamException("address not found: " + addressId);
        }
        // 默认地址唯一性仍成立
        ensureSingleDefaultAddress();
    }

    /**
     * 将指定地址设为默认（唯一）。
     */
    public void setDefaultAddress(Long addressId) {
        UserAddress target = findAddress(addressId);
        addresses.forEach(a -> a.setDefault(false));
        target.setDefault(true);
        ensureSingleDefaultAddress();
    }

    /**
     * 获取默认地址（可能为 null）。
     */
    public UserAddress getDefaultAddress() {
        return addresses.stream().filter(UserAddress::isDefaultAddress).findFirst().orElse(null);
    }

    /**
     * 获取地址的只读快照。
     */
    public List<UserAddress> getAddressesSnapshot() {
        return Collections.unmodifiableList(addresses);
    }

    /**
     * 获取绑定的只读快照。
     */
    public List<AuthBinding> getBindingsSnapshot() {
        return Collections.unmodifiableList(bindings);
    }

    // ========== 资料行为 ==========

    /**
     * 更新资料（以值对象整体替换）。
     */
    public void updateProfile(UserProfile newProfile) {
        this.profile = Objects.requireNonNullElseGet(newProfile, UserProfile::empty);
    }

    /**
     * 修改资料中的显示名。
     */
    public void changeDisplayName(String name) {
        this.profile = this.profile.withDisplayName(name);
    }

    // ========== 内部工具 ==========

    private UserAddress findAddress(Long id) {
        return addresses.stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("address not found: " + id));
    }

    /**
     * 确保存在至少一种登录方式。
     */
    private void ensureHasAtLeastOneLoginMethod() {
        if (bindings.isEmpty()) {
            throw new IllegalParamException("user must have at least one login method");
        }
    }

    /**
     * 确保默认地址唯一（0 或 1 个）。
     */
    private void ensureSingleDefaultAddress() {
        List<UserAddress> defaults = addresses.stream()
                .filter(UserAddress::isDefaultAddress)
                .collect(Collectors.toList());
        if (defaults.size() <= 1) return;
        // 保留最早的那个默认，其他置为非默认
        boolean first = true;
        for (UserAddress a : defaults) {
            if (first) {
                first = false;
                continue;
            }
            a.setDefault(false);
        }
    }

    // ========== 便捷方法（可供仓储等调用） ==========

    /**
     * 禁止删除：使用软删除标记。
     */
    public void softDelete() {
        this.deleted = true;
        this.status = AccountStatus.DISABLED;
    }

    /**
     * 恢复软删除。
     */
    public void restore() {
        this.deleted = false;
    }
}

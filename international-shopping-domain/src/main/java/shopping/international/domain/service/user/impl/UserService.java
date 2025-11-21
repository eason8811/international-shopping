package shopping.international.domain.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.domain.adapter.port.user.IVerificationCodePort;
import shopping.international.domain.adapter.repository.user.IUserRepository;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.EmailAddress;
import shopping.international.domain.model.vo.user.Nickname;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.model.vo.user.UserProfile;
import shopping.international.domain.service.user.IUserService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * 用户账户/资料领域服务实现
 *
 * <p>职责:
 * <ul>
 *   <li>账户聚合按 ID 查询</li>
 *   <li>修改账户基本信息 (昵称, 手机号)</li>
 *   <li>邮箱变更流程 (申请验证码, 校验并生效)</li>
 *   <li>资料读取与更新 (upsert user_profile)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    /**
     * 用户聚合仓储, 用于读写 user_account / user_auth / user_profile / user_address
     */
    private final IUserRepository userRepository;
    /**
     * 邮件发送端口
     */
    private final IEmailPort emailPort;
    /**
     * 验证码存取端口
     */
    private final IVerificationCodePort verificationCodePort;
    /**
     * 邮箱验证码有效期 (默认 10 分钟)
     */
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(10);
    /**
     * 邮箱验证码长度 (默认 6 位数字)
     */
    private static final int EMAIL_CODE_LENGTH = 6;

    // ========================== 查询 ==========================

    /**
     * 按ID获取用户聚合 (含必要快照)
     *
     * @param userId 用户ID
     * @return 聚合
     */
    @Override
    public @NotNull User getById(@NotNull Long userId) {
        Optional<User> opt = userRepository.findById(userId);
        return opt.orElseThrow(() -> new IllegalParamException("用户不存在"));
    }

    // ========================== 更新账户 (昵称/手机号) ==========================

    /**
     * 修改账户基本信息 (昵称, 手机号为可选)
     *
     * @param userId   用户ID
     * @param nickname 新昵称 (可空不改)
     * @param phone    新手机号 (可空不改)
     * @return 修改后的聚合
     */
    @Override
    public @NotNull User updateAccount(@NotNull Long userId, @Nullable Nickname nickname, @Nullable PhoneNumber phone) {
        // 1) 读取当前聚合
        User user = getById(userId);

        // 2) 校验手机号唯一 (仅当传入且与当前不同)
        if (phone != null) {
            PhoneNumber oldPhone = user.getPhone();
            boolean changed = (oldPhone == null && phone.getValue() != null)
                    || (oldPhone != null && !oldPhone.getValue().equals(phone.getValue()));
            if (changed && userRepository.existsByPhoneExceptUser(userId, phone))
                throw new IllegalParamException("手机号已被使用");
            user.changePhone(phone);
        }
        if (nickname != null)
            user.changeNickname(nickname);
        // 3) Patch 更新昵称与手机号 (仅设置非空字段)
        userRepository.updateNicknameAndPhone(userId, nickname, phone);

        // 4) 回读最新聚合 (带上最新时间戳等快照)
        return getById(userId);
    }

    // ========================== 邮箱变更 (申请验证码) ==========================

    /**
     * 申请变更邮箱并发送校验码
     *
     * @param userId   用户ID
     * @param newEmail 新邮箱 (VO)
     */
    @Override
    public void requestChangeEmail(@NotNull Long userId, @NotNull EmailAddress newEmail) {
        // 1) 同邮箱检查
        User current = getById(userId);
        if (current.getEmail() != null && current.getEmail().getValue().equalsIgnoreCase(newEmail.getValue()))
            throw new IllegalParamException("新邮箱不能与当前邮箱相同");
        // 2) 唯一性 (预检, 最终仍以唯一约束兜底)
        if (userRepository.existsByEmail(newEmail))
            throw new ConflictException("邮箱已被使用");

        // 3) 若该邮箱的验证码尚在有效期内, 直接复用, 避免重复发送
        String existingCode = verificationCodePort.getEmailActivationCode(newEmail);
        if (existingCode != null && !existingCode.isBlank()) {
            log.info("邮箱 {} 在验证码 TTL 内已有待验证的激活码, 跳过重复发送", newEmail.getValue());
            return;
        }

        // 3) 生成验证码 & 保存 (Redis)
        String code = generateNumericCode(EMAIL_CODE_LENGTH);
        verificationCodePort.storeEmailActivationCode(newEmail, code, EMAIL_CODE_TTL);

        // 4) 发信 (异步端口内部处理失败会记录日志, 不影响主流程)
        emailPort.sendActivationEmail(newEmail, code);
    }

    // ========================== 邮箱变更 (校验并生效) ==========================

    /**
     * 校验验证码并生效新邮箱
     *
     * @param userId   用户ID
     * @param newEmail 新邮箱 (VO)
     * @param code     验证码
     * @return 更新后的聚合
     */
    @Override
    public @NotNull User verifyAndApplyNewEmail(@NotNull Long userId, @NotNull EmailAddress newEmail, @NotNull String code) {
        requireNonNull(code, "验证码不能为空");

        User user = getById(userId);
        if (user.getEmail() != null && user.getEmail().getValue().equalsIgnoreCase(newEmail.getValue()))
            return user; // 幂等: 邮箱更改已生效

        // 1) 原子校验并消费验证码
        boolean pass = verificationCodePort.verifyAndConsumeEmailActivationCode(newEmail, code);
        if (!pass)
            throw new IllegalParamException("验证码错误或已过期");

        // 2) 聚合内先应用 "变更邮箱" 的领域语义
        user.changeEmail(newEmail);

        // 3) 再由仓储做持久化 (仍由唯一索引兜底)
        userRepository.updateEmail(userId, newEmail);

        // 4) 回读聚合
        return getById(userId);
    }

    // ========================== 资料更新 ==========================

    /**
     * 更新资料
     *
     * @param userId     用户ID
     * @param newProfile 新资料 VO
     * @return 更新后的聚合
     */
    @Override
    public @NotNull User updateProfile(@NotNull Long userId, @NotNull UserProfile newProfile) {
        // == 聚合负责 "资料更新" 语义 (空 -> UserProfile.empty()) ==
        User user = getById(userId);
        user.updateProfile(newProfile);

        // upsert user_profile, 以聚合中的 profile 为准
        userRepository.upsertProfile(userId, user.getProfile());
        return getById(userId);
    }

    // ========================== 私有工具 ==========================

    /**
     * 生成一个指定长度的纯数字验证码
     *
     * @param len 需要生成的数字字符串的长度, 必须为正整数
     * @return 由随机数字组成的字符串, 字符串的长度等于传入的参数 <code>len</code>
     */
    private static String generateNumericCode(int len) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(random.nextInt(10));
        return sb.toString();
    }
}

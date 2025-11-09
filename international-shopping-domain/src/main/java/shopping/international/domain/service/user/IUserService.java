package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.EmailAddress;
import shopping.international.domain.model.vo.user.Nickname;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.model.vo.user.UserProfile;

/**
 * 用户账户/资料领域服务接口
 *
 * <p>职责:
 * <ul>
 *   <li>账户聚合查询</li>
 *   <li>修改账户可变字段 (昵称/手机)</li>
 *   <li>邮箱变更流程 (申请发送验证码, 校验并生效)</li>
 *   <li>资料读取与更新</li>
 * </ul>
 * </p>
 */
public interface IUserService {

    /**
     * 按ID获取用户聚合 (含必要快照)
     *
     * @param userId 用户ID
     * @return 聚合
     */
    @NotNull
    User getById(@NotNull Long userId);

    /**
     * 修改账户基本信息 (昵称, 手机号为可选)
     *
     * @param userId   用户ID
     * @param nickname 新昵称 (可空不改)
     * @param phone    新手机号 (可空不改)
     * @return 修改后的聚合
     */
    @NotNull
    User updateAccount(@NotNull Long userId, @Nullable Nickname nickname, @Nullable PhoneNumber phone);

    /**
     * 申请变更邮箱并发送校验码
     *
     * @param userId   用户ID
     * @param newEmail 新邮箱 (VO)
     */
    void requestChangeEmail(@NotNull Long userId, @NotNull EmailAddress newEmail);

    /**
     * 校验验证码并生效新邮箱
     *
     * @param userId   用户ID
     * @param newEmail 新邮箱 (VO)
     * @param code     验证码
     * @return 更新后的聚合
     */
    @NotNull
    User verifyAndApplyNewEmail(@NotNull Long userId, @NotNull EmailAddress newEmail, @NotNull String code);

    /**
     * 更新资料
     *
     * @param userId     用户ID
     * @param newProfile 新资料 VO
     * @return 更新后的聚合
     */
    @NotNull
    User updateProfile(@NotNull Long userId, @NotNull UserProfile newProfile);
}

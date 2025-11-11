package shopping.international.trigger.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.user.UpdateProfileRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.UserProfileRespond;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.UserProfile;
import shopping.international.domain.service.user.IUserService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

/**
 * 用户资料接口 {@code /users/me/profile}
 *
 * <p>职责: 
 * <ul>
 *   <li>获取当前用户资料</li>
 *   <li>更新当前用户资料</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/profile")
public class ProfileController {

    /**
     * 用户领域服务
     */
    private final IUserService userService;


    /**
     * 获取当前登录用户的资料信息
     *
     * @return 包含用户资料的 {@link ResponseEntity}, 其中 body 为 {@link Result} 对象, 内含 {@link UserProfileRespond} 实例,
     *         表示从服务器成功获取到的用户个人资料
     * @throws AccountException 如果当前没有用户登录或无法解析当前用户的ID时抛出
     */
    @GetMapping
    public ResponseEntity<Result<UserProfileRespond>> getProfile() {
        Long uid = requireCurrentUserId();
        User user = userService.getById(uid);
        return ResponseEntity.ok(Result.ok(UserProfileRespond.from(user.getProfile())));
    }


    /**
     * 更新当前登录用户的个人资料信息
     *
     * @param req 包含更新所需信息的 {@link UpdateProfileRequest} 对象, 该对象需要包含用户希望更新的所有个人资料字段
     * @return 包含更新后用户资料的 {@link ResponseEntity}, 其中 body 为 {@link Result} 对象, 内含 {@link UserProfileRespond} 实例,
     *         表示从服务器成功获取到的已更新用户个人资料, 如果更新成功, 还会附带消息 "个人资料更新成功"
     * @throws AccountException 如果当前没有用户登录或无法解析当前用户的ID时抛出
     */
    @PatchMapping
    public ResponseEntity<Result<UserProfileRespond>> updateProfile(@RequestBody UpdateProfileRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        UserProfile newProfile = req.toVO();
        User updated = userService.updateProfile(uid, newProfile);
        return ResponseEntity.ok(Result.ok(UserProfileRespond.from(updated.getProfile()), "个人资料更新成功"));
    }

    /**
     * 从安全上下文中解析当前用户ID, 解析失败则抛出未登录异常
     *
     * @return 当前登录用户ID
     * @throws AccountException 如果用户未登录或无法解析当前用户信息时抛出
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() == null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        // 典型实现: principal 可为自定义对象, 需从中提取 id, 此处兼容 Long 和 Map-like 的简单示例
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}

package shopping.international.trigger.controller.user;

import jakarta.validation.Valid;
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
import shopping.international.types.exceptions.AccountException;

/**
 * 用户资料接口（/users/me/profile）
 *
 * <p>职责：
 * <ul>
 *   <li>获取当前用户资料</li>
 *   <li>更新当前用户资料</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/profile")
public class ProfileController {

    /**
     * 用户领域服务
     */
    private final IUserService userService;

    /**
     * 获取当前用户资料
     *
     * @return 统一返回结构，包含 {@link UserProfileRespond}
     */
    @GetMapping
    public ResponseEntity<Result<UserProfileRespond>> getProfile() {
        Long uid = requireCurrentUserId();
        User user = userService.getById(uid);
        return ResponseEntity.ok(Result.ok(UserProfileRespond.from(user.getProfile())));
    }

    /**
     * 更新当前用户资料
     *
     * @param req 资料更新请求
     * @return 更新后的资料
     */
    @PatchMapping
    public ResponseEntity<Result<UserProfileRespond>> updateProfile(@RequestBody @Valid UpdateProfileRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        UserProfile newProfile = req.toVO();
        User updated = userService.updateProfile(uid, newProfile);
        return ResponseEntity.ok(Result.ok(UserProfileRespond.from(updated.getProfile()), "Profile updated"));
    }

    private Long requireCurrentUserId() {
        var ctx = SecurityContextHolder.getContext();
        Authentication a = ctx == null ? null : ctx.getAuthentication();
        if (a == null || !a.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = a.getPrincipal();
        if (principal instanceof Long l) return l;
        if (principal instanceof shopping.international.types.security.LoginPrincipal p) return p.id();
        throw new AccountException("无法解析当前用户");
    }
}

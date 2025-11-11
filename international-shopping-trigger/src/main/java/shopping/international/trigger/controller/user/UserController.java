package shopping.international.trigger.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.user.ChangeEmailRequest;
import shopping.international.api.req.user.UpdateAccountRequest;
import shopping.international.api.req.user.VerifyEmailRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.UserAccountRespond;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.Nickname;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.service.user.IUserService;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;

/**
 * 用户账户接口 (/users/me)
 *
 * <p>职责:
 * <ul>
 *   <li>获取当前用户账户信息</li>
 *   <li>修改当前用户账户信息 (昵称, 手机号等)</li>
 *   <li>申请变更邮箱, 验证新邮箱验证码</li>
 * </ul>
 * </p>
 *
 * <p>注意: Controller 不向领域层传递 Web 对象, 只组装为领域值对象或原始值</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    /**
     * 用户领域服务, 聚合账户信息的查询与修改, 邮箱变更流程
     */
    private final IUserService userService;

    /**
     * 获取当前登录用户的账户信息
     *
     * @return 统一返回结构, 包含 {@link UserAccountRespond} 对象, 代表用户账户概要信息
     */
    @GetMapping
    public ResponseEntity<Result<UserAccountRespond>> me() {
        Long uid = requireCurrentUserId();
        User user = userService.getById(uid);
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(user)));
    }

    /**
     * 更新当前用户的账户信息, 包括昵称和手机号
     *
     * @param req 请求体, 包含要更新的用户信息, 如昵称或手机号
     * @return 统一返回结构, 包含 {@link UserAccountRespond} 对象, 代表更新后的用户账户概要信息
     */
    @PatchMapping
    public ResponseEntity<Result<UserAccountRespond>> update(@RequestBody UpdateAccountRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        Nickname nickname = req.getNickname() != null ? Nickname.of(req.getNickname()) : null;
        PhoneNumber phone = req.getPhone() != null ? PhoneNumber.nullableOf(req.getPhone()) : null;

        User updated = userService.updateAccount(uid, nickname, phone);
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(updated), "个人信息更新成功"));
    }

    /**
     * 申请变更邮箱, 向用户的新邮箱发送验证码
     *
     * @param req 请求体, 包含新邮箱信息
     * @return 统一返回结构, 表示请求已被接受, 验证码已发送至新邮箱
     */
    @PostMapping("/email/change")
    public ResponseEntity<Result<Void>> changeEmail(@RequestBody ChangeEmailRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        userService.requestChangeEmail(uid, req.toEmailVO());
        return ResponseEntity.status(ApiCode.ACCEPTED.toHttpStatus())
                .body(Result.accepted("验证码邮件已发送"));
    }

    /**
     * 验证新邮箱验证码并生效
     *
     * @param req 新邮箱 + 验证码
     * @return 更新后的账户概要
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Result<UserAccountRespond>> verifyNewEmail(@RequestBody VerifyEmailRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        User updated = userService.verifyAndApplyNewEmail(uid, req.toEmailVO(), req.getCode());
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(updated), "邮箱地址已更新"));
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

package shopping.international.trigger.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.user.UpdateAccountRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.UserAccountRespond;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.vo.user.Nickname;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.service.user.IUserService;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;

/**
 * 用户账户接口（/users/me）
 *
 * <p>职责：
 * <ul>
 *   <li>获取当前用户账户信息</li>
 *   <li>修改当前用户账户信息（昵称、手机号等）</li>
 *   <li>申请变更邮箱、验证新邮箱验证码</li>
 * </ul>
 * </p>
 *
 * <p>注意：Controller 不向领域层传递 Web 对象；只组装为领域值对象或原始值</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    /**
     * 用户领域服务：聚合账户信息的查询与修改、邮箱变更流程
     */
    private final IUserService userService;

    /**
     * 获取当前用户账户信息
     *
     * @return 统一返回结构，包含 {@link UserAccountRespond}
     */
    @GetMapping
    public ResponseEntity<Result<UserAccountRespond>> me() {
        Long uid = requireCurrentUserId();
        User user = userService.getById(uid);
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(user)));
    }

    /**
     * 修改当前用户账户信息（昵称、手机号）
     *
     * <p>需要 CSRF 双提交（由安全配置保证）。本方法只做入参 validate 与 VO 组装。</p>
     *
     * @param req 修改请求体
     * @return 修改后的账户概要
     */
    @PatchMapping
    public ResponseEntity<Result<UserAccountRespond>> update(@RequestBody @Valid UpdateAccountRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        Nickname nickname = req.getNickname() != null ? Nickname.of(req.getNickname()) : null;
        PhoneNumber phone = req.getPhone() != null ? PhoneNumber.nullableOf(req.getPhone()) : null;

        User updated = userService.updateAccount(uid, nickname, phone);
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(updated), "Profile updated"));
    }

    /**
     * 申请变更邮箱：向新邮箱发送验证码
     *
     * <p>需要 CSRF 双提交（由安全配置保证）；幂等接受。</p>
     *
     * @param req 包含新邮箱
     * @return 202 Accepted
     */
    @PostMapping("/email/change")
    public ResponseEntity<Result<Void>> changeEmail(@RequestBody shopping.international.api.req.user.ChangeEmailRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        userService.requestChangeEmail(uid, req.toEmailVO());
        return ResponseEntity.status(ApiCode.ACCEPTED.toHttpStatus())
                .body(Result.accepted("Verification code sent to new email"));
    }

    /**
     * 验证新邮箱验证码并生效
     *
     * @param req 新邮箱 + 验证码
     * @return 更新后的账户概要
     */
    @PostMapping("/email/verify")
    public ResponseEntity<Result<UserAccountRespond>> verifyNewEmail(@RequestBody shopping.international.api.req.user.VerifyNewEmailRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        User updated = userService.verifyAndApplyNewEmail(uid, req.toEmailVO(), req.getCode());
        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(updated), "Email updated"));
    }

    /**
     * 从安全上下文中解析当前用户ID；解析失败则抛出未登录异常
     *
     * @return 当前登录用户ID
     */
    private Long requireCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = a.getPrincipal();
        // 典型实现：principal 可为自定义对象，需从中提取 id；此处兼容 Long 和 Map-like 的简单示例
        if (principal instanceof Long l) return l;
        if (principal instanceof shopping.international.types.security.LoginPrincipal p)
            return p.id(); // 如存在统一 Principal
        // 可扩展其他解析方式
        throw new AccountException("无法解析当前用户");
    }
}

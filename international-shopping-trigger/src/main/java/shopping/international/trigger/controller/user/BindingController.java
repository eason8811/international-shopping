package shopping.international.trigger.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.UserAuthBindingRespond;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.service.user.IBindingService;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

/**
 * 第三方/本地认证映射（/users/me/bindings）
 *
 * <p>职责：
 * <ul>
 *   <li>查看绑定列表</li>
 *   <li>绑定第三方账号</li>
 *   <li>解绑第三方账号</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/bindings")
public class BindingController {

    /**
     * 绑定领域服务
     */
    private final IBindingService bindingService;

    /**
     * 查看当前用户的认证绑定列表
     *
     * @return 绑定列表
     */
    @GetMapping
    public ResponseEntity<Result<List<UserAuthBindingRespond>>> list() {
        Long uid = requireCurrentUserId();
        List<AuthBinding> list = bindingService.listBindings(uid);
        return ResponseEntity.ok(Result.ok(list.stream().map(UserAuthBindingRespond::from).toList()));
    }

    /**
     * 绑定第三方账号（可能走 authCode 直连交换，也可能是已在上游完成的绑定意图）
     *
     * @param providerPath 路径枚举
     * @param req          可选的授权码
     * @return 新增的绑定
     */
    @PostMapping("/{provider}/link")
    public ResponseEntity<Result<UserAuthBindingRespond>> link(@PathVariable("provider") String providerPath,
                                                               @RequestBody(required = false) @Valid BindingLinkRequest req) {
        Long uid = requireCurrentUserId();
        AuthProvider provider = parseProvider(providerPath);
        String code = req == null ? null : req.getAuthCode();
        AuthBinding created = bindingService.bindByAuthCode(uid, provider, code);
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(UserAuthBindingRespond.from(created)));
    }

    /**
     * 解绑第三方账号
     *
     * @param providerPath 路径枚举
     * @return 200 OK
     */
    @DeleteMapping("/{provider}")
    public ResponseEntity<Result<Void>> unlink(@PathVariable("provider") String providerPath) {
        Long uid = requireCurrentUserId();
        AuthProvider provider = parseProvider(providerPath);
        bindingService.unbind(uid, provider);
        return ResponseEntity.ok(Result.ok("Binding removed"));
    }

    /**
     * 将 path 变量转换为领域枚举
     */
    private AuthProvider parseProvider(String raw) {
        try {
            return AuthProvider.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalParamException("不支持的 provider: " + raw);
        }
    }

    private Long requireCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = a.getPrincipal();
        if (principal instanceof Long l) return l;
        if (principal instanceof shopping.international.types.security.LoginPrincipal p) return p.id();
        throw new AccountException("无法解析当前用户");
    }
}

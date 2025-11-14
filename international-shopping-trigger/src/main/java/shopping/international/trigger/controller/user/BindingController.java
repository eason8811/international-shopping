package shopping.international.trigger.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.RedirectUrlRespond;
import shopping.international.api.resp.user.UserAuthBindingRespond;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2CallbackResult;
import shopping.international.domain.service.user.IBindingService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

import java.util.List;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 第三方账号绑定/解绑 控制器
 * <p>
 * 约定:
 * <ul>
 *     <li>
 *         发起绑定: {@code GET /user/bindings/{provider}/authorize?redirectUrl=...}<br/>
 *         返回 data.url (前端跳转至第三方授权页)
 *     </li>
 *     <li>
 *         回调完成绑定: {@code GET /user/bindings/oauth2/{provider}/callback?code=...&state=...}<br/>
 *         内部完成绑定并 302 到前端 redirectUrl
 *     </li>
 *     <li>解绑: {@code DELETE /user/bindings/{provider}}</li>
 *     <li>列表: {@code GET /user/bindings}</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/user/bindings")
public class BindingController {

    /**
     * 认证绑定服务
     */
    private final IBindingService bindingService;
    /**
     * CSRF 令牌存储器
     */
    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * 查询当前用户的第三方绑定列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<UserAuthBindingRespond>> list() {
        Long userId = requireCurrentUserId();
        List<AuthBinding> authBindingList = bindingService.listBindings(userId);
        List<UserAuthBindingRespond> userAuthBindingRespondList = authBindingList.stream()
                .map(UserAuthBindingRespond::from)
                .toList();
        return Result.ok(userAuthBindingRespondList);
    }

    /**
     * 发起 绑定意图 的第三方授权 (前端仅需拿到URL并跳转, 无需提交authCode)
     *
     * @param provider    第三方提供方
     * @param redirectUrl 绑定完成后的前端落地页
     */
    @GetMapping("/{provider}/authorize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<RedirectUrlRespond>> authorize(@PathVariable("provider") AuthProvider provider,
                                                                @RequestParam(value = "redirectUrl") String redirectUrl) {
        requireNotNull(provider, "认证提供者不能为空");
        requireNotBlank(redirectUrl, "跳转地址不能为空");
        Pattern URL_REGEX = Pattern.compile("^https?://.*$");
        require(URL_REGEX.matcher(redirectUrl).matches(), "跳转地址格式错误");

        Long userId = requireCurrentUserId();
        String url = bindingService.buildBindAuthorizationUrl(userId, provider, redirectUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Result.found(new RedirectUrlRespond(url)));
    }

    /**
     * 处理 OAuth2 回调请求, 用于完成第三方登录或绑定流程
     *
     * @param provider         第三方提供商的标识, 例如 "google", "facebook" 等
     * @param code             从第三方服务获取的授权码
     * @param state            用于防止 CSRF 攻击的状态参数
     * @param error            如果有错误发生, 该参数将包含错误代码
     * @param errorDescription 错误描述信息, 当 {@code error} 参数存在时可能附带此信息
     * @param request          HTTP 请求对象, 用于处理 CSRF 令牌
     * @param response         HTTP 响应对象, 用于更新客户端的 CSRF 令牌
     * @return 包含重定向 URL 的响应实体, 根据操作结果返回不同的 HTTP 状态码
     * <p>
     * 如果绑定成功, 将返回状态码为 {@code 302 Found} 的响应, 并提供一个重定向 URL.
     * 若绑定失败, 则返回状态码为 {@code 400 Bad Request} 的响应, 同样包含一个重定向 URL 及错误信息.
     */
    @GetMapping("/oauth2/{provider}/callback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<RedirectUrlRespond>> callback(@PathVariable("provider") AuthProvider provider,
                                                               @RequestParam(value = "code", required = false) String code,
                                                               @RequestParam(value = "state", required = false) String state,
                                                               @RequestParam(value = "error", required = false) String error,
                                                               @RequestParam(value = "error_description", required = false) String errorDescription,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        OAuth2CallbackResult result = bindingService.handleBindCallback(provider, code, state, error, errorDescription);
        if (!result.isSuccess())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.of(
                            false,
                            shopping.international.types.enums.ApiCode.BAD_REQUEST,
                            provider.name() + " 绑定错误",
                            new RedirectUrlRespond(result.getRedirectUrl()),
                            null
                    ));

        // 轮换 CSRF (与登录回调保持一致的安全姿势)
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);

        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Result.found(new RedirectUrlRespond(result.getRedirectUrl())));
    }

    /**
     * 解绑指定 provider 的第三方账号
     *
     * @param provider 认证提供者, 例如 "google", "facebook" 等
     * @return 返回一个 {@link Result} 对象, 包含解绑操作的结果信息. 如果解绑成功, 则返回状态为成功的 {@code Result} 对象, 并携带消息 "解绑成功"
     */
    @DeleteMapping("/{provider}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> unbind(@PathVariable("provider") AuthProvider provider) {
        requireNotNull(provider, "认证提供者不能为空");
        Long userId = requireCurrentUserId();
        bindingService.unbind(userId, provider);
        return Result.ok("解绑成功");
    }

    // ========================= 内部工具方法 =========================

    /**
     * 获取当前登录用户的ID 如果用户未登录或无法解析用户信息, 则抛出异常
     *
     * @return 当前登录用户的ID, 类型为 {@code Long}
     * @throws AccountException 如果用户未登录或用户信息无法被解析成Long类型
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}

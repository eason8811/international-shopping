package shopping.international.trigger.controller.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.req.customerservice.CsWsSessionCreateRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.domain.model.vo.customerservice.UserTicketWsSessionCreateCommand;
import shopping.international.domain.model.vo.customerservice.UserTicketWsSessionIssueView;
import shopping.international.domain.service.customerservice.IUserTicketService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧工单 WebSocket 会话控制器, 提供短期会话票据签发能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/ws-sessions")
public class UserTicketWsSessionController {

    /**
     * 用户侧工单领域服务
     */
    private final IUserTicketService userTicketService;

    /**
     * 用户侧创建 WebSocket 会话票据
     *
     * @param idempotencyKey 幂等键
     * @param request        会话创建请求
     * @return 会话签发数据
     */
    @PostMapping
    public ResponseEntity<Result<CsWsSessionIssueDataRespond>> createMyWsSession(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                  @RequestBody CsWsSessionCreateRequest request) {
        request.validate();
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        Long userId = requireCurrentUserId();
        UserTicketWsSessionCreateCommand command = new UserTicketWsSessionCreateCommand(
                request.getTicketNos(),
                request.getTicketIds(),
                request.getEventTypes(),
                request.getLastEventId()
        );
        UserTicketWsSessionIssueView view = userTicketService.createMyWsSession(userId, command, normalizedIdempotencyKey);
        return ResponseEntity.ok(Result.ok(UserTicketRespondAssembler.toWsSessionIssueDataRespond(view)));
    }

    /**
     * 从安全上下文读取当前用户主键
     *
     * @return 当前用户主键
     */
    private @NotNull Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId)
            return userId;
        if (principal instanceof String userId)
            return Long.parseLong(userId);
        throw new AccountException("无法解析当前用户");
    }
}


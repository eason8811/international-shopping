package shopping.international.trigger.controller.customerservice;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.customerservice.CsWsSessionCreateRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.domain.model.vo.customerservice.TicketWsSessionCreateCommand;
import shopping.international.domain.model.vo.customerservice.TicketWsSessionIssueView;
import shopping.international.domain.service.customerservice.IAdminTicketService;
import shopping.international.domain.service.customerservice.IUserTicketService;
import shopping.international.types.constant.SecurityConstants;

import static shopping.international.trigger.controller.customerservice.support.CustomerServiceControllerSupport.requireCurrentUserId;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧工单 WebSocket 会话控制器, 提供短期会话票据签发能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX)
public class TicketWsSessionController {

    /**
     * 用户侧工单领域服务
     */
    private final IUserTicketService userTicketService;
    /**
     * 管理侧工单领域服务
     */
    private final IAdminTicketService adminTicketService;

    /**
     * 用户侧创建 WebSocket 会话票据
     *
     * @param idempotencyKey 幂等键
     * @param request        会话创建请求
     * @return 会话签发数据
     */
    @PostMapping("/users/me/ws-sessions")
    public ResponseEntity<Result<CsWsSessionIssueDataRespond>> createMyWsSession(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                 @RequestBody CsWsSessionCreateRequest request) {
        PreprocessResult result = getPreprocessResult(idempotencyKey, request);
        TicketWsSessionIssueView view = userTicketService.createMyWsSession(
                result.userId(),
                result.command(),
                result.normalizedIdempotencyKey()
        );
        return ResponseEntity.ok(Result.ok(UserTicketRespondAssembler.toWsSessionIssueDataRespond(view)));
    }

    /**
     * 管理侧创建 WebSocket 会话票据
     *
     * @param idempotencyKey 幂等键
     * @param request        会话创建请求
     * @return 会话签发数据
     */
    @PostMapping("/admin/ws-sessions")
    public ResponseEntity<Result<CsWsSessionIssueDataRespond>> createWsAgentSession(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                                    @RequestBody CsWsSessionCreateRequest request) {
        PreprocessResult result = getPreprocessResult(idempotencyKey, request);
        TicketWsSessionIssueView view = adminTicketService.createWsSession(
                result.userId(),
                result.command(),
                result.normalizedIdempotencyKey()
        );
        return ResponseEntity.ok(Result.ok(AdminTicketRespondAssembler.toWsSessionIssueDataRespond(view)));
    }

    /**
     * 用于封装预处理结果的记录类, 包含标准化后的幂等键, 用户 ID 和会话创建命令
     *
     * @param normalizedIdempotencyKey 标准化后的幂等键
     * @param userId                   用户 ID
     * @param command                  会话创建命令
     */
    private record PreprocessResult(String normalizedIdempotencyKey,
                                    Long userId,
                                    TicketWsSessionCreateCommand command) {
    }

    /**
     * 预处理会话创建请求, 包括验证幂等键和请求参数, 以及获取当前用户 ID, 并返回封装后的预处理结果
     *
     * @param idempotencyKey 请求的幂等键, 用于确保操作的唯一性
     * @param request        创建 WebSocket 会话的请求对象
     * @return 返回一个 {@link PreprocessResult} 对象, 包含标准化后的幂等键, 用户 ID 和会话创建命令
     */
    private @NotNull PreprocessResult getPreprocessResult(String idempotencyKey, CsWsSessionCreateRequest request) {
        request.validate();
        String normalizedIdempotencyKey = normalizeNotNullField(
                idempotencyKey,
                "Idempotency-Key 不能为空",
                value -> value.length() <= 64,
                "Idempotency-Key 长度不能超过 64 个字符"
        );

        Long userId = requireCurrentUserId();
        TicketWsSessionCreateCommand command = new TicketWsSessionCreateCommand(
                request.getTicketNos(),
                request.getTicketIds(),
                request.getEventTypes(),
                request.getLastEventId()
        );
        return new PreprocessResult(normalizedIdempotencyKey, userId, command);
    }

}

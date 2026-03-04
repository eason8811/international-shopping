package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.types.exceptions.ConflictException;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单写操作幂等模板基类, 通过模板方法统一 register, execute, markSucceeded 流程
 */
public abstract class AbstractTicketIdempotentActionService {

    /**
     * 幂等占位状态 TTL
     */
    protected static final Duration IDEMPOTENCY_PENDING_TTL = Duration.ofMinutes(5);
    /**
     * 幂等成功状态 TTL
     */
    protected static final Duration IDEMPOTENCY_SUCCESS_TTL = Duration.ofHours(24);

    /**
     * 工单幂等端口
     */
    private final ITicketIdempotencyPort ticketIdempotencyPort;

    /**
     * 构造幂等模板基类
     *
     * @param ticketIdempotencyPort 工单幂等端口
     */
    protected AbstractTicketIdempotentActionService(@NotNull ITicketIdempotencyPort ticketIdempotencyPort) {
        this.ticketIdempotencyPort = ticketIdempotencyPort;
    }

    /**
     * 提供幂等端口访问入口, 供子类复用 create, close 等专用幂等方法
     *
     * @return 工单幂等端口
     */
    protected final @NotNull ITicketIdempotencyPort idempotencyPort() {
        return ticketIdempotencyPort;
    }

    /**
     * 使用模板方法执行通用写操作幂等流程
     *
     * @param scene                 幂等场景
     * @param actorUserId           操作者用户 ID
     * @param resource              资源标识
     * @param idempotencyKey        幂等键
     * @param inProgressMessage     请求处理中提示
     * @param succeededResultLoader 幂等成功命中时的结果加载器, 入参为 resultRef
     * @param actionExecutor        实际业务执行器
     * @param resultRefExtractor    业务执行成功后的结果引用提取器
     * @param <R>                   返回类型
     * @return 业务执行结果
     */
    protected final <R> @NotNull R executeActionWithIdempotency(@NotNull String scene,
                                                                 @NotNull Long actorUserId,
                                                                 @NotNull String resource,
                                                                 @NotNull String idempotencyKey,
                                                                 @NotNull String inProgressMessage,
                                                                 @NotNull Function<@Nullable String, R> succeededResultLoader,
                                                                 @NotNull Supplier<R> actionExecutor,
                                                                 @NotNull Function<R, String> resultRefExtractor) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                scene,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return succeededResultLoader.apply(tokenStatus.ticketNo());
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException(inProgressMessage);

        R result = actionExecutor.get();
        String resultRef = resultRefExtractor.apply(result);
        requireNotBlank(resultRef, "resultRef 不能为空");
        ticketIdempotencyPort.markActionSucceeded(
                scene,
                actorUserId,
                resource,
                idempotencyKey,
                resultRef,
                IDEMPOTENCY_SUCCESS_TTL
        );
        return result;
    }

    /**
     * 标记通用写操作幂等成功
     *
     * @param scene          幂等场景
     * @param actorUserId    操作者用户 ID
     * @param resource       资源标识
     * @param idempotencyKey 幂等键
     * @param resultRef      成功结果引用
     */
    protected final void markActionSucceeded(@NotNull String scene,
                                             @NotNull Long actorUserId,
                                             @NotNull String resource,
                                             @NotNull String idempotencyKey,
                                             @NotNull String resultRef) {
        ticketIdempotencyPort.markActionSucceeded(
                scene,
                actorUserId,
                resource,
                idempotencyKey,
                resultRef,
                IDEMPOTENCY_SUCCESS_TTL
        );
    }
}

package shopping.international.domain.adapter.port.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * 工单写操作幂等端口接口
 */
public interface ITicketIdempotencyPort {

    /**
     * 幂等令牌状态值对象
     *
     * @param status    当前状态
     * @param ticketNo  已绑定工单编号
     */
    record TokenStatus(@NotNull Status status,
                       @Nullable String ticketNo) {

        /**
         * 幂等状态枚举
         */
        public enum Status {
            /**
             * 当前请求首次获得执行权
             */
            NEW,
            /**
             * 同键请求正在执行中
             */
            IN_PROGRESS,
            /**
             * 同键请求已执行成功
             */
            SUCCEEDED
        }

        /**
         * 判断当前状态是否为成功态
         *
         * @return 成功态返回 true
         */
        public boolean isSucceeded() {
            return status == Status.SUCCEEDED;
        }
    }

    /**
     * 注册或查询创建工单幂等令牌
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @NotNull
    TokenStatus registerCreateOrGet(@NotNull Long userId,
                                    @NotNull String idempotencyKey,
                                    @NotNull Duration ttl);

    /**
     * 标记创建工单幂等令牌成功
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @param ticketNo        工单编号
     * @param ttl             成功状态 TTL
     */
    void markCreateSucceeded(@NotNull Long userId,
                             @NotNull String idempotencyKey,
                             @NotNull String ticketNo,
                             @NotNull Duration ttl);

    /**
     * 注册或查询关闭工单幂等令牌
     *
     * @param userId          用户 ID
     * @param ticketNo        工单编号
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @NotNull
    TokenStatus registerCloseOrGet(@NotNull Long userId,
                                   @NotNull String ticketNo,
                                   @NotNull String idempotencyKey,
                                   @NotNull Duration ttl);

    /**
     * 标记关闭工单幂等令牌成功
     *
     * @param userId          用户 ID
     * @param ticketNo        工单编号
     * @param idempotencyKey  幂等键
     * @param ttl             成功状态 TTL
     */
    void markCloseSucceeded(@NotNull Long userId,
                            @NotNull String ticketNo,
                            @NotNull String idempotencyKey,
                            @NotNull Duration ttl);

    /**
     * 注册或查询通用写操作幂等令牌
     *
     * @param scene           场景标识
     * @param userId          用户 ID
     * @param resource        资源标识
     * @param idempotencyKey  幂等键
     * @param ttl             占位状态 TTL
     * @return 幂等状态
     */
    @NotNull
    TokenStatus registerActionOrGet(@NotNull String scene,
                                    @NotNull Long userId,
                                    @NotNull String resource,
                                    @NotNull String idempotencyKey,
                                    @NotNull Duration ttl);

    /**
     * 标记通用写操作幂等令牌成功
     *
     * @param scene           场景标识
     * @param userId          用户 ID
     * @param resource        资源标识
     * @param idempotencyKey  幂等键
     * @param resultRef       成功结果引用
     * @param ttl             成功状态 TTL
     */
    void markActionSucceeded(@NotNull String scene,
                             @NotNull Long userId,
                             @NotNull String resource,
                             @NotNull String idempotencyKey,
                             @NotNull String resultRef,
                             @NotNull Duration ttl);
}

package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * 用户收货地址创建幂等性端口
 *
 * <p>职责:</p>
 * <ul>
 *     <li>针对 {@code (userId, Idempotency-Key)} 管理地址创建请求的幂等状态</li>
 *     <li>避免同一幂等键在短时间窗口内重复插入多条地址记录</li>
 *     <li>在重复请求中返回已创建的地址 ID, 供领域服务回读地址</li>
 * </ul>
 */
public interface IAddressIdempotencyPort {

    /**
     * 幂等 Token 状态值对象, 表示当前幂等键在 Redis 中的状态
     *
     * @param status    当前幂等状态
     * @param addressId 若状态为 {@link Status#SUCCEEDED SUCCEEDED}, 则为已绑定的地址 ID, 否则为 {@code null}
     */
    record TokenStatus(Status status, Long addressId) {

        /**
         * 幂等状态枚举
         */
        public enum Status {
            /**
             * 该幂等键尚未被使用, 当前调用获得了"创建权", 调用方可以继续执行创建逻辑
             */
            NEW,
            /**
             * 该幂等键已经被其他请求抢占且尚未完成创建流程, 通常表示"请求正在处理"
             */
            IN_PROGRESS,
            /**
             * 该幂等键已经成功绑定了一个地址 ID, 调用方应直接返回该地址的结果
             */
            SUCCEEDED
        }

        /**
         * 判断当前状态是否为 {@link Status#NEW NEW}
         *
         * @return 若为 NEW 返回 {@code true}, 否则返回 {@code false}
         */
        public boolean isNew() {
            return status == Status.NEW;
        }

        /**
         * 判断当前状态是否为 {@link Status#SUCCEEDED SUCCEEDED}
         *
         * @return 若为 SUCCEEDED 返回 {@code true}, 否则返回 {@code false}
         */
        public boolean isSucceeded() {
            return status == Status.SUCCEEDED;
        }
    }

    /**
     * 尝试为给定用户和幂等键注册一个"地址创建幂等 Token", 或返回已有结果
     *
     * <p>语义说明:</p>
     * <ul>
     *     <li>若 Redis 中不存在该 key, 则以"PENDING"占位写入并返回 {@link TokenStatus.Status#NEW NEW}</li>
     *     <li>若已存在且值为 {@code PENDING}, 则返回 {@link TokenStatus.Status#IN_PROGRESS IN_PROGRESS}</li>
     *     <li>若已存在且值为 {@code OK:{addressId}}, 则返回 {@link TokenStatus.Status#SUCCEEDED SUCCEEDED} 并携带 addressId</li>
     * </ul>
     *
     * @param userId         用户 ID, 用于与幂等键一起构成 Redis key 命名空间
     * @param idempotencyKey 幂等键, 通常来自 HTTP 头 {@code Idempotency-Key}
     * @param ttl            PENDING 状态在 Redis 中的存活时间, 到期后允许新的创建请求重新获得创建权
     * @return 当前幂等键对应的状态
     */
    @NotNull
    TokenStatus registerOrGet(@NotNull Long userId, @NotNull String idempotencyKey, @NotNull Duration ttl);

    /**
     * 在地址创建成功后, 将指定地址 ID 与幂等键进行绑定
     *
     * <p>该操作通常在数据库事务成功提交后调用, 用于将占位值 {@code PENDING} 覆盖为 {@code OK:{addressId}},
     * 以便后续重复请求能够直接返回已创建的地址结果</p>
     *
     * @param userId         用户 ID
     * @param idempotencyKey 幂等键, 必须与 {@link #registerOrGet(Long, String, Duration)} 中使用的值保持一致
     * @param addressId      新创建的地址主键 ID
     * @param ttl            成功态在 Redis 中的存活时间, 通常可以比 PENDING 态更长
     */
    void markSucceeded(@NotNull Long userId, @NotNull String idempotencyKey, @NotNull Long addressId, @NotNull Duration ttl);
}

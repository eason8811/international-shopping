package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * 订单超时取消消息载荷
 *
 * @param orderNo   订单号
 * @param userId    用户 ID
 * @param createdAt 创建时间
 */
public record OrderTimeoutMessage(@NotNull String orderNo,
                                  @NotNull Long userId,
                                  @NotNull LocalDateTime createdAt) {
}

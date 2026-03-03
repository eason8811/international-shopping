package shopping.international.domain.model.vo.customerservice;

import java.time.LocalDateTime;

/**
 * 用户侧 WebSocket 会话签发结果视图值对象
 *
 * @param wsToken                  WebSocket 短期访问令牌
 * @param wsUrl                    WebSocket 连接地址
 * @param issuedAt                 签发时间
 * @param expiresAt                过期时间
 * @param heartbeatIntervalSeconds 心跳间隔秒数
 * @param resumeTtlSeconds         续传窗口秒数
 */
public record TicketWsSessionIssueView(String wsToken,
                                       String wsUrl,
                                       LocalDateTime issuedAt,
                                       LocalDateTime expiresAt,
                                       Integer heartbeatIntervalSeconds,
                                       Integer resumeTtlSeconds) {
}

package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.user.OAuth2EphemeralState;

import java.time.Duration;
import java.util.Optional;

/**
 * OAuth2 授权一次性状态存取端口
 * <p>职责: 存储并一次性弹出 state 对应的 {@code nonce, code_verifier, redirect, provider}</p>
 * <p>一般由 Redis 等 KV 存储实现，并设置合理 TTL</p>
 */
public interface IOAuth2StatePort {

    /**
     * 保存一次性授权上下文 (state → nonce/code_verifier/redirect/provider)
     *
     * @param state  上下文
     * @param ttl    过期时间
     */
    void storeEphemeral(@NotNull OAuth2EphemeralState state, @NotNull Duration ttl);

    /**
     * 根据 state 一次性弹出上下文 (读取即删除)
     *
     * @param state  state 字符串
     * @return 上下文, 不存在或过期返回空
     */
    @NotNull Optional<OAuth2EphemeralState> popByState(@NotNull String state);
}

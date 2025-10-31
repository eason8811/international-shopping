package shopping.international.domain.model.vo.user;

import shopping.international.domain.model.enums.user.AuthProvider;

/**
 * OAuth2 授权一次性上下文 (与 state 绑定)
 *
 * @param provider     提供方
 * @param state        一次性 state
 * @param nonce        一次性 nonce (OIDC 防重放)
 * @param codeVerifier PKCE 明文 (回调置换 token 用)
 * @param redirect     登录完成后的前端落地页 (不透传第三方)
 */
public record OAuth2EphemeralState(
        AuthProvider provider,
        String state,
        String nonce,
        String codeVerifier,
        String redirect
) {
}

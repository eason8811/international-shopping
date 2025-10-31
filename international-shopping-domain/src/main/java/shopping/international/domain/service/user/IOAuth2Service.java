package shopping.international.domain.service.user;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2CallbackResult;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * OAuth2/OIDC 第三方登录的领域服务接口
 *
 * <p>职责边界: </p>
 * <ul>
 *   <li>生成第三方授权页 URL: 创建并持久化一次性 {@code state}/{@code nonce}/{@code code_verifier}</li>
 *   <li>处理第三方回调: 校验 {@code state}/{@code nonce}, 用 {@code code} 换取令牌, 校验 {@code id_token}, 
 *       拉取 {@code userinfo}, 完成 {@code 存在则登录, 不存在则注册/绑定}, 并签发本地会话令牌</li>
 * </ul>
 *
 * <p>重要约束: </p>
 * <ul>
 *   <li>不依赖任何 Servlet/Web 类型, 控制器负责重定向与下发 Cookie</li>
 *   <li>实现可依赖 OAuth2 客户端, JWK 验签, 仓储等基础设施, 但不向上暴露</li>
 * </ul>
 */
public interface IOAuth2Service {

    /**
     * 构造第三方授权地址, 并以一次性 {@code state} 关联缓存 {@code nonce}/{@code code_verifier}/{@code redirect} 等上下文
     *
     * @param provider 第三方提供方 (如 GOOGLE/APPLE/...)
     * @param redirect 登录完成后的站内跳转地址 (可空)
     * @return 第三方授权页的完整 URL (包含 {@code client_id}, {@code redirect_uri}, {@code scope}, {@code state}, {@code nonce}, {@code code_challenge} 等)
     * @throws IllegalParamException 当提供方未配置或内部错误时抛出
     */
    String buildAuthorizationUrl(AuthProvider provider, @Nullable String redirect);

    /**
     * 处理第三方回调 (与 OAuth2/OIDC 授权码 + PKCE 时序一致)
     *
     * <p><b>标准流程: </b></p>
     * <ol>
     *   <li><b>错误短路: </b>若 {@code error} 非空, 则仍需校验 {@code state} (一次性, 与发起时一致), 
     *       成立则清理缓存并返回失败结果 (用于前端展示), 不一致则拒绝 (防重放/伪造)</li>
     *
     *   <li><b>校验 state: </b>必须与发起授权时生成的一次性 {@code state} 完全一致且未被使用, 据此
     *       <b>弹出 (并删除)</b>当时缓存的上下文: {@code nonce}, {@code code_verifier}, {@code redirect}</li>
     *
     *   <li><b>置换令牌: </b>向提供方的 token 端点发起 <code>application/x-www-form-urlencoded</code> 请求, 
     *       携带参数: 
     *       <ul>
     *         <li>{@code grant_type=authorization_code}</li>
     *         <li>{@code code}: 回调中的授权码</li>
     *         <li>{@code redirect_uri}: 必须与授权请求阶段使用的完全一致</li>
     *         <li>{@code client_id}: 客户端标识</li>
     *         <li>{@code client_secret}: <i>机密客户端</i>必填, 公共客户端通常不需要</li>
     *         <li>{@code code_verifier}: 与授权阶段配对的 PKCE 明文字段</li>
     *       </ul>
     *       期望返回 {@code access_token}, {@code expires_in}, {@code id_token}[OIDC], {@code refresh_token}[可选] 等</li>
     *
     *   <li><b>校验 ID Token (OIDC): </b>若返回了 {@code id_token}, 需使用提供方 JWK 验签, 并严格校验声明: 
     *       <ul>
     *         <li>{@code iss}: 在允许发行者列表内</li>
     *         <li>{@code aud}: 包含本服务的 {@code client_id}</li>
     *         <li>{@code exp}/{@code iat}/{@code nbf}: 在允许时钟偏移内有效</li>
     *         <li>{@code nonce}: 必须与 <b>缓存中取回的 nonce 完全一致</b> (防止重放)</li>
     *       </ul>
     *       任一校验失败 → 失败结果并清理一次性状态</li>
     *
     *   <li><b>获取用户信息 (可选): </b>若需要更详细资料且 scope 覆盖 (如 {@code openid email profile}), 
     *       使用 {@code access_token} 调用 <code>/userinfo</code>, 并要求 {@code sub} 与 {@code id_token.sub} 一致</li>
     *
     *   <li><b>本地账户处理: </b>依据 ({@code iss}, {@code sub})定位或创建本地用户及其绑定关系 (不存在则注册并绑定)</li>
     *
     *   <li><b>签发本地会话: </b>为该用户签发本地 {@code access_token} (以及必要时的 {@code refresh_token}), 
     *       并返回 {@code redirectUrl} (优先使用缓存中的 {@code redirect}, 否则给出默认落地页)</li>
     * </ol>
     *
     * <p><b>安全要点: </b></p>
     * <ul>
     *   <li>{@code state}, {@code nonce}, {@code code_verifier} 必须与授权阶段一并生成并与会话/缓存关联, 回调时<b>一次性弹出并删除</b></li>
     *   <li>{@code redirect_uri} 在 token 置换阶段必须与授权阶段完全一致, 否则会被判定为 {@code invalid_grant}</li>
     *   <li>若提供方非 OIDC (不返回 {@code id_token}), 则跳过 nonce 比对, 仅依赖 {@code state}+token 机密性与 (可选) {@code userinfo}</li>
     * </ul>
     *
     * @param provider         第三方提供方 (如 GOOGLE/APPLE/...)
     * @param code             授权码 (成功场景)
     * @param state            一次性状态码 (由授权阶段生成)
     * @param error            失败场景错误码 (用户取消等, 可为空)
     * @param errorDescription 失败场景错误描述 (可为空)
     * @return 处理结果 (是否成功, 本地 access/refresh 令牌字符串, 跳转地址)
     * @throws IllegalParamException 当 state 非法/失配, token 置换失败, id_token 验签失败等场景抛出
     */
    OAuth2CallbackResult handleCallback(AuthProvider provider, @Nullable String code, @Nullable String state,
                                        @Nullable String error, @Nullable String errorDescription);
}

package shopping.international.app.security.service;

import org.springframework.security.core.Authentication;

/**
 * {@link IJwtTokenService} 接口定义了与 JWT 相关的操作方法, 主要用于解析和验证 JWT 字符串, 并根据其内容生成相应的认证信息
 */
public interface IJwtTokenService {
    /**
     * 解析并验证传入的 JWT 字符串, 并基于解析结果构建一个 {@link Authentication} 对象
     *
     * @param rawJwt 待解析和验证的原始 JWT 字符串
     * @return 返回一个 Authentication 对象, 包含了从 JWT 中解析出的身份信息 如果解析或验证失败, 则返回 null 或抛出异常(根据具体实现)
     */
    Authentication parseAndAuthenticate(String rawJwt);
}

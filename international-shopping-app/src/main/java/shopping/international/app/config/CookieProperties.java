package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cookie CSRF 仓储相关配置属性
 */
@Data
@ConfigurationProperties(prefix = "security.cookie")
public class CookieProperties {
    /**
     * 标识 Cookie 是否只能通过 HTTPS 协议发送, 以增强安全性
     * <p>当设置为 {@code true} 时, 浏览器将仅在安全连接 (HTTPS) 下发送该 Cookie
     * 这有助于防止中间人攻击, 保护敏感信息不被截取</p>
     */
    private Boolean secure;
    /**
     * 定义 Cookie 的 SameSite 属性, 用于控制 Cookie 在跨站请求中的发送行为
     * <p>该属性可设置为 {@code Strict}, {@code Lax} 或 {@code None}, 以确定在何种情况下浏览器会随请求附带该 Cookie:
     * <ul>
     *   <li>{@code Strict}: 只有来自同一站点的请求才会携带该 Cookie, 提供了最高级别的保护</li>
     *   <li>{@code Lax}: 对于跨站子请求 (如图片加载或脚本引用) 不携带 Cookie, 但允许用户从外部链接访问网站时携带 Cookie</li>
     *   <li>{@code None}: Cookie 将随所有类型请求一同发送, 包括跨域请求, 需要与 Secure 属性一起使用来增强安全性</li>
     * </ul>
     * </p>
     * <p>默认值为 {@code Lax}</p>
     */
    private String sameSite;
    /**
     * 定义 Cookie 的路径, 用于指定浏览器在哪些路径下发送该 Cookie
     */
    private String path;
    /**
     * 标识 Cookie 是否只能通过 HTTP 访问, 为 {@code true} 时不能通过 JavaScript 的 <code>Document.cookie</code> API 获取
     */
    private Boolean httpOnly;
}

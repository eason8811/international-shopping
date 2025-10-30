package shopping.international.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CSRF 令牌响应 (用于 {@code /auth/csrf})
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsrfTokenRespond {
    /**
     * 当前会话绑定的 CSRF 令牌
     */
    private String csrfToken;
}

package shopping.international.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重定向地址响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedirectUrlRespond {
    /**
     * 需要前端跳转的 URL
     */
    private String url;
}

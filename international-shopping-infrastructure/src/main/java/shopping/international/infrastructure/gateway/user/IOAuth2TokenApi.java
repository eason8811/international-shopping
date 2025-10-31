package shopping.international.infrastructure.gateway.user;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * OAuth2 Token 端点网关接口
 *
 * <p>使用 {@code @Url} 传入完整 Token Endpoint, {@code @FormUrlEncoded} 提交 {@code x-www-form-urlencoded}</p>
 */
public interface IOAuth2TokenApi {

    /**
     * 置换授权码为 Token
     *
     * @param url    完整 token endpoint (例如 <a href="https://oauth2.googleapis.com/token">https://oauth2.googleapis.com/token</a>)
     * @param fields 表单字段 (grant_type, code, redirect_uri, client_id, client_secret, code_verifier)
     * @return 返回一个 Call 对象, 该对象可以用来异步执行网络请求并获取响应体 ResponseBody, 响应体中包含服务器返回的令牌信息
     */
    @FormUrlEncoded
    @POST
    Call<ResponseBody> exchangeCode(@Url String url, @FieldMap Map<String, String> fields);
}

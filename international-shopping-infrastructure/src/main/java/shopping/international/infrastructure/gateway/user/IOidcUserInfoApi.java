package shopping.international.infrastructure.gateway.user;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Url;

/**
 * OIDC UserInfo 端点网关接口
 */
public interface IOidcUserInfoApi {

    /**
     * 获取用户信息
     *
     * @param url            完整的 userinfo endpoint 地址 (例如 <a href="https://openidconnect.googleapis.com/v1/userinfo">https://openidconnect.googleapis.com/v1/userinfo</a>)
     * @param authorization  授权头, 形如 {@code "Bearer {access_token}"}, 用于提供访问令牌
     * @return 返回一个 Call 对象, 该对象可以用来异步执行网络请求并获取响应体 ResponseBody
     */
    @GET
    Call<ResponseBody> userInfo(@Url String url, @Header("Authorization") String authorization);
}

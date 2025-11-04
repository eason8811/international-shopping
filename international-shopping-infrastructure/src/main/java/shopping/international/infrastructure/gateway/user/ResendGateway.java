package shopping.international.infrastructure.gateway.user;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;
import shopping.international.infrastructure.gateway.user.dto.ResendSendRequest;
import shopping.international.infrastructure.gateway.user.dto.ResendSendResponse;

/**
 * Resend 服务的邮件发送网关接口
 */
public interface ResendGateway {

    /**
     * 发送邮件至 Resend 服务
     *
     * <p>通过此方法, 可以将构建好的 {@link ResendSendRequest} 请求体发送到指定的 URL,
     * 并使用 HTTP Authorization 头进行认证, 该方法返回一个 {@code Call<ResendSendResponse>} 对象,
     * 该对象可以用来异步执行网络请求并获取响应体, 响应体中包含了发送结果信息, 如状态, 消息ID等</p>
     *
     * @param url 目标 URL, 应指向 Resend 的相应端点
     * @param authorization HTTP Authorization 头, 用于提供访问令牌, 形如 {@code "Bearer {access_token}"}
     * @param body 请求体, 包含了发送邮件所需的信息, 如发件人, 收件人, 主题, HTML 内容及纯文本内容
     * @return 返回一个 {@code Call<ResendSendResponse>} 对象, 允许调用者异步地发起网络请求并处理响应
     */
    @POST
    Call<ResendSendResponse> send(@Url String url, @Header("Authorization") String authorization, 
                                  @Body ResendSendRequest body);
}

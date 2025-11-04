package shopping.international.infrastructure.gateway.user;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendRequest;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendResponse;

/**
 * Mailjet 发送邮件的 Retrofit 网关
 *
 * <p>Base URL 形如 {@code https://api.mailjet.com/v3.1/},
 * 调用路径为 {@code POST send}</p>
 */
public interface MailjetGateway {
    /**
     * 发送邮件至 Mailjet 服务
     *
     * @param baseUrl Base URL, 形如 {@code https://api.mailjet.com/v3.1/}
     * @param authorization HTTP Authorization 头, 使用 HTTP Basic 认证格式: {@code Basic base64(apiKey:apiSecret)}
     * @param request 请求体, 应遵循 Mailjet v3.1 {@code /send} 接口定义的 Messages 结构
     * @return {@code Call<MailjetSendResponse>} 返回一个 Call 对象, 该对象可以用来异步执行网络请求并获取响应体, 响应体中包含了发送结果信息, 如状态, 消息ID等
     */
    @POST("/send")
    Call<MailjetSendResponse> send(@Url String baseUrl,  @Header("Authorization") String authorization,
                                   @Body MailjetSendRequest request);
}

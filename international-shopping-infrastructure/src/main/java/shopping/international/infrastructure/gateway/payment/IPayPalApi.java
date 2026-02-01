package shopping.international.infrastructure.gateway.payment;

import retrofit2.Call;
import retrofit2.http.*;
import shopping.international.infrastructure.gateway.payment.dto.*;

import java.util.Map;

/**
 * PayPal 网关 API (Retrofit 接口)
 *
 * <ul>
 *     <li>使用 {@code @Url} 传入完整地址，避免在配置中固化 baseUrl</li>
 *     <li>Authorization 使用 Bearer Token (由 {@code /v1/oauth2/token} 获取)</li>
 * </ul>
 */
public interface IPayPalApi {

    /**
     * 获取 OAuth2 Access Token (client_credentials)
     */
    @FormUrlEncoded
    @POST
    Call<PayPalAccessTokenRespond> token(@Url String url,
                                         @Header("Authorization") String authorization,
                                         @Field("grant_type") String grantType);

    /**
     * 创建 PayPal Order
     */
    @POST
    Call<PayPalCreateOrderRespond> createOrder(@Url String url,
                                               @Header("Authorization") String authorization,
                                               @Header("PayPal-Request-Id") String requestId,
                                               @Header("Prefer") String prefer,
                                               @Body PayPalCreateOrderRequest body);

    /**
     * 查询 PayPal Order
     */
    @GET
    Call<PayPalGetOrderRespond> getOrder(@Url String url,
                                         @Header("Authorization") String authorization);

    /**
     * Capture PayPal Order
     */
    @POST
    Call<PayPalCaptureOrderRespond> captureOrder(@Url String url,
                                                 @Header("Authorization") String authorization,
                                                 @Header("PayPal-Request-Id") String requestId,
                                                 @Header("Prefer") String prefer,
                                                 @Body Map<String, Object> body);

    /**
     * Refund PayPal Capture
     */
    @POST
    Call<PayPalRefundCaptureRespond> refundCapture(@Url String url,
                                                   @Header("Authorization") String authorization,
                                                   @Header("PayPal-Request-Id") String requestId,
                                                   @Header("Prefer") String prefer,
                                                   @Body PayPalRefundCaptureRequest body);

    /**
     * 查询 PayPal Refund
     */
    @GET
    Call<PayPalGetRefundRespond> getRefund(@Url String url,
                                           @Header("Authorization") String authorization);

    /**
     * 验证 Webhook 签名
     */
    @POST
    Call<PayPalVerifyWebhookSignatureRespond> verifyWebhookSignature(@Url String url,
                                                                     @Header("Authorization") String authorization,
                                                                     @Body PayPalVerifyWebhookSignatureRequest body);
}

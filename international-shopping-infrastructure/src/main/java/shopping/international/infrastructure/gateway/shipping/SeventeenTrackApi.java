package shopping.international.infrastructure.gateway.shipping;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;
import shopping.international.infrastructure.gateway.shipping.dto.SeventeenTrackRegisterTrackRequest;
import shopping.international.infrastructure.gateway.shipping.dto.SeventeenTrackRegisterTrackRespond;

import java.util.List;

/**
 * 17Track HTTP API 声明
 */
public interface SeventeenTrackApi {

    /**
     * 注册运单
     *
     * @param url 完整 URL
     * @param token 17Track token, 对应 header 17token
     * @param request 请求体
     * @return 调用句柄
     */
    @POST
    Call<SeventeenTrackRegisterTrackRespond> registerTrack(@Url String url,
                                                           @Header("17token") String token,
                                                           @Body List<SeventeenTrackRegisterTrackRequest> request);
}

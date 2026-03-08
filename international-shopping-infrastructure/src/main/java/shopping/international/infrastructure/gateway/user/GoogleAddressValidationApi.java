package shopping.international.infrastructure.gateway.user;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * Google Address Validation Retrofit API
 */
public interface GoogleAddressValidationApi {

    /**
     * 校验地址
     *
     * @param url 完整请求 URL
     * @param apiKey Google API Key
     * @param body 请求体
     * @return 原始响应体
     */
    @POST
    @Headers("Content-Type: application/json")
    Call<ResponseBody> validateAddress(@Url String url, @Query("key") String apiKey, @Body Map<String, Object> body);
}

package shopping.international.infrastructure.gateway.common;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;
import shopping.international.infrastructure.gateway.common.dto.OpenErLatestRespond;

/**
 * 汇率数据源网关 (第三方 REST API)
 *
 * <p>使用 {@code @Url} 传入完整 URL, 复用全局 Retrofit 客户端。</p>
 */
public interface IExchangeRateApi {

    /**
     * 获取 latest 汇率
     *
     * @param url 完整 URL (例如 <a href="https://open.er-api.com/v6/latest/USD">https://open.er-api.com/v6/latest/USD</a>)
     */
    @GET
    Call<OpenErLatestRespond> latest(@Url String url);
}


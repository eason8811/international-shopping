package shopping.international.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import shopping.international.infrastructure.gateway.common.IExchangeRateApi;
import shopping.international.infrastructure.gateway.payment.IPayPalApi;
import shopping.international.infrastructure.gateway.shipping.SeventeenTrackApi;
import shopping.international.infrastructure.gateway.user.IOAuth2TokenApi;
import shopping.international.infrastructure.gateway.user.IOidcUserInfoApi;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * Retrofit2/OkHttp 全局配置
 *
 * <p>要点: </p>
 * <ul>
 *   <li>HTTPS: 使用系统默认信任的证书链 (Google 等均要求 HTTPS) </li>
 *   <li>Jackson: 启用蛇形命名支持第三方 JSON (如 {@code access_token}) </li>
 *   <li>超时与连接池: 适配第三方 OAuth2/OIDC 端点</li>
 * </ul>
 */
@Configuration
public class RetrofitConfig {

    /**
     * 配置并返回一个适用于 OAuth 相关操作的 <code>ObjectMapper</code> 实例
     *
     * @return 返回一个经过特别配置的 <code>ObjectMapper</code> 实例, 用于处理 OAuth 操作中的 JSON 数据
     */
    @Bean
    public ObjectMapper oauthObjectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)    // 字段命名, access_token -> accessToken
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)       // 属性名大小写不敏感
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)             // 允许未加引号的字段名
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)     // 反序列化忽略未知字段
                .addModule(new JavaTimeModule())                                // 注册 JavaTime
                .build();

        // 非空序列化
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 配置并返回一个适用于 OAuth 相关操作的 <code>OkHttpClient</code> 实例
     * <p>此方法创建了一个 <code>OkHttpClient</code>, 该客户端配置了 SSL 上下文, 日志拦截器, 超时设置等, 以适应 OAuth 请求的需求</p>
     * <p>采用系统默认的信任管理器与 TLS 配置, 生产环境应避免 <b>信任所有证书</b> </p>
     *
     * @return 返回一个经过特别配置的 <code>OkHttpClient</code> 实例, 用于处理 OAuth 操作中的 HTTP 请求
     */
    @Bean
    public OkHttpClient oauthOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        SSLContext sslContext = systemDefaultSSLContext();

        return new OkHttpClient.Builder()
                .sslSocketFactory(
                        sslContext.getSocketFactory(),
                        SslUtil.systemDefaultTrustManager()
                )
                .callTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(8, 5, TimeUnit.MINUTES))
                .addInterceptor(logging)
                .build();
    }

    /**
     * 初始化并返回一个基于系统默认设置的 <code>SSLContext</code> 实例
     * <p>该方法尝试使用系统默认的信任管理器来初始化 <code>SSLContext</code>, 以确保在进行 HTTPS 通信时, 客户端能够验证服务器的身份, 此 SSL 上下文适用于需要安全连接的应用场景, 如 OAuth 认证过程</p>
     *
     * @return 返回一个已初始化的 <code>SSLContext</code> 对象, 配置了系统默认的信任管理器与 TLS 协议
     * @throws IllegalStateException 如果由于任何原因无法初始化 SSL 上下文, 则抛出此异常
     */
    private static SSLContext systemDefaultSSLContext() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null); // 使用系统 CA
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("初始化系统默认 SSLContext 失败", e);
        }
    }

    /**
     * SSL 工具: 暴露系统默认 TrustManager
     */
    static class SslUtil {
        /**
         * 获取系统默认的 <code>X509TrustManager</code> 用于 SSL/TLS 连接中的证书验证
         *
         * @return 返回一个 <code>javax.net.ssl.X509TrustManager</code> 实例, 代表了当前 JVM 环境下的系统默认信任管理器
         * @throws IllegalStateException 如果无法获取到系统默认的信任管理器, 则抛出此异常
         */
        static javax.net.ssl.X509TrustManager systemDefaultTrustManager() {
            try {
                TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
                return (javax.net.ssl.X509TrustManager) tmf.getTrustManagers()[0];
            } catch (Exception e) {
                throw new IllegalStateException("获取系统默认 TrustManager 失败", e);
            }
        }
    }

    /**
     * 配置并返回一个 <code>Retrofit</code> 实例, 用于处理 OAuth 操作中的 HTTP 请求
     *
     * @param mapper 一个经过特别配置的 <code>ObjectMapper</code> 实例, 用于处理 OAuth 操作中的 JSON 数据
     * @param client 一个经过特别配置的 <code>OkHttpClient</code> 实例, 用于处理 OAuth 操作中的 HTTP 请求
     * @return 返回一个已配置好的 <code>Retrofit</code> 实例, 可以用来创建 REST API 客户端
     */
    @Bean
    public Retrofit retrofit(ObjectMapper mapper, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl("https://example.com/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build();
    }

    /**
     * 创建并返回一个实现 <code>IOAuth2TokenApi</code> 接口的 Retrofit 客户端实例
     *
     * @param retrofit 已配置好的 <code>Retrofit</code> 实例
     * @return 返回一个实现了 {@link IOAuth2TokenApi} 接口的 Retrofit 客户端实例, 可以用来执行 OAuth 令牌相关的网络请求
     */
    @Bean
    public IOAuth2TokenApi oauthTokenApi(Retrofit retrofit) {
        return retrofit.create(IOAuth2TokenApi.class);
    }

    /**
     * 创建并返回一个实现 <code>IOidcUserInfoApi</code> 接口的 Retrofit 客户端实例
     *
     * @param retrofit 已配置好的 Retrofit 实例
     * @return 返回一个实现了 {@link IOidcUserInfoApi} 接口的 Retrofit 客户端实例, 可以用来执行 OIDC 用户信息相关的网络请求
     */
    @Bean
    public IOidcUserInfoApi oidcUserInfoApi(Retrofit retrofit) {
        return retrofit.create(IOidcUserInfoApi.class);
    }

    /**
     * 创建并返回一个实现 <code>IExchangeRateApi</code> 接口的 Retrofit 客户端实例
     */
    @Bean
    public IExchangeRateApi exchangeRateApi(Retrofit retrofit) {
        return retrofit.create(IExchangeRateApi.class);
    }

    /**
     * 创建并返回一个实现 {@link IPayPalApi} 接口的 Retrofit 客户端实例
     *
     * <p>说明, PayPal 端点使用 {@code @Url} 传入完整地址, 避免在此处固化 baseUrl</p>
     */
    @Bean
    public IPayPalApi payPalApi(Retrofit retrofit) {
        return retrofit.create(IPayPalApi.class);
    }

    /**
     * 创建并返回一个实现 {@link SeventeenTrackApi} 接口的 Retrofit 客户端实例
     *
     * @param retrofit 已配置好的 Retrofit 实例
     * @return 返回一个实现了 {@link SeventeenTrackApi} 接口的 Retrofit 客户端实例
     */
    @Bean
    public SeventeenTrackApi seventeenTrackApi(Retrofit retrofit) {
        return retrofit.create(SeventeenTrackApi.class);
    }
}

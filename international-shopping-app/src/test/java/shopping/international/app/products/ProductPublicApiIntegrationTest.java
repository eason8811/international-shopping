package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * 用户侧商品浏览接口的 API 级集成测试
 */
class ProductPublicApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @DisplayName("列表接口应支持本地化字段、过滤与排序")
    void listProductsSupportsI18nFilteringAndSorting() {
        HttpHeaders adminHeaders = authHeaders(1L, true, true);
        ProductFixture cheap = seedVariantProduct(adminHeaders, "alpha", "a-zh", new BigDecimal("19.99"));
        ProductFixture expensive = seedVariantProduct(adminHeaders, "beta", "b-zh", new BigDecimal("29.99"));

        HttpHeaders userHeaders = authHeaders(2001L, false, false);
        JsonNode root = doRequest(
                url("/api/v1/products?locale=zh-CN&currency=USD&page=1&size=10&sortBy=PRICE_DESC&categorySlug=dianzi"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.OK
        );

        assertSuccess(root);
        JsonNode data = data(root);
        Assertions.assertThat(data.isArray()).isTrue();
        Assertions.assertThat(data).hasSize(2);
        Assertions.assertThat(data.get(0).path("slug").asText()).isEqualTo(expensive.localizedSlug());
        Assertions.assertThat(data.get(1).path("slug").asText()).isEqualTo(cheap.localizedSlug());

        JsonNode meta = root.path("meta");
        Assertions.assertThat(meta.path("total").asLong()).isEqualTo(2L);
        Assertions.assertThat(meta.path("page").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("价格过滤与分页应按预期工作")
    void listProductsRespectsPriceFilter() {
        HttpHeaders adminHeaders = authHeaders(2L, true, true);
        seedVariantProduct(adminHeaders, "alpha", "alpha-zh", new BigDecimal("15.00"));
        seedVariantProduct(adminHeaders, "beta", "beta-zh", new BigDecimal("35.00"));

        HttpHeaders userHeaders = authHeaders(2002L, false, false);
        JsonNode root = doRequest(
                url("/api/v1/products?locale=zh-CN&currency=USD&page=1&size=1&priceMax=20"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.OK
        );

        assertSuccess(root);
        Assertions.assertThat(data(root)).hasSize(1);
        Assertions.assertThat(data(root).get(0).path("slug").asText()).isEqualTo("alpha-zh");
        Assertions.assertThat(root.path("meta").path("total").asLong()).isEqualTo(1L);
        Assertions.assertThat(root.path("meta").path("size").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("商品详情应输出本地化字段并包含规格/价格")
    void productDetailReturnsLocalizedPayload() {
        HttpHeaders adminHeaders = authHeaders(3L, true, true);
        ProductFixture fixture = seedVariantProduct(adminHeaders, "gamma", "gamma-zh", new BigDecimal("45.50"));

        HttpHeaders userHeaders = authHeaders(2003L, false, false);
        JsonNode root = doRequest(
                url("/api/v1/products/" + fixture.localizedSlug() + "?locale=zh-CN&currency=USD"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.OK
        );

        assertSuccess(root);
        JsonNode detail = data(root);
        Assertions.assertThat(detail.path("slug").asText()).isEqualTo(fixture.localizedSlug());
        Assertions.assertThat(detail.path("categorySlug").asText()).isEqualTo("dianzi");
        Assertions.assertThat(detail.path("specs").isArray()).isTrue();
        Assertions.assertThat(detail.path("specs").get(0).path("values")).isNotEmpty();
        Assertions.assertThat(detail.path("skus")).isNotEmpty();
        Assertions.assertThat(detail.path("skus").get(0).path("price").get(0).path("currency").asText()).isEqualTo("USD");
    }

    @Test
    @DisplayName("缺失 locale 或 currency 时应返回 400 并携带错误码")
    void detailValidatesRequiredParams() {
        HttpHeaders adminHeaders = authHeaders(4L, true, true);
        ProductFixture fixture = seedVariantProduct(adminHeaders, "delta", "delta-zh", new BigDecimal("12.00"));
        HttpHeaders userHeaders = authHeaders(2004L, false, false);

        JsonNode missingLocale = doRequest(
                url("/api/v1/products/" + fixture.slug() + "?currency=USD"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(missingLocale.path("success").asBoolean()).isFalse();
        Assertions.assertThat(missingLocale.path("code").asText()).isEqualTo("BAD_REQUEST");

        JsonNode missingCurrency = doRequest(
                url("/api/v1/products/" + fixture.slug() + "?locale=en-US"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(missingCurrency.path("success").asBoolean()).isFalse();
        Assertions.assertThat(missingCurrency.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("未认证访问商品列表应返回 401，体现鉴权要求")
    void listRequiresAuthentication() {
        JsonNode root = doRequest(
                url("/api/v1/products?locale=en-US&currency=USD"),
                HttpMethod.GET,
                null,
                new HttpHeaders(),
                HttpStatus.UNAUTHORIZED
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("UNAUTHORIZED");
    }
}

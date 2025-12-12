package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 管理端商品维护接口的集成测试，覆盖鉴权、CSRF 与参数校验。
 */
class AdminProductApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @DisplayName("创建商品需要有效的 CSRF，缺失时返回 403")
    void createProductRequiresCsrf() {
        HttpHeaders headers = authHeaders(10L, true, false);
        JsonNode root = doRequest(
                url("/api/v1/admin/products"),
                HttpMethod.POST,
                Map.of(
                        "slug", "no-csrf",
                        "title", "No CSRF",
                        "categoryId", 1
                ),
                headers,
                HttpStatus.FORBIDDEN
        );

        Assertions.assertThat(root.path("code").asText()).isEqualTo("FORBIDDEN");
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("创建商品缺少必填字段时返回 400 且保持统一错误体")
    void createProductValidatesPayload() {
        HttpHeaders headers = authHeaders(11L, true, true);
        JsonNode root = doRequest(
                url("/api/v1/admin/products"),
                HttpMethod.POST,
                Map.of(
                        "title", "",
                        "categoryId", null,
                        "status", "ON_SALE"
                ),
                headers,
                HttpStatus.BAD_REQUEST
        );

        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
        Assertions.assertThat(root.path("message").asText()).isNotBlank();
    }

    @Test
    @DisplayName("完整创建流程返回 201 并可查询到详情")
    void createProductHappyPath() {
        HttpHeaders headers = authHeaders(12L, true, true);
        long categoryId = createCategory(headers, "Shoes", "shoes", "zh-CN", "xiezi");

        JsonNode created = doRequest(
                url("/api/v1/admin/products"),
                HttpMethod.POST,
                Map.of(
                        "slug", "sport-shoe",
                        "title", "Sport Shoe",
                        "subtitle", "Light and fast",
                        "description", "Breathable runner",
                        "categoryId", categoryId,
                        "brand", "Runner",
                        "coverImageUrl", "https://img.example.com/shoe.jpg",
                        "skuType", "SINGLE",
                        "status", "ON_SALE",
                        "tags", java.util.List.of("sport", "shoe")
                ),
                headers,
                HttpStatus.CREATED
        );
        assertSuccess(created);

        long productId = data(created).path("id").asLong();
        JsonNode detail = doRequest(
                url("/api/v1/admin/products/" + productId),
                HttpMethod.GET,
                null,
                headers,
                HttpStatus.OK
        );
        assertSuccess(detail);
        Assertions.assertThat(data(detail).path("slug").asText()).isEqualTo("sport-shoe");
        Assertions.assertThat(data(detail).path("categoryId").asLong()).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("商品列表缺少 keyword/tag 返回 400")
    void listProductsRequireKeywordAndTag() {
        HttpHeaders headers = authHeaders(13L, true, false);
        JsonNode root = doRequest(
                url("/api/v1/admin/products?page=1&size=10"),
                HttpMethod.GET,
                null,
                headers,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("更新状态缺少 CSRF 返回 403")
    void updateStatusRequiresCsrf() {
        HttpHeaders adminHeaders = authHeaders(14L, true, true);
        long categoryId = createCategory(adminHeaders, "Bags", "bags", "zh-CN", "bao");
        long productId = createProduct(adminHeaders, categoryId, "bag-slug", "Bag Title");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/status"),
                HttpMethod.PATCH,
                Map.of("status", "ON_SALE"),
                authHeaders(14L, true, false),
                HttpStatus.FORBIDDEN
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("更新基础信息时 slug 为空返回 400")
    void updateBasicRejectsEmptySlug() {
        HttpHeaders adminHeaders = authHeaders(15L, true, true);
        long categoryId = createCategory(adminHeaders, "Hats", "hats", "zh-CN", "maozi");
        long productId = createProduct(adminHeaders, categoryId, "hat-slug", "Hat Title");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId),
                HttpMethod.PATCH,
                Map.of("slug", " "),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }
}

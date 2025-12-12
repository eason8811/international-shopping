package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class CategoryApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @DisplayName("分类树接口需要登录")
    void treeRequiresAuthentication() {
        JsonNode root = doRequest(
                url("/api/v1/products/categories/tree?locale=zh-CN"),
                HttpMethod.GET,
                null,
                new HttpHeaders(),
                HttpStatus.UNAUTHORIZED
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("分类树缺少 locale 参数返回 400")
    void treeRejectsMissingLocale() {
        HttpHeaders userHeaders = authHeaders(560L, false, false);
        JsonNode root = doRequest(
                url("/api/v1/products/categories/tree"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("分类树按 locale 返回本地化字段")
    void treeReturnsLocalizedFields() {
        HttpHeaders adminHeaders = authHeaders(561L, true, true);
        long categoryId = createCategory(adminHeaders, "Camera", "camera", "zh-CN", "xiangji");

        HttpHeaders userHeaders = authHeaders(562L, false, false);
        JsonNode root = doRequest(
                url("/api/v1/products/categories/tree?locale=zh-CN"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
        Assertions.assertThat(data(root).get(0).path("id").asLong()).isEqualTo(categoryId);
        Assertions.assertThat(data(root).get(0).path("slug").asText()).isEqualTo("xiangji");
        Assertions.assertThat(data(root).get(0).path("locale").asText()).isEqualTo("zh-CN");
    }
}

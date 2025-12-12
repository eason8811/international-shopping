package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

class AdminCategoryApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @DisplayName("分类列表需要登录才能访问")
    void listRequiresAuthentication() {
        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories?page=1&size=5"),
                HttpMethod.GET,
                null,
                new HttpHeaders(),
                HttpStatus.UNAUTHORIZED
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("创建分类需要 CSRF 且返回 201")
    void createCategoryWithCsrf() {
        HttpHeaders adminHeaders = authHeaders(501L, true, true);
        JsonNode created = doRequest(
                url("/api/v1/admin/products/categories"),
                HttpMethod.POST,
                Map.of(
                        "name", "Electronics",
                        "slug", "electronics",
//                        "parentId", null,
                        "sortOrder", 1,
                        "isEnabled", true,
                        "i18n", List.of(Map.of(
                                "locale", "zh-CN",
                                "name", "电子",
                                "slug", "dianzi",
                                "brand", "牌子"
                        ))
                ),
                adminHeaders,
                HttpStatus.CREATED
        );
        assertCreated(created);
        Assertions.assertThat(data(created).path("slug").asText()).isEqualTo("electronics");
    }

    @Test
    @DisplayName("创建分类缺少 CSRF 返回 403")
    void createCategoryWithoutCsrf() {
        HttpHeaders adminHeaders = authHeaders(502L, true, false);
        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories"),
                HttpMethod.POST,
                Map.of("name", "NoCsrf",
                        "slug", "no-csrf",
//                        "parentId", null,
                        "sortOrder", 1,
                        "isEnabled", true
                ),
                adminHeaders,
                HttpStatus.FORBIDDEN
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("创建分类空字段返回 400")
    void createCategoryRejectsEmptyFields() {
        HttpHeaders adminHeaders = authHeaders(503L, true, true);
        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories"),
                HttpMethod.POST,
                Map.of(
                        "name", "",
                        "slug", " ",
//                        "parentId", null,
                        "sortOrder", 1,
                        "isEnabled", true
                ),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("分页参数小于 1 时应回退到安全默认值")
    void listUsesSafePagingDefaults() {
        HttpHeaders adminHeaders = authHeaders(504L, true, true);
        createCategory(adminHeaders, "Books", "books", "zh-CN", "tushu");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories?page=0&size=-1"),
                HttpMethod.GET,
                null,
                adminHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
        Assertions.assertThat(root.path("meta").path("page").asInt()).isEqualTo(1);
        Assertions.assertThat(root.path("meta").path("size").asInt()).isEqualTo(20);
    }

    @Test
    @DisplayName("切换启用状态时 is_enabled 不能为空")
    void toggleEnableRejectsNull() {
        HttpHeaders adminHeaders = authHeaders(505L, true, true);
        long categoryId = createCategory(adminHeaders, "Toys", "toys", "zh-CN", "wanju");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories/" + categoryId + "/enable"),
                HttpMethod.PATCH,
                Map.of(/*"is_enabled", null*/),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }
}

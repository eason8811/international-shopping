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

class AdminSpecApiIntegrationTest extends ProductApiIntegrationTestBase {

    private long prepareProduct(HttpHeaders adminHeaders) {
        long categoryId = createCategory(adminHeaders, "SpecCat", "spec-cat", "en-US", "spec-cat");
        return createProduct(adminHeaders, categoryId, "spec-product", "Spec Product");
    }

    @Test
    @DisplayName("创建规格需要 CSRF 且返回 spec_id")
    void createSpecWithCsrf() {
        HttpHeaders adminHeaders = authHeaders(520L, true, true);
        long productId = prepareProduct(adminHeaders);

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs"),
                HttpMethod.POST,
                Map.of(
                        "specCode", "color",
                        "specName", "Color",
                        "specType", "COLOR",
                        "isRequired", true,
                        "i18nList", List.of(Map.of("locale", "en-US", "specName", "Color"))
                ),
                adminHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
        Assertions.assertThat(data(root).path("specIds").get(0).asLong()).isPositive();
    }

    @Test
    @DisplayName("创建规格缺少 CSRF 返回 403")
    void createSpecWithoutCsrf() {
        HttpHeaders adminHeaders = authHeaders(521L, true, false);
        long productId = prepareProduct(authHeaders(521L, true, true));

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs"),
                HttpMethod.POST,
                Map.of("specCode", "size", "specName", "Size", "specType", "SIZE", "isRequired", false),
                adminHeaders,
                HttpStatus.FORBIDDEN
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("更新规格名称为空返回 400")
    void updateSpecRejectsEmptyName() {
        HttpHeaders adminHeaders = authHeaders(522L, true, true);
        long productId = prepareProduct(adminHeaders);
        long specId = createSpec(adminHeaders, productId, "color", "en-US");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs"),
                HttpMethod.PATCH,
                Map.of(
                        "specId", specId,
                        "specName", " ",
                        "specType", "COLOR",
                        "isRequired", true
                ),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("新增规格值缺失必填字段返回 400")
    void createSpecValueRejectsMissingFields() {
        HttpHeaders adminHeaders = authHeaders(523L, true, true);
        long productId = prepareProduct(adminHeaders);
        long specId = createSpec(adminHeaders, productId, "capacity", "en-US");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs/" + specId + "/values"),
                HttpMethod.POST,
                Map.of(
                        "valueCode", "   ",
                        "valueName", "",
                        "isEnabled", true
                ),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }
}

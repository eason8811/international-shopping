package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class AdminSkuApiIntegrationTest extends ProductApiIntegrationTestBase {

    private ProductFixture setupProductWithSpec(HttpHeaders adminHeaders) {
        return seedVariantProduct(adminHeaders, "sku-base", "sku-base-zh", new BigDecimal("19.99"));
    }

    @Test
    @DisplayName("创建 SKU 需要 CSRF 且返回 201")
    void createSkuWithCsrf() {
        HttpHeaders adminHeaders = authHeaders(540L, true, true);
        ProductFixture fixture = setupProductWithSpec(adminHeaders);

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + fixture.productId() + "/skus"),
                HttpMethod.POST,
                Map.of(
                        "id", 1001,
                        "skuCode", "SKU-1001",
                        "stock", 10,
                        "weight", new BigDecimal("0.50"),
                        "status", "ENABLED",
                        "isDefault", false,
                        "barcode", "BC-1001",
                        "price", List.of(Map.of(
                                "currency", "USD",
                                "listPrice", new BigDecimal("19.99"),
                                "salePrice", new BigDecimal("18.99"),
                                "isActive", true
                        )),
                        "specs", List.of(Map.of(
                                "specId", fixture.specId(),
                                "specCode", "color",
                                "specName", "Color",
                                "valueId", fixture.valueId(),
                                "valueCode", "red",
                                "valueName", "Red"
                        )),
                        "images", List.of(Map.of(
                                "url", "https://img.example.com/sku-1001.jpg",
                                "isMain", true,
                                "sortOrder", 0
                        ))
                ),
                adminHeaders,
                HttpStatus.CREATED
        );
        assertSuccess(root);
        Assertions.assertThat(data(root).path("skuCode").asText()).isEqualTo("SKU-1001");
    }

    @Test
    @DisplayName("创建 SKU 未携带 CSRF 返回 403")
    void createSkuWithoutCsrf() {
        HttpHeaders adminHeaders = authHeaders(541L, true, false);
        ProductFixture fixture = setupProductWithSpec(authHeaders(541L, true, true));

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + fixture.productId() + "/skus"),
                HttpMethod.POST,
                Map.of(
                        "id", 1002,
                        "skuCode", "SKU-1002",
                        "stock", 5,
                        "status", "ENABLED",
                        "isDefault", true,
                        "price", List.of(Map.of(
                                "currency", "USD",
                                "listPrice", new BigDecimal("10.00"),
                                "salePrice", new BigDecimal("9.00")
                        )),
                        "specs", List.of(Map.of(
                                "specId", fixture.specId(),
                                "specCode", "color",
                                "specName", "Color",
                                "valueId", fixture.valueId(),
                                "valueCode", "red",
                                "valueName", "Red"
                        ))
                ),
                adminHeaders,
                HttpStatus.FORBIDDEN
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("库存调整数量非法返回 400")
    void adjustStockRejectsInvalidQuantity() {
        HttpHeaders adminHeaders = authHeaders(542L, true, true);
        ProductFixture fixture = setupProductWithSpec(adminHeaders);
        long skuId = createSku(adminHeaders, fixture.productId(), fixture.specId(), fixture.valueId(),
                "SKU-ADJ", new BigDecimal("12.00"), "color", "red");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + fixture.productId() + "/skus/" + skuId + "/stock"),
                HttpMethod.PATCH,
                Map.of(
                        "mode", "INCREASE",
                        "quantity", 0
                ),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("价格 upsert 币种格式非法返回 400")
    void upsertPriceRejectsInvalidCurrency() {
        HttpHeaders adminHeaders = authHeaders(543L, true, true);
        ProductFixture fixture = setupProductWithSpec(adminHeaders);
        long skuId = createSku(adminHeaders, fixture.productId(), fixture.specId(), fixture.valueId(),
                "SKU-PRICE", new BigDecimal("22.00"), "color", "red");

        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + fixture.productId() + "/skus/" + skuId + "/price"),
                HttpMethod.PATCH,
                List.of(Map.of(
                        "currency", "usd",
                        "listPrice", new BigDecimal("22.00"),
                        "salePrice", new BigDecimal("20.00"),
                        "isActive", true
                )),
                adminHeaders,
                HttpStatus.BAD_REQUEST
        );
        Assertions.assertThat(root.path("success").asBoolean()).isFalse();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("BAD_REQUEST");
    }
}

package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * 产品领域 API 集成测试的通用基类。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>启动携带初始化脚本的 MySQL Testcontainer</li>
 *   <li>注入 {@link TestRestTemplate} 并提供简化的请求/解析工具</li>
 *   <li>统一构造认证/CSRF 头部，确保测试路径覆盖安全校验</li>
 *   <li>在每个用例前清空相关表，避免数据串扰</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public abstract class ProductApiIntegrationTestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value("${security.jwt.secret-base64}")
    private String jwtSecretBase64;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${security.jwt.audience}")
    private String jwtAudience;

    @Value("${security.jwt.access-token-validity-seconds:3600}")
    private long accessTokenValiditySeconds;

    @BeforeEach
    void truncateTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        for (String table : List.of(
                "product_like",
                "product_sku_spec",
                "product_sku_image",
                "product_price",
                "product_sku",
                "product_spec_value_i18n",
                "product_spec_value",
                "product_spec_i18n",
                "product_spec",
                "product_image",
                "product_i18n",
                "product",
                "product_category_i18n",
                "product_category",
                "user_account"
        )) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    protected HttpHeaders authHeaders(long userId, boolean asAdmin, boolean withCsrf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jwt = generateJwt(userId, asAdmin);
        String csrf = withCsrf ? UUID.randomUUID().toString() : null;
        StringBuilder cookie = new StringBuilder("access_token=").append(jwt);
        if (csrf != null) {
            cookie.append("; csrf_token=").append(csrf);
            headers.add("X-CSRF-Token", csrf);
        }
        headers.add(HttpHeaders.COOKIE, cookie.toString());
        return headers;
    }

    protected JsonNode doRequest(String path, HttpMethod method, Object payload, HttpHeaders headers, HttpStatus expected) {
        ResponseEntity<String> resp = restTemplate.exchange(path, method, new HttpEntity<>(payload, headers), String.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(expected);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("响应解析失败: " + e.getMessage(), e);
        }
    }

    protected void assertSuccess(JsonNode root) {
        Assertions.assertThat(root.path("success").asBoolean()).isTrue();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("OK");
    }

    protected void assertCreated(JsonNode root) {
        Assertions.assertThat(root.path("success").asBoolean()).isTrue();
        Assertions.assertThat(root.path("code").asText()).isEqualTo("CREATED");
    }

    protected JsonNode data(JsonNode root) {
        return root.path("data");
    }

    protected long createCategory(HttpHeaders adminHeaders, String name, String slug, String locale, String localizedSlug) {
        JsonNode root = doRequest(
                url("/api/v1/admin/products/categories"),
                HttpMethod.POST,
                Map.of(
                        "name", name,
                        "slug", slug,
//                        "parentId", null,
                        "sortOrder", 1,
                        "isEnabled", true,
                        "i18n", List.of(Map.of(
                                "locale", locale,
                                "name", name + " " + locale,
                                "slug", localizedSlug,
                                "brand", "LocalizedBrand"
                        ))
                ),
                adminHeaders,
                HttpStatus.CREATED
        );
        assertCreated(root);
        return data(root).path("id").asLong();
    }

    protected long createProduct(HttpHeaders adminHeaders, long categoryId, String slug, String title) {
        JsonNode root = doRequest(
                url("/api/v1/admin/products"),
                HttpMethod.POST,
                Map.of(
                        "slug", slug,
                        "title", title,
                        "subtitle", "Sub " + title,
                        "description", "Desc " + title,
                        "categoryId", categoryId,
                        "brand", "BrandX",
                        "coverImageUrl", "https://img.example.com/" + slug + ".jpg",
                        "skuType", "VARIANT",
                        "status", "ON_SALE",
                        "tags", List.of("hot", "tag-" + slug)
                ),
                adminHeaders,
                HttpStatus.CREATED
        );
        assertCreated(root);
        return data(root).path("id").asLong();
    }

    protected void addProductI18n(HttpHeaders adminHeaders, long productId, String locale, String slug, String title) {
        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/i18n"),
                HttpMethod.POST,
                Map.of(
                        "locale", locale,
                        "title", title,
                        "subtitle", "本地化副标题",
                        "description", "本地化描述",
                        "slug", slug,
                        "tags", List.of("本地化")
                ),
                adminHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
    }

    protected long createSpec(HttpHeaders adminHeaders, long productId, String code, String locale) {
        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs"),
                HttpMethod.POST,
                Map.of(
                        "specCode", code,
                        "specName", "Spec " + code,
                        "specType", "COLOR",
                        "isRequired", true,
                        "i18nList", List.of(Map.of(
                                "locale", locale,
                                "specName", "规格" + code
                        ))
                ),
                adminHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
        return data(root).path("specIds").get(0).asLong();
    }

    protected long createSpecValue(HttpHeaders adminHeaders, long productId, long specId, String code, String locale) {
        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/specs/" + specId + "/values"),
                HttpMethod.POST,
                Map.of(
                        "valueCode", code,
                        "valueName", "Value " + code,
                        "attributes", Map.of("hex", "#FFFFFF"),
                        "isEnabled", true,
                        "i18nList", List.of(Map.of(
                                "locale", locale,
                                "valueName", "值" + code
                        ))
                ),
                adminHeaders,
                HttpStatus.OK
        );
        assertSuccess(root);
        return data(root).path("value_id").asLong();
    }

    protected long createSku(HttpHeaders adminHeaders, long productId, long specId, long valueId,
                             String skuCode, BigDecimal price, String specCode, String valueCode) {
        // request DTO 校验要求 id 非空且大于 0, 但领域层会忽略该值并使用数据库自增 ID
        long requestSkuId = Math.abs(Objects.hash(productId, skuCode, System.nanoTime())) + 1;
        JsonNode root = doRequest(
                url("/api/v1/admin/products/" + productId + "/skus"),
                HttpMethod.POST,
                Map.of(
                        "id", requestSkuId,
                        "skuCode", skuCode,
                        "stock", 30,
                        "weight", new BigDecimal("1.20"),
                        "status", "ENABLED",
                        "isDefault", true,
                        "barcode", "BAR-" + skuCode,
                        "price", List.of(Map.of(
                                "currency", "USD",
                                "listPrice", price,
                                "salePrice", price.subtract(new BigDecimal("1.00")),
                                "isActive", true
                        )),
                        "specs", List.of(Map.of(
                                "specId", specId,
                                "specCode", specCode,
                                "specName", "Spec " + specCode,
                                "valueId", valueId,
                                "valueCode", valueCode,
                                "valueName", "Value " + valueCode
                        )),
                        "images", List.of(Map.of(
                                "url", "https://img.example.com/" + skuCode + ".jpg",
                                "isMain", true,
                                "sortOrder", 0
                        ))
                ),
                adminHeaders,
                HttpStatus.CREATED
        );
        assertSuccess(root);
        return data(root).path("id").asLong();
    }

    protected ProductFixture seedVariantProduct(HttpHeaders adminHeaders, String slug, String zhSlug, BigDecimal price) {
        long categoryId = createCategory(adminHeaders, "Electronics", "electronics", "zh-CN", "dianzi");
        long productId = createProduct(adminHeaders, categoryId, slug, "Title " + slug);
        addProductI18n(adminHeaders, productId, "zh-CN", zhSlug, "本地化标题 " + slug);
        long specId = createSpec(adminHeaders, productId, "color", "zh-CN");
        long valueId = createSpecValue(adminHeaders, productId, specId, "red", "zh-CN");
        long skuId = createSku(adminHeaders, productId, specId, valueId, slug.toUpperCase(), price, "color", "red");
        return new ProductFixture(categoryId, productId, specId, valueId, skuId, slug, zhSlug);
    }

    private String generateJwt(long userId, boolean asAdmin) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("uid", userId)
                    .claim("roles", asAdmin ? List.of("ADMIN") : List.of("USER"))
                    .issuer(jwtIssuer)
                    .audience(jwtAudience)
                    .issueTime(new Date())
                    .notBeforeTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(accessTokenValiditySeconds)))
                    .claim("typ", "access")
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(Base64.getDecoder().decode(jwtSecretBase64)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("JWT 创建失败", e);
        }
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected record ProductFixture(long categoryId, long productId, long specId, long valueId, long skuId,
                                    String slug, String localizedSlug) {
    }
}

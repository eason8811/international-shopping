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
 * 点赞接口的幂等性、鉴权与副作用验证。
 */
class ProductLikeApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @DisplayName("点赞/取消点赞应幂等且更新用户点赞列表与数据库副作用")
    void likeAndCancelAreIdempotent() {
        HttpHeaders adminHeaders = authHeaders(20L, true, true);
        ProductFixture fixture = seedVariantProduct(adminHeaders, "omega", "omega-zh", new BigDecimal("18.00"));

        HttpHeaders userHeaders = authHeaders(3001L, false, true);

        JsonNode firstLike = doRequest(
                url("/api/v1/products/" + fixture.productId() + "/like"),
                HttpMethod.PUT,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(firstLike);
        String likedAt = data(firstLike).path("likedAt").asText();
        Assertions.assertThat(data(firstLike).path("liked").asBoolean()).isTrue();

        JsonNode secondLike = doRequest(
                url("/api/v1/products/" + fixture.productId() + "/like"),
                HttpMethod.PUT,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(secondLike);
        Assertions.assertThat(data(secondLike).path("likedAt").asText()).isEqualTo(likedAt);

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_like WHERE user_id=? AND product_id=?",
                Integer.class,
                3001L,
                fixture.productId()
        );
        Assertions.assertThat(likeCount).isEqualTo(1);

        JsonNode likedList = doRequest(
                url("/api/v1/users/me/likes/products?locale=zh-CN&currency=USD&page=1&size=10"),
                HttpMethod.GET,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(likedList);
        Assertions.assertThat(data(likedList)).hasSize(1);

        JsonNode cancel = doRequest(
                url("/api/v1/products/" + fixture.productId() + "/like"),
                HttpMethod.DELETE,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(cancel);
        Assertions.assertThat(data(cancel).path("liked").asBoolean()).isFalse();

        JsonNode cancelAgain = doRequest(
                url("/api/v1/products/" + fixture.productId() + "/like"),
                HttpMethod.DELETE,
                null,
                userHeaders,
                HttpStatus.OK
        );
        assertSuccess(cancelAgain);
        Assertions.assertThat(data(cancelAgain).path("liked").asBoolean()).isFalse();

        Integer afterCancel = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_like WHERE user_id=? AND product_id=?",
                Integer.class,
                3001L,
                fixture.productId()
        );
        Assertions.assertThat(afterCancel).isZero();
    }
}

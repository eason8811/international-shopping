package shopping.international.app.products;

import org.junit.jupiter.api.Test;
import shopping.international.domain.model.enums.products.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductPublicApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    void shouldListOnSaleProductsWithMeta() throws Exception {
        SeedProduct seed = seedOnSaleProduct("camera", "en-US", "USD");

        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.data[0].slug").value(seed.product().getSlug()))
                .andExpect(jsonPath("$.data[0].price_range.currency").value("USD"))
                .andExpect(jsonPath("$.data[0].category_slug").value(
                        seed.category().getI18nList()
                                .stream()
                                .filter(i -> i.getLocale().equals("en-US"))
                                .findFirst()
                                .get()
                                .getSlug()));
    }

    @Test
    void shouldReturnProductDetailWithSkuAndGallery() throws Exception {
        SeedProduct seed = seedOnSaleProduct("laptop", "en-US", "USD");

        mockMvc.perform(
                        get(API_PREFIX + "/products/{slug}", seed.product().getSlug())
                                .param("locale", "en-US")
                                .param("currency", "USD")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value(seed.product().getSlug()))
                .andExpect(jsonPath("$.data.category_slug").value(seed.category().getSlug()))
                .andExpect(jsonPath("$.data.cover_image_url").value(seed.product().getCoverImageUrl()))
                .andExpect(jsonPath("$.data.skus[0].sku_code").value(seed.sku().getSkuCode()))
                .andExpect(jsonPath("$.data.skus[0].prices[0].currency").value("USD"));
    }

    @Test
    void shouldRejectListWhenPriceMinGreaterThanMax() throws Exception {
        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("price_min", "100")
                                .param("price_max", "10")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldFilterByTagAndSortByPriceAsc() throws Exception {
        SeedProduct cheap = seedOnSaleProduct("cheap", "en-US", "USD",
                List.of("cheap"), new BigDecimal("10.00"), new BigDecimal("9.00"));
        SeedProduct expensive = seedOnSaleProduct("expensive", "en-US", "USD",
                List.of("expensive"), new BigDecimal("200.00"), new BigDecimal("180.00"));

        // tag 过滤
        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("tags", "cheap")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].slug").value(cheap.product().getSlug()));

        // 价格升序排序
        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("sort_by", "PRICE_ASC")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value(cheap.product().getSlug()))
                .andExpect(jsonPath("$.data[1].slug").value(expensive.product().getSlug()));
    }

    @Test
    void shouldPaginateAndHideOffShelfProducts() throws Exception {
        SeedProduct p1 = seedOnSaleProduct("page1", "en-US", "USD");
        SeedProduct p2 = seedOnSaleProduct("page2", "en-US", "USD");
        seedProduct("off-shelf", "en-US", "USD", ProductStatus.OFF_SHELF,
                List.of("tag3"), new BigDecimal("50.00"), new BigDecimal("45.00"));

        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("page", "1")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(
                        get(API_PREFIX + "/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("page", "2")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldRejectDetailWhenLocaleMissing() throws Exception {
        SeedProduct seed = seedOnSaleProduct("watch", "en-US", "USD");

        mockMvc.perform(get(API_PREFIX + "/products/{slug}", seed.product().getSlug())
                        .param("currency", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}

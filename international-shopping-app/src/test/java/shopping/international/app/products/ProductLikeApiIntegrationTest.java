package shopping.international.app.products;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductLikeApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    @WithMockUser(username = "123")
    void shouldLikeAndUnlikeProductAndReflectInList() throws Exception {
        SeedProduct seed = seedOnSaleProduct("headphone", "en-US", "USD");

        mockMvc.perform(
                        put(API_PREFIX + "/products/{id}/like", seed.product().getId())
                                .with(csrf())
                                .with(postProcessor -> {
                                    postProcessor.setCookies(new MockCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidWlkIjoxLCJhdWQiOiJ3ZWIiLCJuYmYiOjE3NjU2NDAzNzYsInJvbGVzIjoiVVNFUiIsImlzcyI6InNob3BwaW5nLmludGVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3NjU4OTk1NzYsImlhdCI6MTc2NTY0MDM3NiwiZW1haWwiOiJsZWlsYTk0Njg1NTVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.o6YxvCboadnEHBzNx5BTdj3ooLM-nfnbzt0naC40mNI"));
                                    return postProcessor;
                                })
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(
                        get(API_PREFIX + "/users/me/likes/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .param("page", "1")
                                .param("size", "10")
                                .with(postProcessor -> {
                                    postProcessor.setCookies(new MockCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidWlkIjoxLCJhdWQiOiJ3ZWIiLCJuYmYiOjE3NjU2NDAzNzYsInJvbGVzIjoiVVNFUiIsImlzcyI6InNob3BwaW5nLmludGVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3NjU4OTk1NzYsImlhdCI6MTc2NTY0MDM3NiwiZW1haWwiOiJsZWlsYTk0Njg1NTVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.o6YxvCboadnEHBzNx5BTdj3ooLM-nfnbzt0naC40mNI"));
                                    return postProcessor;
                                })
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(seed.product().getId()))
                .andExpect(jsonPath("$.data[0].liked_at").exists());

        mockMvc.perform(
                        delete(API_PREFIX + "/products/{id}/like", seed.product().getId())
                                .with(csrf())
                                .with(postProcessor -> {
                                    postProcessor.setCookies(new MockCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidWlkIjoxLCJhdWQiOiJ3ZWIiLCJuYmYiOjE3NjU2NDAzNzYsInJvbGVzIjoiVVNFUiIsImlzcyI6InNob3BwaW5nLmludGVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3NjU4OTk1NzYsImlhdCI6MTc2NTY0MDM3NiwiZW1haWwiOiJsZWlsYTk0Njg1NTVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.o6YxvCboadnEHBzNx5BTdj3ooLM-nfnbzt0naC40mNI"));
                                    return postProcessor;
                                })
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));

        mockMvc.perform(
                        get(API_PREFIX + "/users/me/likes/products")
                                .param("locale", "en-US")
                                .param("currency", "USD")
                                .with(postProcessor -> {
                                    postProcessor.setCookies(new MockCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidWlkIjoxLCJhdWQiOiJ3ZWIiLCJuYmYiOjE3NjU2NDAzNzYsInJvbGVzIjoiVVNFUiIsImlzcyI6InNob3BwaW5nLmludGVybmF0aW9uYWwiLCJ0eXAiOiJhY2Nlc3MiLCJleHAiOjE3NjU4OTk1NzYsImlhdCI6MTc2NTY0MDM3NiwiZW1haWwiOiJsZWlsYTk0Njg1NTVAZ21haWwuY29tIiwidXNlcm5hbWUiOiJsZWlsYS11c2VyXzAxIn0.o6YxvCboadnEHBzNx5BTdj3ooLM-nfnbzt0naC40mNI"));
                                    return postProcessor;
                                })
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void shouldRejectLikeWhenUnauthenticated() throws Exception {
        SeedProduct seed = seedOnSaleProduct("tablet", "en-US", "USD");

        mockMvc.perform(
                        put(API_PREFIX + "/products/{id}/like", seed.product().getId())
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}

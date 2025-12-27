package shopping.international.app.products;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import shopping.international.domain.model.aggregate.products.Category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    void shouldReturnLocalizedCategoryTree() throws Exception {
        Category root = seedCategory("Root", "root", "en-US");
        seedChildCategory(root, "Phone", "phone", "en-US");

        ResultActions result = mockMvc.perform(
                get(API_PREFIX + "/products/categories/tree")
                        .param("locale", "en-US")
        );


        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("root-en-US"))
                .andExpect(jsonPath("$.data[0].children[0].slug").value("phone-en-US"))
                .andExpect(jsonPath("$.data[0].children[0].parent_id").value(root.getId()));
    }

    @Test
    void shouldRejectMissingLocaleForTree() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/products/categories/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}

package shopping.international.app.products;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "9001")
class AdminProductApiIntegrationTest extends ProductApiIntegrationTestBase {

    @Test
    void shouldCreateProductAndSkuThroughAdminApis() throws Exception {
        // 1) 创建分类
        String categoryPayload = """
                {
                  "name": "Admin Cat",
                  "slug": "admin-cat",
                  "parent_id": null,
                  "sort_order": 1,
                  "is_enabled": true,
                  "i18n": [
                    {"locale": "en-US", "name": "Admin Cat EN", "slug": "admin-cat-en", "brand": "EN Brand"}
                  ]
                }
                """;
        String categoryBody = mockMvc.perform(
                        post(API_PREFIX + "/admin/products/categories")
                                .with(csrf())
                                .contentType("application/json")
                                .content(categoryPayload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode categoryJson = objectMapper.readTree(categoryBody);
        long categoryId = categoryJson.path("data").path("id").asLong();

        // 2) 创建商品
        String productPayload = """
                {
                  "slug": "admin-created-product",
                  "title": "Admin Created",
                  "subtitle": "Admin Subtitle",
                  "description": "Admin Desc",
                  "category_id": %d,
                  "brand": "AdminBrand",
                  "coverImageUrl": "https://img/admin-product.jpg",
                  "sku_type": "SINGLE",
                  "status": "ON_SALE",
                  "tags": ["t1","t2"]
                }
                """.formatted(categoryId);
        String productBody = mockMvc.perform(
                        post(API_PREFIX + "/admin/products")
                                .with(csrf())
                                .contentType("application/json")
                                .content(productPayload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.category_id").value(categoryId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode productJson = objectMapper.readTree(productBody);
        long productId = productJson.path("data").path("id").asLong();

        // 3) 创建 SKU (注意: DTO 要求 id 非空, 这里传入占位符)
        String skuPayload = """
                {
                  "sku_code": "SKU-ADMIN-1",
                  "stock": 20,
                  "weight": 0.8,
                  "status": "ENABLED",
                  "is_default": true,
                  "barcode": "SKU-ADMIN-1",
                  "price": [
                    {"currency": "USD", "list_price": 120, "sale_price": 99, "is_active": true}
                  ],
                  "specs": [],
                  "images": [
                    {"url": "https://img/admin-product-sku.jpg", "is_main": true, "sort_order": 1}
                  ]
                }
                """;
        mockMvc.perform(
                        post(API_PREFIX + "/admin/products/{productId}/skus", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content(skuPayload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku_code").value("SKU-ADMIN-1"));

        // 4) 管理端详情包含 SKU 与价格
        mockMvc.perform(get(API_PREFIX + "/admin/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skus[0].sku_code").value("SKU-ADMIN-1"))
                .andExpect(jsonPath("$.data.skus[0].price[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
    }

    @Test
    void shouldFlowStatusAndRejectIllegalTransition() throws Exception {
        long categoryId = seedCategory("Status Cat", "status-cat", "en-US").getId();
        String productPayload = """
                {
                  "slug": "status-prod",
                  "title": "Status Prod",
                  "subtitle": "Sub",
                  "description": "Desc",
                  "category_id": %d,
                  "sku_type": "SINGLE",
                  "status": "DRAFT",
                  "tags": []
                }
                """.formatted(categoryId);
        String productBody = mockMvc.perform(
                        post(API_PREFIX + "/admin/products")
                                .with(csrf())
                                .contentType("application/json")
                                .content(productPayload)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long productId = objectMapper.readTree(productBody).path("data").path("id").asLong();

        // 合法: DRAFT -> ON_SALE
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{id}/status", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"status\":\"ON_SALE\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));

        // 非法: ON_SALE -> DRAFT
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{id}/status", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"status\":\"DRAFT\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldUpdateGalleryAndI18n() throws Exception {
        SeedProduct seed = seedOnSaleProduct("gallery-prod", "en-US", "USD");
        long productId = seed.product().getId();

        mockMvc.perform(
                        put(API_PREFIX + "/admin/products/{id}/gallery", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        [
                                          {"url":"https://img/new1.jpg","is_main":true,"sort_order":1},
                                          {"url":"https://img/new2.jpg","is_main":false,"sort_order":2}
                                        ]
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.image_count").value(2));

        mockMvc.perform(
                        post(API_PREFIX + "/admin/products/{id}/i18n", productId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content("""
                                        {"locale":"fr-FR","title":"Titre","subtitle":"Sous","description":"Desc FR","slug":"galerie-fr","tags":["fr1"]}
                                        """)
                )
                .andExpect(result -> {
                    var ex = result.getResolvedException();
                    if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException e) {
                        System.out.println("HMR mostSpecificCause = " + e.getMostSpecificCause());
                        System.out.println("HMR message = " + e.getMessage());
                    } else {
                        System.out.println("ex = " + ex);
                    }
                })
                 .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locale").value("fr-FR"));

        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{id}/i18n", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {"locale":"fr-FR","title":"Titre2","tags":["fr2"]}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locale").value("fr-FR"));
    }

    @Test
    void shouldMaintainSpecsValuesAndSkuPriceStock() throws Exception {
        SeedProduct seed = seedOnSaleProduct("spec-prod", "en-US", "USD");
        long productId = seed.product().getId();
        long skuId = seed.sku().getId();

        // 创建规格
        String specBody = mockMvc.perform(
                        post(API_PREFIX + "/admin/products/{pid}/specs", productId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "spec_code":"color",
                                          "spec_name":"Color",
                                          "spec_type":"COLOR",
                                          "is_required":true,
                                          "i18n_list":[{"locale":"en-US","spec_name":"Color"}]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long specId = objectMapper.readTree(specBody).path("data").path("spec_ids").get(0).asLong();

        // 创建规格值
        String valueBody = mockMvc.perform(
                        post(API_PREFIX + "/admin/products/{pid}/specs/{sid}/values", productId, specId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "value_code":"red",
                                          "value_name":"Red",
                                          "is_enabled":true,
                                          "i18n_list":[{"locale":"en-US","value_name":"Red"}]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long valueId = objectMapper.readTree(valueBody).path("data").path("value_id").asLong();

        // 更新规格值禁用
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{pid}/specs/{sid}/values", productId, specId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "value_id": %d,
                                          "value_code":"red",
                                          "value_name":"Red",
                                          "is_enabled":false
                                        }
                                        """.formatted(valueId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_id").value(valueId));

        // 绑定规格到 SKU
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{pid}/skus/{sid}/specs", productId, skuId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        [
                                          {
                                            "spec_id": %d,
                                            "spec_code":"color",
                                            "spec_name":"Color",
                                            "value_id": %d,
                                            "value_code":"red",
                                            "value_name":"Red"
                                          }
                                        ]
                                        """.formatted(specId, valueId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spec_ids[0]").value(specId));

        // 更新价格
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{pid}/skus/{sid}/price", productId, skuId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        [
                                          {"currency":"USD","list_price":150,"sale_price":120,"is_active":true}
                                        ]
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencies[0]").value("USD"));

        // 调整库存
        mockMvc.perform(
                        patch(API_PREFIX + "/admin/products/{pid}/skus/{sid}/stock", productId, skuId)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {"mode":"SET","quantity":5}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(5));
    }

    @Test
    void shouldRejectMissingSlugOnCreate() throws Exception {
        long categoryId = seedCategory("Admin Cat", "admin-cat-2", "en-US").getId();
        String productPayload = """
                {
                  "title": "No Slug",
                  "category_id": %d,
                  "sku_type": "SINGLE",
                  "status": "ON_SALE"
                }
                """.formatted(categoryId);
        mockMvc.perform(
                        post(API_PREFIX + "/admin/products")
                                .with(csrf())
                                .contentType("application/json")
                                .content(productPayload)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRejectDuplicateProductSlugWithConflict() throws Exception {
        long categoryId = seedCategory("Admin Cat", "admin-cat", "en-US").getId();

        String productPayload = """
                {
                  "slug": "dup-slug",
                  "title": "First",
                  "subtitle": "first",
                  "description": "desc",
                  "category_id": %d,
                  "sku_type": "SINGLE",
                  "status": "ON_SALE",
                  "tags": []
                }
                """.formatted(categoryId);

        mockMvc.perform(
                        post(API_PREFIX + "/admin/products")
                                .with(csrf())
                                .contentType("application/json")
                                .content(productPayload)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post(API_PREFIX + "/admin/products")
                                .with(csrf())
                                .contentType("application/json")
                                .content(productPayload)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithAnonymousUser
    void shouldRequireAuthenticationForAdminApis() throws Exception {
        mockMvc.perform(
                        post(API_PREFIX + "/admin/products/categories")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {"name":"x","slug":"x","sort_order":1,"is_enabled":true}
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }
}

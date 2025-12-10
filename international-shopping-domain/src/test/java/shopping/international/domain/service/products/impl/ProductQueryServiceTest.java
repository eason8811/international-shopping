package shopping.international.domain.service.products.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, LoggingTestWatcher.class})
class ProductQueryServiceTest {

    @Mock
    private IProductRepository productRepository;
    @Mock
    private ISkuRepository skuRepository;
    @InjectMocks
    private ProductQueryService productQueryService;

    @Test
    void getPublicDetailShouldFilterDisabledSpecsAndPrices() {
        ProductSpecValue enabledValue = ProductSpecValue.reconstitute(21L, 1L, 11L, "v1", "Value1", null, 1, true,
                List.of(ProductSpecValueI18n.of("en-US", "Value1")));
        ProductSpecValue disabledValue = ProductSpecValue.reconstitute(22L, 1L, 11L, "v2", "Value2", null, 2, false, List.of());
        ProductSpec enabledSpec = ProductSpec.reconstitute(11L, 1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), List.of(enabledValue, disabledValue));
        ProductSpec disabledSpec = ProductSpec.reconstitute(12L, 1L, "size", "Size", SpecType.SIZE, true, 1, false, List.of(), List.of());
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of(enabledSpec, disabledSpec));
        when(productRepository.findOnSaleBySlug("slug-1", "en-US")).thenReturn(Optional.of(product));
        Sku sku = Sku.reconstitute(100L, 1L, "SKU-100", 3, null, SkuStatus.ENABLED, true, "BAR",
                List.of(ProductPrice.of("USD", new BigDecimal("10"), null, true),
                        ProductPrice.of("EUR", new BigDecimal("11"), null, true)),
                List.of(SkuSpecRelation.of(11L, "color", "Color", 21L, "v1", "Value1")),
                List.of(ProductImage.of("img", true, 0)), LocalDateTime.now(), LocalDateTime.now());
        when(skuRepository.listByProductId(1L, SkuStatus.ENABLED)).thenReturn(List.of(sku));
        when(productRepository.findCategorySlug(2L)).thenReturn("cat");

        IProductQueryService.ProductDetail detail = productQueryService.getPublicDetail("slug-1", "en-US", "USD");

        assertEquals("cat", detail.categorySlug());
        assertEquals(1, detail.product().getSpecs().size());
        assertEquals(1, detail.product().getSpecs().get(0).getValues().size());
        assertEquals(1, detail.skus().get(0).getPrices().size());
        assertEquals("USD", detail.skus().get(0).getPrices().get(0).getCurrency());
    }

    @Test
    void getPublicDetailShouldThrowWhenNotFoundOrNotOnSale() {
        when(productRepository.findOnSaleBySlug(any(), any())).thenReturn(Optional.empty());
        assertThrows(IllegalParamException.class, () -> productQueryService.getPublicDetail("missing", "en-US", "USD"));

        Product product = TestDataFactory.product(1L, 2L, SkuType.SINGLE, ProductStatus.OFF_SHELF, List.of());
        when(productRepository.findOnSaleBySlug("slug-1", "en-US")).thenReturn(Optional.of(product));
        assertThrows(IllegalParamException.class, () -> productQueryService.getPublicDetail("slug-1", "en-US", "USD"));
    }
}

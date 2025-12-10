package shopping.international.domain.service.products.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, LoggingTestWatcher.class})
class ProductServiceTest {

    @Mock
    private IProductRepository productRepository;
    @InjectMocks
    private ProductService productService;

    @Test
    void createBasicShouldPersistNormalizedProduct() {
        Product saved = TestDataFactory.product(1L, 2L, SkuType.SINGLE, ProductStatus.ON_SALE, List.of());
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.createBasic(" slug ", " Title ", null, null, 2L, null, " cover ",
                SkuType.VARIANT, ProductStatus.ON_SALE, List.of(" t1 ", ""));

        assertSame(saved, result);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product captured = captor.getValue();
        assertEquals("slug", captured.getSlug());
        assertEquals(ProductStatus.ON_SALE, captured.getStatus());
        assertEquals(SkuType.VARIANT, captured.getSkuType());
        assertEquals(List.of("t1"), captured.getTags());
    }

    @Test
    void changeStatusShouldPersistTransition() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.SINGLE, ProductStatus.DRAFT, List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.updateBasic(any(Product.class), eq(false))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductStatus status = productService.changeStatus(1L, ProductStatus.ON_SALE);

        assertEquals(ProductStatus.ON_SALE, status);
        verify(productRepository).updateBasic(product, false);
    }

    @Test
    void updateI18nShouldPatchExistingLocale() {
        Product product = Product.create("slug", "Title", null, null, 2L, null, null, SkuType.SINGLE,
                ProductStatus.ON_SALE, List.of(), List.of(ProductImage.of("url", true, 0)), List.of(), List.of(ProductI18n.of("en-US", "Title", null, null, "slug", List.of("tag1"))));
        product.assignId(2L);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        ProductI18n updated = productService.updateI18n(2L, "en-US", "NewTitle", null, null, "slug-updated", List.of("tag2"));

        assertEquals("NewTitle", updated.getTitle());
        assertEquals("slug-updated", updated.getSlug());
        verify(productRepository).updateI18n(2L, updated);
    }
}

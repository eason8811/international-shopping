package shopping.international.domain.service.products.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.IProductSpecRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;
import shopping.international.types.exceptions.ConflictException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, LoggingTestWatcher.class})
class ProductSpecServiceTest {

    @Mock
    private IProductRepository productRepository;
    @Mock
    private IProductSpecRepository productSpecRepository;
    @InjectMocks
    private ProductSpecService productSpecService;

    @Test
    void createShouldValidateAndPersistSpec() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of());
        ProductSpec spec = ProductSpec.create(1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), List.of());
        ProductSpec saved = ProductSpec.reconstitute(10L, 1L, "color", "Color", SpecType.COLOR, true, 1, true, List.of(), List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSpecRepository.save(any(ProductSpec.class))).thenReturn(saved);

        Long id = productSpecService.create(1L, spec);

        assertEquals(10L, id);
        verify(productSpecRepository).save(spec);
    }

    @Test
    void updateShouldPatchSpecAndI18n() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of());
        ProductSpec spec = ProductSpec.reconstitute(5L, 1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSpecRepository.findById(1L, 5L)).thenReturn(Optional.of(spec));
        when(productSpecRepository.update(any(ProductSpec.class), eq(true))).thenAnswer(invocation -> invocation.getArgument(0));

        Long id = productSpecService.update(1L, 5L, "NewColor", SpecType.SIZE, false, 3, false,
                List.of(ProductSpecI18n.of("fr-FR", "Couleur")), true);

        assertEquals(5L, id);
        assertEquals("NewColor", spec.getSpecName());
        assertEquals(SpecType.SIZE, spec.getSpecType());
        assertEquals(2, spec.getI18nList().size());
    }

    @Test
    void deleteShouldBlockWhenSkuBindingsExist() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of());
        ProductSpec spec = ProductSpec.reconstitute(5L, 1L, "color", "Color", SpecType.COLOR, true, 1, true, List.of(), List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSpecRepository.findById(1L, 5L)).thenReturn(Optional.of(spec));
        when(productSpecRepository.hasSkuBindingsForSpec(5L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> productSpecService.delete(1L, 5L));
    }

    @Test
    void updateValueShouldPatchExistingValue() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of());
        ProductSpecValue value = ProductSpecValue.reconstitute(20L, 1L, 5L, "v1", "Value1", Map.of(), 1, true,
                List.of(ProductSpecValueI18n.of("en-US", "Value1")));
        ProductSpec spec = ProductSpec.reconstitute(5L, 1L, "color", "Color", SpecType.COLOR, true, 1, true, List.of(), List.of(value));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSpecRepository.findById(1L, 5L)).thenReturn(Optional.of(spec));
        when(productSpecRepository.updateValue(any(ProductSpecValue.class), eq(true))).thenAnswer(invocation -> invocation.getArgument(0));

        Long id = productSpecService.updateValue(1L, 5L, 20L, null, "New", Map.of("tone", "dark"), 4, false,
                List.of(ProductSpecValueI18n.of("fr-FR", "Valeur")), true);

        assertEquals(20L, id);
        ProductSpecValue updated = spec.getValues().get(0);
        assertEquals("New", updated.getValueName());
        assertEquals(4, updated.getSortOrder());
        assertFalse(updated.isEnabled());
        assertEquals(2, updated.getI18nList().size());
    }

    @Test
    void deleteValueShouldCheckBindings() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.ON_SALE, List.of());
        ProductSpec spec = ProductSpec.reconstitute(5L, 1L, "color", "Color", SpecType.COLOR, true, 1, true, List.of(), List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSpecRepository.findById(1L, 5L)).thenReturn(Optional.of(spec));
        when(productSpecRepository.hasSkuBindingsForValue(30L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> productSpecService.deleteValue(1L, 5L, 30L));
    }
}

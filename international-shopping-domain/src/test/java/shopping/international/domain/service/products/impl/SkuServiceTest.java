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
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, LoggingTestWatcher.class})
class SkuServiceTest {

    @Mock
    private ISkuRepository skuRepository;
    @Mock
    private IProductRepository productRepository;
    @InjectMocks
    private SkuService skuService;

    @Test
    void createShouldPersistAndRefreshStock() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.SINGLE, ProductStatus.ON_SALE, List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Sku saved = TestDataFactory.sku(10L, 1L, 5, true);
        when(skuRepository.save(any(Sku.class))).thenReturn(saved);
        when(skuRepository.sumStockByProduct(1L)).thenReturn(5);

        Sku result = skuService.create(1L, " code ", 5, new BigDecimal("1.1"), SkuStatus.ENABLED, true, " bar ",
                List.of(TestDataFactory.price("USD", new BigDecimal("9.9"), null, true)),
                List.of(SkuSpecRelation.of(1L, "color", "Color", 11L, "red", "Red")),
                List.of(TestDataFactory.image("sku.jpg", true, 0)));

        assertSame(saved, result);
        verify(skuRepository).markDefault(1L, saved.getId());
        verify(productRepository).updateStockTotal(1L, 5);
    }

    @Test
    void updateBasicShouldAdjustStockAndDefault() {
        Sku existing = TestDataFactory.sku(10L, 1L, 5, false);
        when(skuRepository.findById(1L, 10L)).thenReturn(Optional.of(existing));
        when(skuRepository.updateBasic(any(Sku.class), eq(true))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skuRepository.sumStockByProduct(1L)).thenReturn(8);

        Sku updated = skuService.updateBasic(1L, 10L, " new ", 8, new BigDecimal("2.2"),
                SkuStatus.DISABLED, true, " new-bar ", List.of(TestDataFactory.image("img", true, 0)));

        assertEquals(8, updated.getStock());
        assertEquals(SkuStatus.DISABLED, updated.getStatus());
        verify(skuRepository).markDefault(1L, 10L);
        verify(productRepository).updateStockTotal(1L, 8);
    }

    @Test
    void upsertPricesShouldMergeAndReturnAffectedCurrencies() {
        Sku existing = TestDataFactory.sku(10L, 1L, 5, false);
        when(skuRepository.findById(1L, 10L)).thenReturn(Optional.of(existing));
        when(skuRepository.upsertPrices(eq(10L), any())).thenAnswer(invocation -> {
            List<ProductPrice> prices = invocation.getArgument(1);
            return prices.stream().map(ProductPrice::getCurrency).toList();
        });

        List<String> affected = skuService.upsertPrices(1L, 10L, List.of(
                TestDataFactory.price("USD", new BigDecimal("11"), new BigDecimal("9"), true),
                TestDataFactory.price("EUR", new BigDecimal("12"), null, true)
        ));

        assertEquals(List.of("USD", "EUR"), affected);
        BigDecimal usdPrice = existing.getPrices().stream()
                .filter(p -> p.getCurrency().equals("USD"))
                .findFirst()
                .orElseThrow()
                .getListPrice();
        assertEquals(0, usdPrice.compareTo(new BigDecimal("11")));
    }

    @Test
    void adjustStockShouldUpdateRepositoryAndProduct() {
        Sku existing = TestDataFactory.sku(10L, 1L, 3, false);
        when(skuRepository.findById(1L, 10L)).thenReturn(Optional.of(existing));
        when(skuRepository.updateStock(10L, 5)).thenReturn(5);
        when(skuRepository.sumStockByProduct(1L)).thenReturn(5);

        int stock = skuService.adjustStock(1L, 10L, StockAdjustMode.SET, 5);

        assertEquals(5, stock);
        verify(productRepository).updateStockTotal(1L, 5);
    }
}

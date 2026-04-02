package shopping.international.domain.model.aggregate.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class SkuAggregateTest {

    @Test
    void adjustStockShouldFollowModes() {
        Sku sku = Sku.create(1L, "code", 10, new BigDecimal("1.1"), SkuStatus.ENABLED, false, "bar",
                List.of(ProductPrice.of("USD", (long) 9.90, null, true)),
                List.of(SkuSpecRelation.of(1L, "color", "Color", 11L, "red", "Red")),
                List.of(ProductImage.of("url", true, 0)));

        sku.adjustStock(StockAdjustMode.INCREASE, 5);
        assertEquals(15, sku.getStock());

        sku.adjustStock(StockAdjustMode.DECREASE, 3);
        assertEquals(12, sku.getStock());

        sku.adjustStock(StockAdjustMode.SET, 7);
        assertEquals(7, sku.getStock());

        assertThrows(IllegalParamException.class, () -> sku.adjustStock(StockAdjustMode.DECREASE, 8));
    }

    @Test
    void updatePriceShouldValidateSalePrice() {
        Sku sku = Sku.create(1L, "code", 5, null, SkuStatus.ENABLED, false, null,
                List.of(ProductPrice.of("USD", 10L, null, true)),
                List.of(SkuSpecRelation.of(1L, "color", "Color", 11L, "red", "Red")),
                List.of(ProductImage.of("url", true, 0)));

        IllegalParamException ex = assertThrows(IllegalParamException.class,
                () -> sku.patchPrice(Collections.singletonList(ProductPrice.of("USD", 10L, 12L, true))));
        assertTrue(ex.getMessage().contains("促销价不能高于标价"));
        assertThrows(IllegalStateException.class,
                () -> sku.patchPrice(Collections.singletonList(ProductPrice.of("EUR", 10L, null, true))));
    }

    @Test
    void addSpecSelectionShouldPreventDuplicateKey() {
        Sku sku = Sku.create(1L, "code", 5, null, SkuStatus.ENABLED, false, null,
                List.of(ProductPrice.of("USD", 10L, null, true)),
                List.of(SkuSpecRelation.of(1L, "color", "Color", 11L, "red", "Red")),
                List.of(ProductImage.of("url", true, 0)));

        IllegalParamException ex = assertThrows(IllegalParamException.class,
                () -> sku.patchSpecSelection(Collections.singletonList(SkuSpecRelation.of(1L, "color", "Color", 12L, "blue", "Blue"))));
        assertTrue(ex.getMessage().contains("SKU 已存在该规格绑定"));
    }

    @Test
    void assignIdShouldBeIdempotent() {
        Sku sku = Sku.create(1L, "code", 0, null, SkuStatus.ENABLED, false, null,
                List.of(ProductPrice.of("USD", 10L, null, true)),
                List.of(SkuSpecRelation.of(1L, "color", "Color", 11L, "red", "Red")),
                List.of(ProductImage.of("url", true, 0)));

        sku.assignId(10L);
        assertEquals(10L, sku.getId());
        sku.assignId(10L);
        assertThrows(IllegalStateException.class, () -> sku.assignId(11L));
    }
}

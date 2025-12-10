package shopping.international.domain.model.entity.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ProductSpecTest {

    @Test
    void createShouldBindProductAndValues() {
        ProductSpecValue value = ProductSpecValue.create(1L, null, "v1", "Value1", Map.of("c", "r"), 1, true, List.of());
        ProductSpec spec = ProductSpec.create(1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), List.of(value));

        assertEquals(1L, spec.getProductId());
        assertEquals(1L, spec.getValues().get(0).getProductId());
        assertNull(spec.getValues().get(0).getSpecId());
    }

    @Test
    void addValueShouldRejectDuplicateCodes() {
        ProductSpec spec = TestDataFactory.spec(1L, 10L, "color", true);
        ProductSpecValue duplicate = ProductSpecValue.reconstitute(11L, 1L, 10L, "color-1", "Value", Map.of(), 1, true, List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spec.addValue(duplicate));
        assertTrue(ex.getMessage().contains("规格值编码重复"));
    }

    @Test
    void assignIdShouldPropagateToValues() {
        ProductSpecValue value = ProductSpecValue.create(1L, null, "v1", "Value1", Map.of(), 1, true, List.of());
        ProductSpec spec = ProductSpec.create(1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), new ArrayList<>(List.of(value)));

        spec.assignId(99L);
        assertEquals(99L, spec.getId());
        assertEquals(99L, spec.getValues().get(0).getSpecId());
        assertThrows(IllegalStateException.class, () -> spec.assignId(100L));
    }

    @Test
    void updateValueShouldPatchExisting() {
        ProductSpecValue value = ProductSpecValue.create(1L, 10L, "v1", "Value1", Map.of(), 1, true, List.of());
        ProductSpec spec = ProductSpec.reconstitute(10L, 1L, "color", "Color", SpecType.COLOR, true, 1, true,
                List.of(ProductSpecI18n.of("en-US", "Color")), new ArrayList<>(List.of(value)));

        spec.updateValue("v1", "NewValue", Map.of("tone", "dark"), 3, false);
        ProductSpecValue updated = spec.getValues().get(0);
        assertEquals("NewValue", updated.getValueName());
        assertEquals(3, updated.getSortOrder());
        assertFalse(updated.isEnabled());
    }
}

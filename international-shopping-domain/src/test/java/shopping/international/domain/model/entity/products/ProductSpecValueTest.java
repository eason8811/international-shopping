package shopping.international.domain.model.entity.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ProductSpecValueTest {

    @Test
    void updateShouldReplaceAttributesAndFlags() {
        ProductSpecValue value = ProductSpecValue.create(1L, 2L, "v1", "Value1", Map.of("c", "r"), 1, true,
                java.util.List.of(ProductSpecValueI18n.of("en-US", "Value1")));

        value.update("Blue", Map.of("color", "blue"), 3, false);

        assertEquals("Blue", value.getValueName());
        assertEquals("blue", value.getAttributes().get("color"));
        assertEquals(3, value.getSortOrder());
        assertFalse(value.isEnabled());
    }

    @Test
    void bindSpecAndProductShouldBeIdempotent() {
        ProductSpecValue value = ProductSpecValue.create(1L, null, "v1", "Value1", Map.of(), 1, true, java.util.List.of());

        value.bindSpecId(3L);
        value.bindSpecId(3L);
        assertThrows(IllegalStateException.class, () -> value.bindSpecId(4L));

        value.bindProductId(1L);
        value.bindProductId(1L);
        assertThrows(IllegalStateException.class, () -> value.bindProductId(2L));
    }

    @Test
    void assignIdShouldNotAllowOverride() {
        ProductSpecValue value = ProductSpecValue.create(1L, 2L, "v1", "Value1", Map.of(), 1, true, java.util.List.of());

        value.assignId(5L);
        assertEquals(5L, value.getId());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> value.assignId(6L));
        assertTrue(ex.getMessage().contains("规格值已存在 ID"));
    }

    @Test
    void validateShouldRejectBlankFields() {
        assertThrows(IllegalParamException.class, () -> ProductSpecValue.create(1L, 2L, "v1", " ", Map.of(), 1, true, java.util.List.of()));
    }
}

package shopping.international.domain.model.aggregate.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ProductAggregateTest {

    @Test
    void createShouldNormalizeAndFillDefaults() {
        Product product = Product.create(" slug ", " Title ", " Sub ", null, 10L, " Brand ",
                " cover.jpg ", null, null, List.of(" tag1 ", "tag1", " "), List.of(ProductImage.of(" url ", true, 0)),
                List.of(), List.of(ProductI18n.of("en-US", " Title ", null, null, " slug ", List.of(" t1 "))));

        assertNull(product.getId());
        assertEquals("slug", product.getSlug());
        assertEquals("Title", product.getTitle());
        assertEquals(SkuType.SINGLE, product.getSkuType());
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        assertEquals(List.of("tag1"), product.getTags());
        assertEquals("url", product.getGallery().get(0).getUrl());
    }

    @Test
    void changeStatusShouldFollowStateMachine() {
        Product product = TestDataFactory.product(1L, 2L, SkuType.SINGLE, ProductStatus.DRAFT, List.of());

        product.changeStatus(ProductStatus.ON_SALE);
        assertEquals(ProductStatus.ON_SALE, product.getStatus());

        product.changeStatus(ProductStatus.OFF_SHELF);
        assertEquals(ProductStatus.OFF_SHELF, product.getStatus());

        product.changeStatus(ProductStatus.DELETED);
        assertEquals(ProductStatus.DELETED, product.getStatus());
        assertThrows(IllegalStateException.class, () -> product.changeStatus(ProductStatus.ON_SALE));
    }

    @Test
    void updateI18nShouldPatchExistingEntry() {
        Product product = Product.create("slug", "Title", null, null, 1L, null, null,
                SkuType.SINGLE, ProductStatus.ON_SALE, List.of(),
                List.of(), List.of(), new ArrayList<>(List.of(ProductI18n.of("en-US", "Old", null, null, "slug", List.of("old")))));

        product.updateI18n("en-US", "NewTitle", "NewSub", "NewDesc", "new-slug", List.of("new"));

        assertEquals("NewTitle", product.getTitle());
        assertEquals("new-slug", product.getSlug());
        assertEquals(List.of("new"), product.getTags());
        assertThrows(IllegalStateException.class,
                () -> product.updateI18n("fr-FR", "Title", null, null, "slug-fr", null));
    }

    @Test
    void addSpecShouldRejectDuplicateCodes() {
        ProductSpec existing = ProductSpec.create(1L, "color", "Color", SpecType.COLOR, true, 1, true, List.of(), List.of());
        Product product = TestDataFactory.product(1L, 2L, SkuType.VARIANT, ProductStatus.DRAFT, new ArrayList<>(List.of(existing)));

        IllegalParamException ex = assertThrows(IllegalParamException.class,
                () -> product.addSpec(ProductSpec.create(1L, "color", "Dup", SpecType.COLOR, true, 1, true, List.of(), List.of())));
        assertTrue(ex.getMessage().contains("规格编码已存在"));
    }
}

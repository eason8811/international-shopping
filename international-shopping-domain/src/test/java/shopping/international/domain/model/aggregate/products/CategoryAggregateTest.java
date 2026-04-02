package shopping.international.domain.model.aggregate.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.support.LoggingTestWatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class CategoryAggregateTest {

    @Test
    void createShouldDefaultEnabledAndTrim() {
        Category category = Category.create(null, 1, " Name ", " Slug ", 0, " https://oss.example.com/category/slug.jpg ", " Brand ",
                List.of(CategoryI18n.of("en-US", " Name ", " Slug ", " Brand ")));

        assertEquals(CategoryStatus.ENABLED, category.getStatus());
        assertEquals("Name", category.getName());
        assertEquals("Slug", category.getSlug());
        assertEquals("https://oss.example.com/category/slug.jpg", category.getCover());
        category.validate();
    }

    @Test
    void moveToShouldChangeHierarchy() {
        Category category = Category.reconstitute(2L, 1L, "Child", "child", 2, "/1/", 0,
                CategoryStatus.ENABLED, "https://oss.example.com/category/child.jpg", "Brand", List.of(), null, null);

        category.moveTo(3L, 3, "/1/3/");
        assertEquals(3L, category.getParentId());
        assertEquals(3, category.getLevel());
        assertEquals("/1/3/", category.getPath());
    }

    @Test
    void updateI18nShouldRequireExistingLocale() {
        Category category = Category.create(null, 1, "Root", "root", 0, null, null,
                List.of(CategoryI18n.of("en-US", "Root", "root", null)));

        category.updateI18n("en-US", "Root-Updated", "root-updated", null);
        assertEquals("Root-Updated", category.getI18nList().get(0).getName());
        assertThrows(IllegalStateException.class, () -> category.updateI18n("fr-FR", "Name", "slug", null));
    }
}

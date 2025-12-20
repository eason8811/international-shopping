package shopping.international.domain.service.products.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import shopping.international.domain.adapter.repository.products.ICategoryRepository;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.service.products.ICategoryService;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.domain.support.TestDataFactory;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, LoggingTestWatcher.class})
class CategoryServiceTest {

    @Mock
    private ICategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createShouldValidateUniquenessAndBuildPath() {
        Category saved = TestDataFactory.category(3L, null, 1, "Name", "slug");
        when(categoryRepository.existsBySlug(anyString(), isNull())).thenReturn(false);
        when(categoryRepository.existsByParentAndName(isNull(), eq("Name"), isNull())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        Category category = categoryService.create(" Name ", " slug ", null, 0, true,
                List.of(CategoryI18n.of("en-US", "Name", "slug", null)));

        assertSame(saved, category);
        verify(categoryRepository).save(Mockito.argThat(c -> c.getPath() == null && c.getLevel() == 1));
    }

    @Test
    void updateShouldMoveNodeAndReplaceI18n() {
        Category current = Category.reconstitute(2L, 1L, "Child", "child", 2, "/1/", 0, CategoryStatus.ENABLED, null,
                List.of(CategoryI18n.of("en-US", "Child", "child", null)), null, null);
        Category parent = Category.reconstitute(3L, null, "Parent", "parent", 1, "/", 0, CategoryStatus.ENABLED, null, List.of(), null, null);
        Category updated = Category.reconstitute(2L, 3L, "Child-Updated", "child-updated", 2, "/3/", 5, CategoryStatus.DISABLED, null,
                List.of(CategoryI18n.of("en-US", "Child-Updated", "child-updated", null),
                        CategoryI18n.of("fr-FR", "Enfant", "enfant", null)), null, null);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(current)).thenReturn(Optional.of(updated));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsBySlug(anyString(), eq(2L))).thenReturn(false);
        when(categoryRepository.existsByParentAndName(eq(3L), anyString(), eq(2L))).thenReturn(false);
        when(categoryRepository.update(any(Category.class), eq(true), any())).thenReturn(updated);

        Category result = categoryService.update(2L, "Child-Updated", "child-updated", 3L, 5, false,
                List.of(new ICategoryService.CategoryI18nPatch("fr-FR", "Enfant", "enfant", null)));

        assertEquals(3L, result.getParentId());
        assertEquals(CategoryStatus.DISABLED, result.getStatus());
        assertEquals(2, result.getI18nList().size());
    }

    @Test
    void deleteShouldCascadeWhenChildrenExist() {
        Category current = TestDataFactory.category(2L, null, 1, "Name", "slug");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(current));
        when(categoryRepository.listSubtreeIdsForDelete(2L, "/2")).thenReturn(List.of(3L, 2L));
        when(categoryRepository.hasProductsInCategories(any())).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.delete(2L));
        verify(categoryRepository).deleteCascade(List.of(3L, 2L));
    }

    @Test
    void deleteShouldBlockWhenProductsExistInSubtree() {
        Category current = TestDataFactory.category(2L, null, 1, "Name", "slug");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(current));
        when(categoryRepository.listSubtreeIdsForDelete(2L, "/2")).thenReturn(List.of(3L, 2L));
        when(categoryRepository.hasProductsInCategories(any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.delete(2L));
        verify(categoryRepository, Mockito.never()).deleteCascade(anyList());
    }

    @Test
    void getShouldThrowWhenNotFound() {
        when(categoryRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(IllegalParamException.class, () -> categoryService.get(100L));
    }
}

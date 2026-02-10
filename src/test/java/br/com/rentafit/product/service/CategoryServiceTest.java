package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.CategoryDTO;
import br.com.rentafit.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CategoryService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("VESTIDOS_RENTAL")
                .displayName("Vestidos para Aluguel")
                .description("Categoria de vestidos para aluguel")
                .productType(ProductTypeCategory.RENTAL)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create category successfully")
    void testCreate() {
        // Arrange
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        CategoryDTO result = categoryService.create(category);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(categoryId);
        assertThat(result.name()).isEqualTo("VESTIDOS_RENTAL");
        assertThat(result.displayName()).isEqualTo("Vestidos para Aluguel");
        assertThat(result.active()).isTrue();

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should find category by ID")
    void testFindById() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act
        CategoryDTO result = categoryService.findById(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(categoryId);
        assertThat(result.name()).isEqualTo("VESTIDOS_RENTAL");

        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Should throw exception when category not found by ID")
    void testFindByIdNotFound() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.findById(categoryId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Should find all categories")
    void testFindAll() {
        // Arrange
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryDTO> result = categoryService.findAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(categoryId);

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find categories by product type")
    void testFindByProductType() {
        // Arrange
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findByProductType(ProductTypeCategory.RENTAL)).thenReturn(categories);

        // Act
        List<CategoryDTO> result = categoryService.findByProductType(ProductTypeCategory.RENTAL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).productType()).isEqualTo("RENTAL");

        verify(categoryRepository, times(1)).findByProductType(ProductTypeCategory.RENTAL);
    }

    @Test
    @DisplayName("Should find active categories")
    void testFindActiveCategories() {
        // Arrange
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findByActiveTrue()).thenReturn(categories);

        // Act
        List<CategoryDTO> result = categoryService.findActiveCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();

        verify(categoryRepository, times(1)).findByActiveTrue();
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdate() {
        // Arrange
        Category updatedData = Category.builder()
                .displayName("Updated Display Name")
                .description("Updated Description")
                .active(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        CategoryDTO result = categoryService.update(categoryId, updatedData);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void testUpdateNotFound() {
        // Arrange
        Category updatedData = Category.builder().build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(categoryId, updatedData))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDelete() {
        // Arrange
        doNothing().when(categoryRepository).deleteById(categoryId);

        // Act
        categoryService.delete(categoryId);

        // Assert
        verify(categoryRepository, times(1)).deleteById(categoryId);
    }
}


package br.com.rentafit.product.controller;

import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.CategoryDTO;
import br.com.rentafit.product.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CategoryController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController - Unit Tests")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private UUID categoryId;
    private CategoryDTO categoryDTO;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        categoryDTO = CategoryDTO.builder()
                .id(categoryId)
                .name("VESTIDOS_RENTAL")
                .displayName("Vestidos para Aluguel")
                .description("Categoria de vestidos para aluguel")
                .productType("RENTAL")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        category = Category.builder()
                .id(categoryId)
                .name("VESTIDOS_RENTAL")
                .displayName("Vestidos para Aluguel")
                .description("Categoria de vestidos para aluguel")
                .productType(ProductTypeCategory.RENTAL)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create category successfully")
    void testCreate() {
        // Arrange
        when(categoryService.create(any(Category.class))).thenReturn(categoryDTO);

        // Act
        ResponseEntity<CategoryDTO> response = categoryController.create(category);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(categoryId);
        assertThat(response.getBody().name()).isEqualTo("VESTIDOS_RENTAL");

        verify(categoryService, times(1)).create(any(Category.class));
    }

    @Test
    @DisplayName("Should find category by ID")
    void testFindById() {
        // Arrange
        when(categoryService.findById(categoryId)).thenReturn(categoryDTO);

        // Act
        ResponseEntity<CategoryDTO> response = categoryController.findById(categoryId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(categoryId);
        assertThat(response.getBody().name()).isEqualTo("VESTIDOS_RENTAL");

        verify(categoryService, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Should find all categories")
    void testFindAll() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(categoryDTO);
        when(categoryService.findAll()).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.findAll();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).id()).isEqualTo(categoryId);

        verify(categoryService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find categories by product type")
    void testFindByType() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(categoryDTO);
        when(categoryService.findByProductType(ProductTypeCategory.RENTAL)).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.findByType(ProductTypeCategory.RENTAL);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).productType()).isEqualTo("RENTAL");

        verify(categoryService, times(1)).findByProductType(ProductTypeCategory.RENTAL);
    }

    @Test
    @DisplayName("Should find active categories")
    void testFindActive() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(categoryDTO);
        when(categoryService.findActiveCategories()).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.findActive();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).active()).isTrue();

        verify(categoryService, times(1)).findActiveCategories();
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdate() {
        // Arrange
        CategoryDTO updatedDTO = CategoryDTO.builder()
                .id(categoryId)
                .name("VESTIDOS_RENTAL")
                .displayName("Vestidos Updated")
                .description("Updated description")
                .productType("RENTAL")
                .active(true)
                .build();

        when(categoryService.update(eq(categoryId), any(Category.class))).thenReturn(updatedDTO);

        // Act
        ResponseEntity<CategoryDTO> response = categoryController.update(categoryId, category);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().displayName()).isEqualTo("Vestidos Updated");

        verify(categoryService, times(1)).update(eq(categoryId), any(Category.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDelete() {
        // Arrange
        doNothing().when(categoryService).delete(categoryId);

        // Act
        ResponseEntity<Void> response = categoryController.delete(categoryId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(categoryService, times(1)).delete(categoryId);
    }
}


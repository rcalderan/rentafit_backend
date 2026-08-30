package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.domain.enums.ProductCondition;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.rental.RentalItemDTO;
import br.com.rentafit.product.dto.rental.RentalItemDetailsDTO;
import br.com.rentafit.product.dto.rental.RentalItemUpdateDTO;
import br.com.rentafit.product.repository.CategoryRepository;
import br.com.rentafit.product.repository.RentalItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para RentalItemService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RentalItemService - Unit Tests")
class RentalItemServiceTest {

    @Mock
    private RentalItemRepository rentalItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RentalItemService rentalItemService;

    private UUID productId;
    private UUID categoryId;
    private Category category;
    private RentalItem rentalItem;
    private RentalItemDTO rentalItemDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("VESTIDOS_RENTAL")
                .displayName("Vestidos Rental")
                .productType(ProductTypeCategory.RENTAL)
                .active(true)
                .build();

        rentalItem = RentalItem.builder()
                .id(productId)
                .name("Vestido de Noiva")
                .category(category)
                .size("M")
                .color("Branco")
                .brand("Vera Wang")
                .value(new BigDecimal("500.00"))
                .description("Vestido de noiva para aluguel")
                .legacyId(1001)
                .status(ProductStatus.AVAILABLE)
                .notes("Em excelente estado")
                .condition(ProductCondition.EXCELLENT)
                .rentalCount(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        rentalItemDTO = RentalItemDTO.builder()
                .name("Vestido de Noiva")
                .categoryId(categoryId)
                .size("M")
                .color("Branco")
                .brand("Vera Wang")
                .value(new BigDecimal("500.00"))
                .description("Vestido de noiva para aluguel")
                .legacyId(1001)
                .notes("Em excelente estado")
                .build();
    }

    @Test
    @DisplayName("Should create rental item successfully")
    void testCreate() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(rentalItemRepository.findByLegacyId(1001)).thenReturn(Optional.empty());
        when(rentalItemRepository.save(any(RentalItem.class))).thenReturn(rentalItem);

        // Act
        RentalItemDetailsDTO result = rentalItemService.create(rentalItemDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Vestido de Noiva");
        assertThat(result.legacyId()).isEqualTo(1001);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(rentalItemRepository, times(1)).save(any(RentalItem.class));
    }

    @Test
    @DisplayName("Should generate first numeric legacy ID in backend")
    void testGenerateFirstLegacyId() {
        when(rentalItemRepository.findMaxLegacyId()).thenReturn(Optional.empty());

        Integer legacyId = rentalItemService.generateLegacyId();

        assertThat(legacyId).isEqualTo(1);
        verify(rentalItemRepository).lockLegacyIdGeneration();
    }

    @Test
    @DisplayName("Should increment greatest numeric legacy ID in backend")
    void testGenerateNextLegacyId() {
        when(rentalItemRepository.findMaxLegacyId()).thenReturn(Optional.of(1001));

        Integer legacyId = rentalItemService.generateLegacyId();

        assertThat(legacyId).isEqualTo(1002);
        verify(rentalItemRepository).lockLegacyIdGeneration();
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void testCreateCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rentalItemService.create(rentalItemDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(rentalItemRepository, never()).save(any(RentalItem.class));
    }

    @Test
    @DisplayName("Should throw exception when legacy ID already exists")
    void testCreateLegacyIdExists() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(rentalItemRepository.findByLegacyId(1001)).thenReturn(Optional.of(rentalItem));

        // Act & Assert
        assertThatThrownBy(() -> rentalItemService.create(rentalItemDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Legacy ID already exists");

        verify(rentalItemRepository, never()).save(any(RentalItem.class));
    }

    @Test
    @DisplayName("Should find rental item by ID")
    void testFindById() {
        // Arrange
        when(rentalItemRepository.findById(productId)).thenReturn(Optional.of(rentalItem));

        // Act
        RentalItemDetailsDTO result = rentalItemService.findById(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("Vestido de Noiva");

        verify(rentalItemRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should throw exception when rental item not found by ID")
    void testFindByIdNotFound() {
        // Arrange
        when(rentalItemRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rentalItemService.findById(productId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(rentalItemRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should find all rental items with pagination")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalItem> page = new PageImpl<>(List.of(rentalItem), pageable, 1);
        when(rentalItemRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<RentalItemDetailsDTO> result = rentalItemService.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(productId);

        verify(rentalItemRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should update rental item successfully")
    void testUpdate() {
        // Arrange
        RentalItemUpdateDTO updateDTO = new RentalItemUpdateDTO(
                "Vestido Updated",
                null,
                null,
                null,
                null,
                new BigDecimal("550.00"),
                null,
                null,
                null,
                null
        );

        when(rentalItemRepository.findById(productId)).thenReturn(Optional.of(rentalItem));
        when(rentalItemRepository.save(any(RentalItem.class))).thenReturn(rentalItem);

        // Act
        RentalItemDetailsDTO result = rentalItemService.update(productId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(rentalItemRepository, times(1)).findById(productId);
        verify(rentalItemRepository, times(1)).save(any(RentalItem.class));
    }

    @Test
    @DisplayName("Should find rental item by legacy ID")
    void testFindByLegacyId() {
        // Arrange
        when(rentalItemRepository.findByLegacyId(1001)).thenReturn(Optional.of(rentalItem));

        // Act
        RentalItemDetailsDTO result = rentalItemService.findByLegacyId(1001);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.legacyId()).isEqualTo(1001);

        verify(rentalItemRepository, times(1)).findByLegacyId(1001);
    }

    @Test
    @DisplayName("Should check if legacy ID exists")
    void testCheckLegacyIdExists() {
        // Arrange
        when(rentalItemRepository.findByLegacyId(1001)).thenReturn(Optional.of(rentalItem));

        // Act
        boolean result = rentalItemService.checkLegacyIdExists(1001);

        // Assert
        assertThat(result).isTrue();

        verify(rentalItemRepository, times(1)).findByLegacyId(1001);
    }

    @Test
    @DisplayName("Should delete rental item successfully")
    void testDelete() {
        // Arrange
        doNothing().when(rentalItemRepository).deleteById(productId);

        // Act
        rentalItemService.delete(productId);

        // Assert
        verify(rentalItemRepository, times(1)).deleteById(productId);
    }
}


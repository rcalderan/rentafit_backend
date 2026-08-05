package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.RetailProduct;
import br.com.rentafit.product.domain.Stock;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.retail.ProductRetailDTO;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import br.com.rentafit.product.dto.retail.ProductRetailUpdateDTO;
import br.com.rentafit.product.repository.CategoryRepository;
import br.com.rentafit.product.repository.RetailProductRepository;
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

import org.mockito.ArgumentCaptor;

/**
 * Testes unitários para RetailProductService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetailProductService - Unit Tests")
class RetailProductServiceTest {

    @Mock
    private RetailProductRepository retailProductRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RetailProductService retailProductService;

    private UUID productId;
    private UUID categoryId;
    private Category category;
    private RetailProduct retailProduct;
    private ProductRetailDTO productRetailDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("CAMISAS_RETAIL")
                .displayName("Camisas Retail")
                .productType(ProductTypeCategory.RETAIL)
                .active(true)
                .build();

        Stock stock = Stock.builder()
                .quantityAvailable(10)
                .quantityReserved(2)
                .quantityTotal(12)
                .minStockLevel(5)
                .build();

        retailProduct = RetailProduct.builder()
                .id(productId)
                .name("Camisa Polo")
                .category(category)
                .size("M")
                .color("Azul")
                .brand("Lacoste")
                .value(new BigDecimal("150.00"))
                .description("Camisa Polo para venda")
                .sku("SKU001")
                .details("100% algodão")
                .stock(stock)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productRetailDTO = ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(categoryId)
                .size("M")
                .color("Azul")
                .brand("Lacoste")
                .value(new BigDecimal("150.00"))
                .description("Camisa Polo para venda")
                .sku("SKU001")
                .details("100% algodão")
                .build();
    }

    @Test
    @DisplayName("Should create retail product successfully")
    void testCreate() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(retailProductRepository.findBySku("SKU001")).thenReturn(Optional.empty());
        when(retailProductRepository.save(any(RetailProduct.class))).thenReturn(retailProduct);

        // Act
        ProductRetailDetailsDTO result = retailProductService.create(productRetailDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Camisa Polo");
        assertThat(result.sku()).isEqualTo("SKU001");

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(retailProductRepository, times(1)).save(any(RetailProduct.class));
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void testCreateCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> retailProductService.create(productRetailDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(retailProductRepository, never()).save(any(RetailProduct.class));
    }

    @Test
    @DisplayName("Should throw exception when SKU already exists")
    void testCreateSkuExists() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(retailProductRepository.findBySku("SKU001")).thenReturn(Optional.of(retailProduct));

        // Act & Assert
        assertThatThrownBy(() -> retailProductService.create(productRetailDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SKU already exists");

        verify(retailProductRepository, never()).save(any(RetailProduct.class));
    }

    @Test
    @DisplayName("Should find retail product by ID")
    void testFindById() {
        // Arrange
        when(retailProductRepository.findById(productId)).thenReturn(Optional.of(retailProduct));

        // Act
        ProductRetailDetailsDTO result = retailProductService.findById(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("Camisa Polo");

        verify(retailProductRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should throw exception when product not found by ID")
    void testFindByIdNotFound() {
        // Arrange
        when(retailProductRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> retailProductService.findById(productId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(retailProductRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should find all retail products with pagination")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RetailProduct> page = new PageImpl<>(List.of(retailProduct), pageable, 1);
        when(retailProductRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<ProductRetailDetailsDTO> result = retailProductService.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(productId);

        verify(retailProductRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should update retail product successfully")
    void testUpdate() {
        // Arrange
        ProductRetailUpdateDTO updateDTO = new ProductRetailUpdateDTO(
                null,
                "Camisa Updated",
                null,
                null,
                null,
                null,
                new BigDecimal("160.00"),
                null,
                null
        );

        when(retailProductRepository.findById(productId)).thenReturn(Optional.of(retailProduct));
        when(retailProductRepository.save(any(RetailProduct.class))).thenReturn(retailProduct);

        // Act
        ProductRetailDetailsDTO result = retailProductService.update(productId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(retailProductRepository, times(1)).findById(productId);
        verify(retailProductRepository, times(1)).save(any(RetailProduct.class));
    }

    @Test
    @DisplayName("Should find retail product by SKU")
    void testFindBySku() {
        // Arrange
        when(retailProductRepository.findBySku("SKU001")).thenReturn(Optional.of(retailProduct));

        // Act
        ProductRetailDetailsDTO result = retailProductService.findBySku("SKU001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.sku()).isEqualTo("SKU001");

        verify(retailProductRepository, times(1)).findBySku("SKU001");
    }

    @Test
    @DisplayName("Should delete retail product successfully")
    void testDelete() {
        // Arrange
        doNothing().when(retailProductRepository).deleteById(productId);

        // Act
        retailProductService.delete(productId);

        // Assert
        verify(retailProductRepository, times(1)).deleteById(productId);
    }

    @Test
    @DisplayName("Should not throw NPE when listing products without stock (regression: HTTP 500 on /retail)")
    void testFindAllWithProductWithoutStock() {
        // Arrange — produto legado sem linha de estoque no DB
        RetailProduct productWithoutStock = RetailProduct.builder()
                .id(UUID.randomUUID())
                .name("Sem Estoque")
                .category(category)
                .value(new BigDecimal("99.00"))
                .sku("SKU-NO-STOCK")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        assertThat(productWithoutStock.getStock()).isNull();

        Pageable pageable = PageRequest.of(0, 10);
        Page<RetailProduct> page = new PageImpl<>(List.of(productWithoutStock), pageable, 1);
        when(retailProductRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<ProductRetailDetailsDTO> result = retailProductService.findAll(pageable);

        // Assert — antes do fix: NullPointerException; depois: stock null no DTO
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().stock()).isNull();
        assertThat(result.getContent().getFirst().sku()).isEqualTo("SKU-NO-STOCK");
    }

    @Test
    @DisplayName("Should persist product with zeroed stock when DTO does not provide stock")
    void testCreateAssignsZeroedStock() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(retailProductRepository.findBySku("SKU001")).thenReturn(Optional.empty());
        when(retailProductRepository.save(any(RetailProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        retailProductService.create(productRetailDTO);

        // Assert — o produto passado para save() deve ter estoque zerado vinculado
        ArgumentCaptor<RetailProduct> captor = ArgumentCaptor.forClass(RetailProduct.class);
        verify(retailProductRepository, times(1)).save(captor.capture());
        RetailProduct saved = captor.getValue();
        assertThat(saved.getStock()).isNotNull();
        assertThat(saved.getStock().getQuantityAvailable()).isZero();
        assertThat(saved.getStock().getQuantityReserved()).isZero();
        assertThat(saved.getStock().getQuantityTotal()).isZero();
        assertThat(saved.getStock().getProduct()).isSameAs(saved);
    }
}


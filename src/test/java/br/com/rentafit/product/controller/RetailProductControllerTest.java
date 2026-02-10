package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.StockDTO;
import br.com.rentafit.product.dto.retail.ProductRetailDTO;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import br.com.rentafit.product.dto.retail.ProductRetailUpdateDTO;
import br.com.rentafit.product.service.RetailProductService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para RetailProductController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetailProductController - Unit Tests")
class RetailProductControllerTest {

    @Mock
    private RetailProductService retailProductService;

    @InjectMocks
    private RetailProductController retailProductController;

    private UUID productId;
    private ProductRetailDTO productRetailDTO;
    private ProductRetailDetailsDTO productRetailDetailsDTO;
    private ProductRetailUpdateDTO productRetailUpdateDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

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

        StockDTO stockDTO = StockDTO.builder()
                .productId(productId)
                .quantityAvailable(10)
                .quantityReserved(2)
                .quantityTotal(12)
                .minStockLevel(5)
                .location("Armazém A")
                .build();

        productRetailDetailsDTO = ProductRetailDetailsDTO.builder()
                .id(productId)
                .name("Camisa Polo")
                .categoryName("Camisas Retail")
                .size("M")
                .color("Azul")
                .brand("Lacoste")
                .value(new BigDecimal("150.00"))
                .description("Camisa Polo para venda")
                .sku("SKU001")
                .details("100% algodão")
                .stock(stockDTO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productRetailUpdateDTO = new ProductRetailUpdateDTO(
                null,
                "Camisa Polo Updated",
                "G",
                "Verde",
                null,
                null,
                new BigDecimal("160.00"),
                null
        );
    }

    @Test
    @DisplayName("Should create retail product successfully")
    void testCreate() {
        // Arrange
        when(retailProductService.create(any(ProductRetailDTO.class))).thenReturn(productRetailDetailsDTO);

        // Act
        ResponseEntity<ProductRetailDetailsDTO> response = retailProductController.create(productRetailDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(productId);
        assertThat(response.getBody().name()).isEqualTo("Camisa Polo");
        assertThat(response.getBody().sku()).isEqualTo("SKU001");

        verify(retailProductService, times(1)).create(any(ProductRetailDTO.class));
    }

    @Test
    @DisplayName("Should find retail product by ID")
    void testFindById() {
        // Arrange
        when(retailProductService.findById(productId)).thenReturn(productRetailDetailsDTO);

        // Act
        ResponseEntity<ProductRetailDetailsDTO> response = retailProductController.findById(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(productId);
        assertThat(response.getBody().name()).isEqualTo("Camisa Polo");

        verify(retailProductService, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should find all retail products with pagination")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductRetailDetailsDTO> page = new PageImpl<>(List.of(productRetailDetailsDTO), pageable, 1);
        when(retailProductService.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<Page<ProductRetailDetailsDTO>> response = retailProductController.findAll(pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().getFirst().id()).isEqualTo(productId);

        verify(retailProductService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should update retail product successfully")
    void testUpdate() {
        // Arrange
        ProductRetailDetailsDTO updatedProduct = ProductRetailDetailsDTO.builder()
                .id(productId)
                .name("Camisa Polo Updated")
                .size("G")
                .color("Verde")
                .value(new BigDecimal("160.00"))
                .build();

        when(retailProductService.update(eq(productId), any(ProductRetailUpdateDTO.class))).thenReturn(updatedProduct);

        // Act
        ResponseEntity<ProductRetailDetailsDTO> response = retailProductController.update(productId, productRetailUpdateDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Camisa Polo Updated");

        verify(retailProductService, times(1)).update(eq(productId), any(ProductRetailUpdateDTO.class));
    }

    @Test
    @DisplayName("Should find retail product by SKU")
    void testFindBySku() {
        // Arrange
        String sku = "SKU001";
        when(retailProductService.findBySku(sku)).thenReturn(productRetailDetailsDTO);

        // Act
        ResponseEntity<ProductRetailDetailsDTO> response = retailProductController.findByLegacyId(sku);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sku()).isEqualTo("SKU001");

        verify(retailProductService, times(1)).findBySku(sku);
    }

    @Test
    @DisplayName("Should delete retail product successfully")
    void testDelete() {
        // Arrange
        doNothing().when(retailProductService).delete(productId);

        // Act
        ResponseEntity<Void> response = retailProductController.delete(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(retailProductService, times(1)).delete(productId);
    }
}


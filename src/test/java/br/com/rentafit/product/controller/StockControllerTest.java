package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.StockDTO;
import br.com.rentafit.product.dto.StockMovementDTO;
import br.com.rentafit.product.service.StockService;
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
 * Testes unitários para StockController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockController - Unit Tests")
class StockControllerTest {

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockController stockController;

    private UUID productId;
    private UUID userId;
    private StockDTO stockDTO;
    private StockMovementDTO stockMovementDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        stockDTO = StockDTO.builder()
                .productId(productId)
                .quantityAvailable(10)
                .quantityReserved(2)
                .quantityTotal(12)
                .minStockLevel(5)
                .location("Armazém A")
                .lastMovementDate(LocalDateTime.now())
                .build();

        stockMovementDTO = StockMovementDTO.builder()
                .stockId(UUID.randomUUID())
                .type("ENTRADA")
                .quantity(5)
                .movementDate(LocalDateTime.now())
                .userId(userId)
                .notes("Entrada de estoque")
                .build();
    }

    @Test
    @DisplayName("Should get stock by product ID")
    void testGetStock() {
        // Arrange
        when(stockService.getStockByProduct(productId)).thenReturn(stockDTO);

        // Act
        ResponseEntity<StockDTO> response = stockController.getStock(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productId()).isEqualTo(productId);
        assertThat(response.getBody().quantityAvailable()).isEqualTo(10);

        verify(stockService, times(1)).getStockByProduct(productId);
    }

    @Test
    @DisplayName("Should get low stock products")
    void testGetLowStock() {
        // Arrange
        List<StockDTO> lowStockProducts = Arrays.asList(stockDTO);
        when(stockService.getLowStockProducts()).thenReturn(lowStockProducts);

        // Act
        ResponseEntity<List<StockDTO>> response = stockController.getLowStock();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(stockService, times(1)).getLowStockProducts();
    }

    @Test
    @DisplayName("Should get stock movements for a product")
    void testGetMovements() {
        // Arrange
        List<StockMovementDTO> movements = Arrays.asList(stockMovementDTO);
        when(stockService.getStockMovements(productId)).thenReturn(movements);

        // Act
        ResponseEntity<List<StockMovementDTO>> response = stockController.getMovements(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(stockService, times(1)).getStockMovements(productId);
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void testReserve() {
        // Arrange
        doNothing().when(stockService).reserveStock(productId, 5, userId);

        // Act
        ResponseEntity<Void> response = stockController.reserve(productId, 5, userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(stockService, times(1)).reserveStock(productId, 5, userId);
    }

    @Test
    @DisplayName("Should release reservation successfully")
    void testRelease() {
        // Arrange
        doNothing().when(stockService).releaseReservation(productId, 5, userId);

        // Act
        ResponseEntity<Void> response = stockController.release(productId, 5, userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(stockService, times(1)).releaseReservation(productId, 5, userId);
    }

    @Test
    @DisplayName("Should add stock successfully")
    void testAdd() {
        // Arrange
        doNothing().when(stockService).addStock(productId, 10, "Reposição", userId);

        // Act
        ResponseEntity<Void> response = stockController.add(productId, 10, userId, "Reposição");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(stockService, times(1)).addStock(productId, 10, "Reposição", userId);
    }

    @Test
    @DisplayName("Should remove stock successfully")
    void testRemove() {
        // Arrange
        doNothing().when(stockService).removeStock(productId, 5, "Venda", userId);

        // Act
        ResponseEntity<Void> response = stockController.remove(productId, 5, userId, "Venda");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(stockService, times(1)).removeStock(productId, 5, "Venda", userId);
    }
}


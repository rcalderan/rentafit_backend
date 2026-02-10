

package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.RetailProduct;
import br.com.rentafit.product.domain.Stock;
import br.com.rentafit.product.domain.StockMovement;
import br.com.rentafit.product.domain.enums.StockMovementType;
import br.com.rentafit.product.dto.StockDTO;
import br.com.rentafit.product.dto.StockMovementDTO;
import br.com.rentafit.product.repository.StockMovementRepository;
import br.com.rentafit.product.repository.StockRepository;
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
 * Testes unitários para StockService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService - Unit Tests")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockService stockService;

    private UUID productId;
    private UUID userId;
    private Stock stock;
    private RetailProduct product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        product = RetailProduct.builder()
                .id(productId)
                .build();

        stock = Stock.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantityAvailable(10)
                .quantityReserved(2)
                .quantityTotal(12)
                .minStockLevel(5)
                .location("Armazém A")
                .lastMovementDate(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void testReserveStock() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.reserveStock(productId, 5, userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should throw exception when stock not found for reservation")
    void testReserveStockNotFound() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> stockService.reserveStock(productId, 5, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Stock not found for product");

        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("Should release reservation successfully")
    void testReleaseReservation() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.releaseReservation(productId, 2, userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should add stock successfully")
    void testAddStock() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.addStock(productId, 10, "Reposição", userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should remove stock successfully")
    void testRemoveStock() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.removeStock(productId, 5, "Venda", userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should adjust stock successfully")
    void testAdjustStock() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.adjustStock(productId, 15, "Ajuste de inventário", userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should register loss successfully")
    void testRegisterLoss() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(new StockMovement());

        // Act
        stockService.registerLoss(productId, 3, "Produto danificado", userId);

        // Assert
        verify(stockRepository, times(1)).findByProductId(productId);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should get stock by product")
    void testGetStockByProduct() {
        // Arrange
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        // Act
        StockDTO result = stockService.getStockByProduct(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.quantityAvailable()).isEqualTo(10);
        assertThat(result.quantityReserved()).isEqualTo(2);
        assertThat(result.quantityTotal()).isEqualTo(12);

        verify(stockRepository, times(1)).findByProductId(productId);
    }

    @Test
    @DisplayName("Should get low stock products")
    void testGetLowStockProducts() {
        // Arrange
        List<Stock> lowStockItems = Arrays.asList(stock);
        when(stockRepository.findLowStockItems()).thenReturn(lowStockItems);

        // Act
        List<StockDTO> result = stockService.getLowStockProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(productId);

        verify(stockRepository, times(1)).findLowStockItems();
    }

    @Test
    @DisplayName("Should get stock movements for a product")
    void testGetStockMovements() {
        // Arrange
        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID())
                .stock(stock)
                .type(StockMovementType.ENTRADA)
                .quantity(5)
                .movementDate(LocalDateTime.now())
                .userId(userId)
                .notes("Entrada de estoque")
                .build();

        List<StockMovement> movements = Arrays.asList(movement);
        when(stockMovementRepository.findByStockProductIdOrderByMovementDateDesc(productId)).thenReturn(movements);

        // Act
        List<StockMovementDTO> result = stockService.getStockMovements(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("ENTRADA");

        verify(stockMovementRepository, times(1)).findByStockProductIdOrderByMovementDateDesc(productId);
    }
}


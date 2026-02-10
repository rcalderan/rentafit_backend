package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.rental.RentalItemDTO;
import br.com.rentafit.product.dto.rental.RentalItemDetailsDTO;
import br.com.rentafit.product.dto.rental.RentalItemUpdateDTO;
import br.com.rentafit.product.service.RentalItemService;
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
 * Testes unitários para RentalItemController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RentalItemController - Unit Tests")
class RentalItemControllerTest {

    @Mock
    private RentalItemService rentalItemService;

    @InjectMocks
    private RentalItemController rentalItemController;

    private UUID productId;
    private RentalItemDTO rentalItemDTO;
    private RentalItemDetailsDTO rentalItemDetailsDTO;
    private RentalItemUpdateDTO rentalItemUpdateDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        rentalItemDTO = RentalItemDTO.builder()
                .name("Vestido de Noiva")
                .categoryId(categoryId)
                .size("M")
                .color("Branco")
                .brand("Vera Wang")
                .value(new BigDecimal("500.00"))
                .description("Vestido de noiva para aluguel")
                .legacyId("VEST001")
                .notes("Em excelente estado")
                .build();

        rentalItemDetailsDTO = RentalItemDetailsDTO.builder()
                .id(productId)
                .name("Vestido de Noiva")
                .categoryName("Vestidos Rental")
                .size("M")
                .color("Branco")
                .brand("Vera Wang")
                .value(new BigDecimal("500.00"))
                .description("Vestido de noiva para aluguel")
                .legacyId("VEST001")
                .status("AVAILABLE")
                .notes("Em excelente estado")
                .condition("EXCELLENT")
                .rentalCount(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        rentalItemUpdateDTO = new RentalItemUpdateDTO(
                "Vestido de Noiva Updated",
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
    }

    @Test
    @DisplayName("Should create rental item successfully")
    void testCreate() {
        // Arrange
        when(rentalItemService.create(any(RentalItemDTO.class))).thenReturn(rentalItemDetailsDTO);

        // Act
        ResponseEntity<RentalItemDetailsDTO> response = rentalItemController.create(rentalItemDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(productId);
        assertThat(response.getBody().name()).isEqualTo("Vestido de Noiva");
        assertThat(response.getBody().legacyId()).isEqualTo("VEST001");

        verify(rentalItemService, times(1)).create(any(RentalItemDTO.class));
    }

    @Test
    @DisplayName("Should find rental item by ID")
    void testFindById() {
        // Arrange
        when(rentalItemService.findById(productId)).thenReturn(rentalItemDetailsDTO);

        // Act
        ResponseEntity<RentalItemDetailsDTO> response = rentalItemController.findById(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(productId);
        assertThat(response.getBody().name()).isEqualTo("Vestido de Noiva");

        verify(rentalItemService, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should find all rental items with pagination")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalItemDetailsDTO> page = new PageImpl<>(List.of(rentalItemDetailsDTO), pageable, 1);
        when(rentalItemService.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<Page<RentalItemDetailsDTO>> response = rentalItemController.findAll(pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().getFirst().id()).isEqualTo(productId);

        verify(rentalItemService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should update rental item successfully")
    void testUpdate() {
        // Arrange
        RentalItemDetailsDTO updatedItem = RentalItemDetailsDTO.builder()
                .id(productId)
                .name("Vestido de Noiva Updated")
                .categoryName("Vestidos Rental")
                .value(new BigDecimal("550.00"))
                .build();

        when(rentalItemService.update(eq(productId), any(RentalItemUpdateDTO.class))).thenReturn(updatedItem);

        // Act
        ResponseEntity<RentalItemDetailsDTO> response = rentalItemController.update(productId, rentalItemUpdateDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Vestido de Noiva Updated");

        verify(rentalItemService, times(1)).update(eq(productId), any(RentalItemUpdateDTO.class));
    }

    @Test
    @DisplayName("Should find rental item by legacy ID")
    void testFindByLegacyId() {
        // Arrange
        String legacyId = "VEST001";
        when(rentalItemService.findByLegacyId(legacyId)).thenReturn(rentalItemDetailsDTO);

        // Act
        ResponseEntity<RentalItemDetailsDTO> response = rentalItemController.findByLegacyId(legacyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().legacyId()).isEqualTo("VEST001");

        verify(rentalItemService, times(1)).findByLegacyId(legacyId);
    }

    @Test
    @DisplayName("Should delete rental item successfully")
    void testDelete() {
        // Arrange
        doNothing().when(rentalItemService).delete(productId);

        // Act
        ResponseEntity<Void> response = rentalItemController.delete(productId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(rentalItemService, times(1)).delete(productId);
    }
}


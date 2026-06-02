package br.com.rentafit.people.controller;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.people.dto.CustomerAccountHistoryDTO;
import br.com.rentafit.rental.dto.RentalContractSummaryDTO;
import br.com.rentafit.rental.service.RentalContractService;
import br.com.rentafit.sales.dto.SalesOrderSummaryDTO;
import br.com.rentafit.sales.service.SalesOrderService;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do CustomerAccountController usando Mockito.
 * Testa a área self-service do cliente autenticado (minha conta).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAccountController - Área do Cliente")
class CustomerAccountControllerTest {

    @Mock
    private RentalContractService rentalContractService;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private CustomerAccountController controller;

    private UUID customerId;
    private UserAccount principal;
    private RentalContractSummaryDTO rentalDTO;
    private SalesOrderSummaryDTO orderDTO;

    @BeforeEach
    void setUp() {
        customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        principal = new UserAccount();
        principal.setId(customerId);
        principal.setUsername("cliente@example.com");

        rentalDTO = RentalContractSummaryDTO.builder()
                .id(UUID.randomUUID())
                .legacyId("L-20260101-001")
                .contractType(1)
                .status("SIGNED")
                .statusDescription("Assinado")
                .customerId(customerId)
                .customerName("João Silva")
                .eventDate(LocalDate.of(2026, 6, 15))
                .pickupDate(LocalDate.of(2026, 6, 14))
                .returnDate(LocalDate.of(2026, 6, 16))
                .returned(false)
                .totalValue(new BigDecimal("500.00"))
                .paidValue(new BigDecimal("250.00"))
                .createdAt(OffsetDateTime.now().minusDays(30))
                .build();

        orderDTO = SalesOrderSummaryDTO.builder()
                .id(UUID.randomUUID())
                .legacyId("V-20260102-001")
                .status("CONFIRMED")
                .customerId(customerId)
                .customerName("João Silva")
                .totalValue(new BigDecimal("300.00"))
                .paidValue(new BigDecimal("300.00"))
                .itemCount(2)
                .createdAt(OffsetDateTime.now().minusDays(15))
                .build();
    }

    @Test
    @DisplayName("GET /rentals deve retornar 200 com locações paginadas do cliente autenticado")
    void testMyRentals_retorna200() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> page = new PageImpl<>(List.of(rentalDTO), pageable, 1);
        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(page);

        // Act
        ResponseEntity<Page<RentalContractSummaryDTO>> response = controller.myRentals(principal, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).status()).isEqualTo("SIGNED");
        assertThat(response.getBody().getContent().get(0).customerId()).isEqualTo(customerId);
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);

        verify(rentalContractService, times(1)).findByCustomer(customerId, pageable);
    }

    @Test
    @DisplayName("GET /rentals deve retornar página vazia quando cliente não tem locações")
    void testMyRentals_semLocacoes_retornaPaginaVazia() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(emptyPage);

        // Act
        ResponseEntity<Page<RentalContractSummaryDTO>> response = controller.myRentals(principal, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);

        verify(rentalContractService, times(1)).findByCustomer(customerId, pageable);
    }

    @Test
    @DisplayName("GET /rentals deve respeitar paginação personalizada")
    void testMyRentals_paginacaoPersonalizada() {
        // Arrange
        Pageable pageable = PageRequest.of(2, 5); // página 2, tamanho 5
        Page<RentalContractSummaryDTO> page = new PageImpl<>(List.of(rentalDTO), pageable, 11);
        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(page);

        // Act
        ResponseEntity<Page<RentalContractSummaryDTO>> response = controller.myRentals(principal, pageable);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNumber()).isEqualTo(2);
        assertThat(response.getBody().getSize()).isEqualTo(5);
        assertThat(response.getBody().getTotalPages()).isEqualTo(3); // 11 elementos / 5 por página = 3 páginas

        verify(rentalContractService).findByCustomer(customerId, pageable);
    }

    @Test
    @DisplayName("GET /history deve retornar 200 com histórico combinado (locações + pedidos)")
    void testMyHistory_retorna200() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> rentalsPage = new PageImpl<>(List.of(rentalDTO), pageable, 1);
        List<SalesOrderSummaryDTO> orders = List.of(orderDTO);

        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(rentalsPage);
        when(salesOrderService.findByCustomerId(customerId)).thenReturn(orders);

        // Act
        ResponseEntity<CustomerAccountHistoryDTO> response = controller.myHistory(principal, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rentals().getContent()).hasSize(1);
        assertThat(response.getBody().orders()).hasSize(1);
        assertThat(response.getBody().rentals().getContent().get(0).legacyId()).isEqualTo("L-20260101-001");
        assertThat(response.getBody().orders().get(0).legacyId()).isEqualTo("V-20260102-001");

        verify(rentalContractService, times(1)).findByCustomer(customerId, pageable);
        verify(salesOrderService, times(1)).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("GET /history deve retornar dados vazios quando cliente não tem histórico")
    void testMyHistory_semHistorico_retornaVazio() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> emptyRentals = new PageImpl<>(List.of(), pageable, 0);
        List<SalesOrderSummaryDTO> emptyOrders = List.of();

        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(emptyRentals);
        when(salesOrderService.findByCustomerId(customerId)).thenReturn(emptyOrders);

        // Act
        ResponseEntity<CustomerAccountHistoryDTO> response = controller.myHistory(principal, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rentals().getContent()).isEmpty();
        assertThat(response.getBody().orders()).isEmpty();

        verify(rentalContractService, times(1)).findByCustomer(customerId, pageable);
        verify(salesOrderService, times(1)).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("GET /history deve retornar apenas locações quando não há pedidos")
    void testMyHistory_apenasLocacoes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> rentalsPage = new PageImpl<>(List.of(rentalDTO), pageable, 1);
        List<SalesOrderSummaryDTO> emptyOrders = List.of();

        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(rentalsPage);
        when(salesOrderService.findByCustomerId(customerId)).thenReturn(emptyOrders);

        // Act
        ResponseEntity<CustomerAccountHistoryDTO> response = controller.myHistory(principal, pageable);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rentals().getContent()).hasSize(1);
        assertThat(response.getBody().orders()).isEmpty();
    }

    @Test
    @DisplayName("GET /history deve retornar apenas pedidos quando não há locações")
    void testMyHistory_apenasPedidos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> emptyRentals = new PageImpl<>(List.of(), pageable, 0);
        List<SalesOrderSummaryDTO> orders = List.of(orderDTO);

        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(emptyRentals);
        when(salesOrderService.findByCustomerId(customerId)).thenReturn(orders);

        // Act
        ResponseEntity<CustomerAccountHistoryDTO> response = controller.myHistory(principal, pageable);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rentals().getContent()).isEmpty();
        assertThat(response.getBody().orders()).hasSize(1);
    }

    @Test
    @DisplayName("GET /history deve retornar múltiplos registros quando cliente tem histórico extenso")
    void testMyHistory_historicoExtenso() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        RentalContractSummaryDTO rental2 = RentalContractSummaryDTO.builder()
                .id(UUID.randomUUID())
                .legacyId("L-20260103-002")
                .status("RETURNED")
                .customerId(customerId)
                .build();

        SalesOrderSummaryDTO order2 = SalesOrderSummaryDTO.builder()
                .id(UUID.randomUUID())
                .legacyId("V-20260104-002")
                .status("PAID")
                .customerId(customerId)
                .build();

        Page<RentalContractSummaryDTO> rentalsPage = new PageImpl<>(List.of(rentalDTO, rental2), pageable, 2);
        List<SalesOrderSummaryDTO> orders = List.of(orderDTO, order2);

        when(rentalContractService.findByCustomer(customerId, pageable)).thenReturn(rentalsPage);
        when(salesOrderService.findByCustomerId(customerId)).thenReturn(orders);

        // Act
        ResponseEntity<CustomerAccountHistoryDTO> response = controller.myHistory(principal, pageable);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rentals().getContent()).hasSize(2);
        assertThat(response.getBody().orders()).hasSize(2);
        assertThat(response.getBody().rentals().getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /rentals deve usar ID do principal autenticado, nunca de outro cliente")
    void testMyRentals_usaIdDoPrincipal() {
        // Arrange - simula usuário autenticado
        UUID authUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserAccount authPrincipal = new UserAccount();
        authPrincipal.setId(authUserId);
        authPrincipal.setUsername("cliente@example.com");

        Pageable pageable = PageRequest.of(0, 10);
        Page<RentalContractSummaryDTO> page = new PageImpl<>(List.of(rentalDTO), pageable, 1);

        // O ID passado para o serviço deve ser EXATAMENTE o do principal autenticado
        when(rentalContractService.findByCustomer(authUserId, pageable)).thenReturn(page);

        // Act
        ResponseEntity<Page<RentalContractSummaryDTO>> response = controller.myRentals(authPrincipal, pageable);

        // Assert - garante que o ID do principal foi usado (proteção contra IDOR)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rentalContractService).findByCustomer(eq(authUserId), any(Pageable.class));
    }
}

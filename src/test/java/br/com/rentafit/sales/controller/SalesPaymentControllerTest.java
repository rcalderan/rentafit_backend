package br.com.rentafit.sales.controller;

import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentInputDTO;
import br.com.rentafit.sales.service.SalesPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesPaymentController - Unit Tests")
class SalesPaymentControllerTest {

    @Mock
    private SalesPaymentService paymentService;

    @InjectMocks
    private SalesPaymentController controller;

    private UUID orderId;
    private UUID paymentId;
    private SalesPaymentDetailsDTO paymentDetailsDTO;
    private SalesPaymentInputDTO paymentInputDTO;
    private SalesOrderDetailsDTO orderDetailsDTO;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        paymentDetailsDTO = new SalesPaymentDetailsDTO(
                paymentId,
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                "Pix",
                BigDecimal.valueOf(100),
                1,
                UUID.randomUUID(),
                "PENDING",
                "Pendente"
        );

        paymentInputDTO = new SalesPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                BigDecimal.valueOf(100),
                1,
                null,
                "PENDING"
        );

        orderDetailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId)
                .legacyId("V-20260510-1")
                .status("DRAFT")
                .statusDescription("Rascunho")
                .customerId(UUID.randomUUID())
                .customerName("João Silva")
                .subtotal(BigDecimal.valueOf(100))
                .totalValue(BigDecimal.valueOf(100))
                .paidValue(BigDecimal.ZERO)
                .remainingValue(BigDecimal.valueOf(100))
                .items(List.of())
                .payments(List.of(paymentDetailsDTO))
                .build();
    }

    @Test
    @DisplayName("GET / sales/orders/{orderId}/payments deve retornar 200 com parcelas do pedido")
    void testListPayments_returns200() {
        when(paymentService.listPayments(orderId)).thenReturn(List.of(paymentDetailsDTO));

        ResponseEntity<List<SalesPaymentDetailsDTO>> response = controller.listPayments(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        verify(paymentService, times(1)).listPayments(orderId);
    }

    @Test
    @DisplayName("POST / sales/orders/{orderId}/payments deve retornar 200 com parcela adicionada")
    void testAddPayment_returns200() {
        when(paymentService.addPayment(orderId, paymentInputDTO)).thenReturn(orderDetailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.addPayment(orderId, paymentInputDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(orderId);
        verify(paymentService, times(1)).addPayment(orderId, paymentInputDTO);
    }

    @Test
    @DisplayName("PUT / sales/orders/{orderId}/payments/{paymentId} deve retornar 200 com parcela atualizada")
    void testUpdatePayment_returns200() {
        when(paymentService.updatePayment(orderId, paymentId, paymentInputDTO)).thenReturn(orderDetailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.updatePayment(orderId, paymentId, paymentInputDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(orderId);
        verify(paymentService, times(1)).updatePayment(orderId, paymentId, paymentInputDTO);
    }

    @Test
    @DisplayName("DELETE / sales/orders/{orderId}/payments/{paymentId} deve retornar 200 com parcela cancelada")
    void testCancelPayment_returns200() {
        when(paymentService.cancelPayment(orderId, paymentId)).thenReturn(orderDetailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.cancelPayment(orderId, paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(orderId);
        verify(paymentService, times(1)).cancelPayment(orderId, paymentId);
    }
}

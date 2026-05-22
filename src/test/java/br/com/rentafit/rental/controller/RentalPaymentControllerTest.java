package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.dto.RentalPaymentDetailsDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.service.RentalPaymentService;
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
@DisplayName("RentalPaymentController - Unit Tests")
class RentalPaymentControllerTest {

    @Mock
    private RentalPaymentService paymentService;

    @InjectMocks
    private RentalPaymentController controller;

    private UUID contractId;
    private UUID paymentId;
    private RentalPaymentDetailsDTO paymentDetailsDTO;
    private RentalPaymentInputDTO paymentInputDTO;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        paymentDetailsDTO = new RentalPaymentDetailsDTO(
                paymentId,
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                "Pix",
                BigDecimal.valueOf(100),
                1,
                UUID.randomUUID(),
                "COMPLETED",
                "Paga"
        );

        paymentInputDTO = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                BigDecimal.valueOf(100),
                1,
                null,
                "PENDING"
        );
    }

    @Test
    @DisplayName("GET / contracts/{contractId}/payments deve retornar 200 com lista de pagamentos")
    void testListByContract_returns200() {
        when(paymentService.listByContract(contractId)).thenReturn(List.of(paymentDetailsDTO));

        ResponseEntity<List<RentalPaymentDetailsDTO>> response = controller.listByContract(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody().get(0).id()).isEqualTo(paymentId);
        verify(paymentService, times(1)).listByContract(contractId);
    }

    @Test
    @DisplayName("POST / contracts/{contractId}/payments deve retornar 201 com parcela adicionada")
    void testAddPayment_returns201() {
        when(paymentService.addPayment(contractId, paymentInputDTO)).thenReturn(paymentDetailsDTO);

        ResponseEntity<RentalPaymentDetailsDTO> response = controller.addPayment(contractId, paymentInputDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(paymentId);
        verify(paymentService, times(1)).addPayment(contractId, paymentInputDTO);
    }

    @Test
    @DisplayName("PUT / contracts/{contractId}/payments/{paymentId} deve retornar 200 com parcelas atualizadas")
    void testUpdatePayment_returns200() {
        when(paymentService.updatePayment(contractId, paymentId, paymentInputDTO)).thenReturn(List.of(paymentDetailsDTO));

        ResponseEntity<List<RentalPaymentDetailsDTO>> response = controller.updatePayment(contractId, paymentId, paymentInputDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        verify(paymentService, times(1)).updatePayment(contractId, paymentId, paymentInputDTO);
    }

    @Test
    @DisplayName("DELETE / contracts/{contractId}/payments/{paymentId} deve retornar 204")
    void testCancelPayment_returns204() {
        doNothing().when(paymentService).cancelPayment(contractId, paymentId);

        ResponseEntity<Void> response = controller.cancelPayment(contractId, paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(paymentService, times(1)).cancelPayment(contractId, paymentId);
    }
}

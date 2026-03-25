package br.com.rentafit.rental.controller;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.service.RentalContractService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalContractController - Unit Tests")
class RentalContractControllerTest {

    @Mock private RentalContractService contractService;

    @InjectMocks
    private RentalContractController controller;

    private UUID contractId;
    private UUID customerId;
    private RentalContractDetailsDTO detailsDTO;
    private RentalContractDetailsDTO detailsDTOWithWarnings;
    private RentalContractSummaryDTO summaryDTO;
    private CreateRentalContractDTO createDTO;
    private UpdateRentalContractDTO updateDTO;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        detailsDTO = RentalContractDetailsDTO.builder()
                .id(contractId).status("DRAFT").statusDescription("Proposta")
                .customerId(customerId).customerName("Ana Lima")
                .totalValue(BigDecimal.valueOf(500)).paidValue(BigDecimal.ZERO)
                .remainingValue(BigDecimal.valueOf(500))
                .items(List.of()).payments(List.of()).warnings(null)
                .build();

        detailsDTOWithWarnings = RentalContractDetailsDTO.builder()
                .id(contractId).status("SIGNED").statusDescription("Assinado")
                .customerId(customerId).customerName("Ana Lima")
                .totalValue(BigDecimal.valueOf(500)).paidValue(BigDecimal.ZERO)
                .remainingValue(BigDecimal.valueOf(500))
                .items(List.of()).payments(List.of())
                .warnings(List.of("Item 'Vestido' próximo de reserva em 2026-06-05"))
                .build();

        summaryDTO = RentalContractSummaryDTO.builder()
                .id(contractId).status("DRAFT").statusDescription("Proposta")
                .customerId(customerId).customerName("Ana Lima")
                .totalValue(BigDecimal.valueOf(500)).paidValue(BigDecimal.ZERO)
                .build();

        createDTO = new CreateRentalContractDTO(
                customerId, 0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Observação teste",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        BigDecimal.valueOf(500), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", BigDecimal.valueOf(500), 1, null, "PENDING"))
        );

        updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Observação atualizada",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        BigDecimal.valueOf(500), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", BigDecimal.valueOf(500), 1, null, "PENDING"))
        );
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET / deve retornar 200 com página de contratos")
    void testFindAll_returns200() {
        Page<RentalContractSummaryDTO> page = new PageImpl<>(List.of(summaryDTO));
        when(contractService.findAll(any())).thenReturn(page);

        ResponseEntity<Page<RentalContractSummaryDTO>> response = controller.findAll(PageRequest.of(0, 10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} deve retornar 200 com contrato")
    void testFindById_returns200() {
        when(contractService.findById(contractId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.findById(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(contractId);
    }

    @Test
    @DisplayName("GET /{id} deve propagar ResourceNotFoundException quando não encontrado")
    void testFindById_notFound() {
        when(contractService.findById(contractId)).thenThrow(
                new ResourceNotFoundException("RentalContract", "id", contractId.toString()));

        assertThatThrownBy(() -> controller.findById(contractId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST / deve retornar 201 com contrato criado")
    void testCreate_returns201() {
        when(contractService.create(createDTO)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.create(createDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("POST / deve propagar ValidationException em dados inválidos")
    void testCreate_validationError() {
        when(contractService.create(any())).thenThrow(new ValidationException("Cliente não encontrado"));

        assertThatThrownBy(() -> controller.create(createDTO))
                .isInstanceOf(ValidationException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id} deve retornar 200 com contrato atualizado")
    void testUpdate_returns200() {
        when(contractService.update(contractId, updateDTO)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.update(contractId, updateDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── sign ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/sign deve retornar 200 com contrato assinado")
    void testSign_returns200() {
        when(contractService.sign(contractId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.sign(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /{id}/sign deve retornar 200 com warnings quando há conflito de proximidade")
    void testSign_returns200WithWarnings() {
        when(contractService.sign(contractId)).thenReturn(detailsDTOWithWarnings);

        ResponseEntity<RentalContractDetailsDTO> response = controller.sign(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().warnings()).isNotEmpty();
        assertThat(response.getBody().warnings().get(0)).contains("Vestido");
    }

    @Test
    @DisplayName("PATCH /{id}/sign deve propagar ValidationException em conflito bloqueante")
    void testSign_blockingConflict() {
        when(contractService.sign(contractId))
                .thenThrow(new ValidationException("Conflito de reserva: Item 'Vestido' — BLOQUEIO"));

        assertThatThrownBy(() -> controller.sign(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BLOQUEIO");
    }

    // ── finalize ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/finalize deve retornar 200 com contrato finalizado")
    void testFinalize_returns200() {
        when(contractService.finalize(contractId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.finalize(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── processReturn ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/return deve retornar 200 com devolução processada")
    void testProcessReturn_returns200() {
        ReturnContractDTO returnDTO = new ReturnContractDTO(LocalDate.now(), null);
        when(contractService.processReturn(contractId, returnDTO)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.processReturn(contractId, returnDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── duplicate ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/duplicate deve retornar 201 com novo DRAFT")
    void testDuplicate_returns201() {
        when(contractService.duplicate(contractId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.duplicate(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }

    // ── deliverItem ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/items/{itemId}/deliver deve retornar 200 com entrega confirmada")
    void testDeliverItem_returns200() {
        UUID itemId = UUID.randomUUID();
        UUID attendantId = UUID.randomUUID();
        when(contractService.deliverItem(contractId, itemId, attendantId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.deliverItem(contractId, itemId, attendantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── warnings field ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Response sem conflitos NÃO deve ter campo warnings")
    void testNoWarnings_fieldIsNull() {
        when(contractService.sign(contractId)).thenReturn(detailsDTO);

        ResponseEntity<RentalContractDetailsDTO> response = controller.sign(contractId);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().warnings()).isNull();
    }
}


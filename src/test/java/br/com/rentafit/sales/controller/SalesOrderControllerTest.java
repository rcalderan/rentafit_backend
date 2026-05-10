package br.com.rentafit.sales.controller;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.*;
import br.com.rentafit.sales.service.SalesBillingService;
import br.com.rentafit.sales.service.SalesOrderService;
import br.com.rentafit.sales.service.SalesWorkflowService;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrderController - Testes Unitários")
class SalesOrderControllerTest {

    @Mock private SalesOrderService orderService;
    @Mock private SalesWorkflowService workflowService;
    @Mock private SalesBillingService billingService;

    @InjectMocks
    private SalesOrderController controller;

    private UUID orderId;
    private UUID customerId;
    private UUID itemId;
    private SalesOrderDetailsDTO detailsDTO;
    private SalesOrderSummaryDTO summaryDTO;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId     = UUID.randomUUID();

        detailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId).legacyId("V-20260510-1")
                .status("DRAFT").statusDescription("Rascunho")
                .customerId(customerId).customerName("João Silva")
                .subtotal(new BigDecimal("100.00")).totalValue(new BigDecimal("100.00"))
                .paidValue(BigDecimal.ZERO).remainingValue(new BigDecimal("100.00"))
                .discountValue(BigDecimal.ZERO).items(List.of()).payments(List.of())
                .build();

        summaryDTO = SalesOrderSummaryDTO.builder()
                .id(orderId).legacyId("V-20260510-1").status("DRAFT")
                .customerId(customerId).customerName("João Silva")
                .totalValue(new BigDecimal("100.00")).paidValue(BigDecimal.ZERO)
                .itemCount(1)
                .build();
    }

    @Test
    @DisplayName("GET / deve retornar 200 com página de pedidos (sem filtros)")
    void testFindAll_semFiltros_retorna200() {
        Page<SalesOrderSummaryDTO> page = new PageImpl<>(List.of(summaryDTO));
        when(orderService.findAll(any())).thenReturn(page);

        ResponseEntity<Page<SalesOrderSummaryDTO>> response =
                controller.findAll(null, null, null, PageRequest.of(0, 10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(orderService).findAll(any());
        verify(orderService, never()).findWithFilters(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET / deve usar findWithFilters quando filtro de status informado")
    void testFindAll_comFiltroStatus_usaFindWithFilters() {
        Page<SalesOrderSummaryDTO> page = new PageImpl<>(List.of(summaryDTO));
        when(orderService.findWithFilters(eq(SalesOrderStatus.DRAFT), any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<SalesOrderSummaryDTO>> response =
                controller.findAll(SalesOrderStatus.DRAFT, null, null, PageRequest.of(0, 10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderService).findWithFilters(eq(SalesOrderStatus.DRAFT), any(), any(), any());
        verify(orderService, never()).findAll(any());
    }

    @Test
    @DisplayName("GET /{id} deve retornar 200 com pedido")
    void testFindById_retorna200() {
        when(orderService.findById(orderId)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.findById(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("GET /{id} deve propagar ResourceNotFoundException quando não encontrado")
    void testFindById_naoEncontrado() {
        when(orderService.findById(orderId))
                .thenThrow(ResourceNotFoundException.forId("SalesOrder", orderId));

        assertThatThrownBy(() -> controller.findById(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("GET /legacyId/{legacyId} deve retornar 200")
    void testFindByLegacyId_retorna200() {
        when(orderService.findByLegacyId("V-20260510-1")).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.findByLegacyId("V-20260510-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /byCustomer/{customerId} deve retornar lista de pedidos")
    void testFindByCustomer_retornaLista() {
        when(orderService.findByCustomerId(customerId)).thenReturn(List.of(summaryDTO));

        ResponseEntity<List<SalesOrderSummaryDTO>> response = controller.findByCustomer(customerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("POST / deve retornar 201 com pedido criado")
    void testCreate_retorna201() {
        CreateSalesOrderDTO createDTO = new CreateSalesOrderDTO(
                customerId, UUID.randomUUID(), null, BigDecimal.ZERO,
                List.of(new SalesOrderItemInputDTO(UUID.randomUUID(), 1, BigDecimal.ZERO, null, false, null)),
                null);
        when(orderService.create(createDTO)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.create(createDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("POST / deve propagar ValidationException em dados inválidos")
    void testCreate_erroValidacao() {
        CreateSalesOrderDTO createDTO = new CreateSalesOrderDTO(
                customerId, null, null, BigDecimal.ZERO,
                List.of(new SalesOrderItemInputDTO(UUID.randomUUID(), 1, BigDecimal.ZERO, null, false, null)),
                null);
        when(orderService.create(any())).thenThrow(new ValidationException("Cliente não encontrado"));

        assertThatThrownBy(() -> controller.create(createDTO))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("PUT /{id} deve retornar 200 com pedido atualizado")
    void testUpdate_retorna200() {
        UpdateSalesOrderDTO updateDTO = new UpdateSalesOrderDTO(
                customerId, null, BigDecimal.ZERO,
                List.of(new SalesOrderItemInputDTO(UUID.randomUUID(), 1, BigDecimal.ZERO, null, false, null)),
                null);
        when(orderService.update(orderId, updateDTO)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.update(orderId, updateDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /{id}/confirm deve retornar 200")
    void testConfirm_retorna200() {
        when(workflowService.confirm(orderId)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.confirm(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /{id}/confirm deve propagar ValidationException em estoque insuficiente")
    void testConfirm_estoqueInsuficiente() {
        when(workflowService.confirm(orderId))
                .thenThrow(new ValidationException("Estoque insuficiente para SKU-001"));

        assertThatThrownBy(() -> controller.confirm(orderId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SKU-001");
    }

    @Test
    @DisplayName("PATCH /{id}/cancel deve retornar 200")
    void testCancel_retorna200() {
        CancelSalesOrderDTO cancelDTO = new CancelSalesOrderDTO("Desistência");
        when(workflowService.cancel(orderId, cancelDTO)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.cancel(orderId, cancelDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /{id}/items/{itemId}/ready deve retornar 200")
    void testMarkItemReady_retorna200() {
        when(workflowService.markItemReady(orderId, itemId)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.markItemReady(orderId, itemId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /{id}/items/{itemId}/deliver deve retornar 200")
    void testDeliverItem_retorna200() {
        UUID employeeId = UUID.randomUUID();
        when(workflowService.deliverItem(orderId, itemId, employeeId)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.deliverItem(orderId, itemId, employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /{id}/emit-invoice deve retornar 200")
    void testEmitInvoice_retorna200() {
        when(billingService.emitInvoice(orderId)).thenReturn(detailsDTO);

        ResponseEntity<SalesOrderDetailsDTO> response = controller.emitInvoice(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

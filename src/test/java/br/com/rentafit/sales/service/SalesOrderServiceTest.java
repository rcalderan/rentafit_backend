package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.*;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.port.RetailProductPort;
import br.com.rentafit.sales.port.RetailProductPort.RetailProductSnapshot;
import br.com.rentafit.sales.port.SalesCustomerPort;
import br.com.rentafit.sales.port.SalesCustomerPort.CustomerSnapshot;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrderService - Testes Unitários")
class SalesOrderServiceTest {

    @Mock private SalesOrderRepository orderRepository;
    @Mock private SalesCustomerPort customerPort;
    @Mock private RetailProductPort productPort;
    @Mock private SalesMapper mapper;

    @InjectMocks
    private SalesOrderService orderService;

    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private UUID employeeId;
    private CustomerSnapshot customerSnapshot;
    private RetailProductSnapshot productSnapshot;
    private SalesOrder draftOrder;
    private SalesOrderDetailsDTO detailsDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "legacyIdPattern", "yyMMdd");
        orderId     = UUID.randomUUID();
        customerId  = UUID.randomUUID();
        productId   = UUID.randomUUID();
        employeeId  = UUID.randomUUID();

        customerSnapshot = new CustomerSnapshot(customerId, "João Silva", "12345678901");

        productSnapshot = new RetailProductSnapshot(
                productId, "SKU-001", "Camiseta Fitness", "Vestuário",
                "M", "Azul", "Nike", new BigDecimal("89.90"),
                "Camiseta de academia", 30, 10);

        SalesOrderItem item = SalesOrderItem.builder()
                .id(UUID.randomUUID())
                .retailProductId(productId)
                .sku("SKU-001")
                .description("Camiseta Fitness — M Azul")
                .unitPrice(new BigDecimal("89.90"))
                .quantity(2)
                .discountValue(BigDecimal.ZERO)
                .build();

        draftOrder = SalesOrder.builder()
                .id(orderId)
                .customerId(customerId)
                .customerName("João Silva")
                .customerDocument("12345678901")
                .legacyId("V-20260510-1")
                .status(SalesOrderStatus.DRAFT)
                .discountValue(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(item)))
                .payments(new ArrayList<>())
                .build();

        detailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId)
                .legacyId("V-20260510-1")
                .status("DRAFT")
                .statusDescription("Rascunho")
                .customerId(customerId)
                .customerName("João Silva")
                .subtotal(new BigDecimal("179.80"))
                .totalValue(new BigDecimal("179.80"))
                .paidValue(BigDecimal.ZERO)
                .remainingValue(new BigDecimal("179.80"))
                .discountValue(BigDecimal.ZERO)
                .items(List.of())
                .payments(List.of())
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Deve criar pedido com legacyId gerado automaticamente")
        void deveCriarComLegacyIdGerado() {
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 2, BigDecimal.ZERO, employeeId, false, null);
            CreateSalesOrderDTO dto = new CreateSalesOrderDTO(
                    customerId, employeeId, "Obs", BigDecimal.ZERO, List.of(itemInput), null);

            SalesOrder orderSemId = SalesOrder.builder()
                    .id(orderId).customerId(customerId)
                    .status(SalesOrderStatus.DRAFT)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();

            when(customerPort.findById(customerId)).thenReturn(Optional.of(customerSnapshot));
            when(mapper.toEntity(dto, customerSnapshot)).thenReturn(orderSemId);
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(mapper.toItemEntity(any(), any(), eq(productSnapshot))).thenReturn(draftOrder.getItems().get(0));
            when(orderRepository.countByLegacyIdPrefix(any())).thenReturn(0L);
            when(orderRepository.save(any(SalesOrder.class))).thenReturn(draftOrder);
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = orderService.create(dto);

            assertThat(result).isNotNull();
            assertThat(orderSemId.getLegacyId()).startsWith("V-");
            verify(orderRepository).save(any(SalesOrder.class));
        }

        @Test
        @DisplayName("Deve criar pedido sem cliente (venda balcão)")
        void deveCriarSemCliente() {
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 1, BigDecimal.ZERO, employeeId, false, null);
            CreateSalesOrderDTO dto = new CreateSalesOrderDTO(
                    null, employeeId, null, BigDecimal.ZERO, List.of(itemInput), null);

            SalesOrder orderBalcao = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.DRAFT)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();

            when(mapper.toEntity(dto, null)).thenReturn(orderBalcao);
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(mapper.toItemEntity(any(), any(), eq(productSnapshot))).thenReturn(draftOrder.getItems().get(0));
            when(orderRepository.countByLegacyIdPrefix(any())).thenReturn(0L);
            when(orderRepository.save(any())).thenReturn(orderBalcao);
            when(mapper.toDetailsDTO(orderBalcao, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = orderService.create(dto);

            assertThat(result).isNotNull();
            verify(customerPort, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando produto não encontrado")
        void deveLancarQuandoProdutoNaoEncontrado() {
            UUID produtoInexistente = UUID.randomUUID();
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    produtoInexistente, 1, BigDecimal.ZERO, employeeId, false, null);
            CreateSalesOrderDTO dto = new CreateSalesOrderDTO(
                    customerId, employeeId, null, BigDecimal.ZERO, List.of(itemInput), null);

            SalesOrder order = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.DRAFT)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();

            when(customerPort.findById(customerId)).thenReturn(Optional.of(customerSnapshot));
            when(mapper.toEntity(dto, customerSnapshot)).thenReturn(order);
            when(productPort.findById(produtoInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(produtoInexistente.toString());
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando cliente não encontrado")
        void deveLancarQuandoClienteNaoEncontrado() {
            UUID clienteInexistente = UUID.randomUUID();
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 1, BigDecimal.ZERO, employeeId, false, null);
            CreateSalesOrderDTO dto = new CreateSalesOrderDTO(
                    clienteInexistente, employeeId, null, BigDecimal.ZERO, List.of(itemInput), null);

            when(customerPort.findById(clienteInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(clienteInexistente.toString());
        }

        @Test
        @DisplayName("LegacyId deve ser V-YYYYMMDD-2 quando já existe 1 pedido no mesmo dia")
        void deveLegacyIdSequencial() {
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 1, BigDecimal.ZERO, employeeId, false, null);
            CreateSalesOrderDTO dto = new CreateSalesOrderDTO(
                    null, employeeId, null, BigDecimal.ZERO, List.of(itemInput), null);

            SalesOrder order = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.DRAFT)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();

            when(mapper.toEntity(dto, null)).thenReturn(order);
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(mapper.toItemEntity(any(), any(), any())).thenReturn(draftOrder.getItems().get(0));
            when(orderRepository.countByLegacyIdPrefix(any())).thenReturn(1L);
            when(orderRepository.save(any())).thenReturn(order);
            when(mapper.toDetailsDTO(any(), any())).thenReturn(detailsDTO);

            orderService.create(dto);

            assertThat(order.getLegacyId()).endsWith("-2");
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Deve atualizar pedido em status DRAFT")
        void deveAtualizarDraft() {
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 1, BigDecimal.ZERO, employeeId, false, null);
            UpdateSalesOrderDTO updateDTO = new UpdateSalesOrderDTO(
                    customerId, "Nova obs", BigDecimal.ZERO, List.of(itemInput), null);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            when(customerPort.findById(customerId)).thenReturn(Optional.of(customerSnapshot));
            doNothing().when(mapper).updateEntityFromDTO(any(), any(), any());
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(mapper.toItemEntity(any(), any(), any())).thenReturn(draftOrder.getItems().get(0));
            when(orderRepository.save(draftOrder)).thenReturn(draftOrder);
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = orderService.update(orderId, updateDTO);

            assertThat(result).isNotNull();
            verify(orderRepository).save(draftOrder);
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao tentar editar pedido não-DRAFT")
        void deveLancarQuandoNaoDraft() {
            draftOrder.setStatus(SalesOrderStatus.CONFIRMED);
            SalesOrderItemInputDTO itemInput = new SalesOrderItemInputDTO(
                    productId, 1, BigDecimal.ZERO, employeeId, false, null);
            UpdateSalesOrderDTO updateDTO = new UpdateSalesOrderDTO(
                    customerId, null, BigDecimal.ZERO, List.of(itemInput), null);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));

            assertThatThrownBy(() -> orderService.update(orderId, updateDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("CONFIRMED");
        }
    }

    // ── findById / findByLegacyId ─────────────────────────────────────────────

    @Nested
    @DisplayName("findById e findByLegacyId")
    class FindById {

        @Test
        @DisplayName("Deve retornar pedido quando encontrado por UUID")
        void deveRetornarQuandoEncontrado() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = orderService.findById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando não encontrado por UUID")
        void deveLancarQuandoNaoEncontrado() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve retornar pedido quando encontrado por legacyId")
        void deveRetornarPorLegacyId() {
            when(orderRepository.findByLegacyId("V-20260510-1")).thenReturn(Optional.of(draftOrder));
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = orderService.findByLegacyId("V-20260510-1");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando legacyId não encontrado")
        void deveLancarQuandoLegacyIdNaoEncontrado() {
            when(orderRepository.findByLegacyId("V-INEXISTENTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findByLegacyId("V-INEXISTENTE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("V-INEXISTENTE");
        }
    }

    @Nested
    @DisplayName("findWithFilters")
    class FindWithFilters {

        @Test
        @DisplayName("Deve consultar por status quando não há filtro de data")
        void deveConsultarPorStatusSemFiltroDeData() {
            var pageable = PageRequest.of(0, 20);
            when(orderRepository.findByStatus(SalesOrderStatus.CANCELLED, pageable))
                    .thenReturn(new PageImpl<>(List.of(draftOrder), pageable, 1));
            when(mapper.toSummaryDTO(draftOrder)).thenReturn(SalesOrderSummaryDTO.builder()
                    .id(orderId).status("CANCELLED").build());

            var result = orderService.findWithFilters(SalesOrderStatus.CANCELLED, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByStatus(SalesOrderStatus.CANCELLED, pageable);
            verify(orderRepository, never()).findWithFilters(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve consultar com filtros de data quando informados")
        void deveConsultarComFiltrosDeData() {
            var pageable = PageRequest.of(0, 20);
            var dateFrom = java.time.OffsetDateTime.now().minusDays(7);
            var dateTo = java.time.OffsetDateTime.now();

            when(orderRepository.findWithFilters(null, dateFrom, dateTo, pageable))
                    .thenReturn(new PageImpl<>(List.of(draftOrder), pageable, 1));
            when(mapper.toSummaryDTO(draftOrder)).thenReturn(SalesOrderSummaryDTO.builder()
                    .id(orderId).status("DRAFT").build());

            var result = orderService.findWithFilters(null, dateFrom, dateTo, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findWithFilters(null, dateFrom, dateTo, pageable);
            verify(orderRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("Deve consultar com filtros combinados (status + data)")
        void deveConsultarComStatusEData() {
            var pageable = PageRequest.of(0, 20);
            var dateFrom = java.time.OffsetDateTime.now().minusDays(7);

            when(orderRepository.findWithFilters(SalesOrderStatus.CONFIRMED, dateFrom, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            var result = orderService.findWithFilters(SalesOrderStatus.CONFIRMED, dateFrom, null, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(orderRepository).findWithFilters(SalesOrderStatus.CONFIRMED, dateFrom, null, pageable);
        }
    }

    // ── findByCustomerId ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCustomerId")
    class FindByCustomerId {

        @Test
        @DisplayName("Deve retornar lista de pedidos do cliente")
        void deveRetornarListaDoPedidosDoCliente() {
            SalesOrderSummaryDTO summary = SalesOrderSummaryDTO.builder()
                    .id(orderId).legacyId("V-20260510-1").status("DRAFT")
                    .customerId(customerId).build();

            when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(List.of(draftOrder));
            when(mapper.toSummaryDTO(draftOrder)).thenReturn(summary);

            List<SalesOrderSummaryDTO> result = orderService.findByCustomerId(customerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).customerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando cliente não tem pedidos")
        void deveRetornarListaVazia() {
            when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(List.of());

            List<SalesOrderSummaryDTO> result = orderService.findByCustomerId(customerId);

            assertThat(result).isEmpty();
        }
    }

    // ── findAll ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Deve retornar página de pedidos")
        void deveRetornarPaginaDePedidos() {
            var pageable = PageRequest.of(0, 20);
            when(orderRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(draftOrder), pageable, 1));
            when(mapper.toSummaryDTO(draftOrder)).thenReturn(SalesOrderSummaryDTO.builder()
                    .id(orderId).status("DRAFT").build());

            var result = orderService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findAll(pageable);
        }
    }
}

package br.com.rentafit.sales.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.enums.SalesItemStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.CancelSalesOrderDTO;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.port.RetailProductPort;
import br.com.rentafit.sales.port.RetailProductPort.RetailProductSnapshot;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesWorkflowService - Testes Unitários")
class SalesWorkflowServiceTest {

    @Mock private SalesOrderRepository orderRepository;
    @Mock private SalesOrderService orderService;
    @Mock private RetailProductPort productPort;
    @Mock private SalesMapper mapper;

    @InjectMocks
    private SalesWorkflowService workflowService;

    private UUID orderId;
    private UUID productId;
    private UUID itemId;
    private UUID userId;
    private SalesOrder draftOrder;
    private SalesOrder confirmedOrder;
    private SalesOrderItem reservedItem;
    private RetailProductSnapshot productSnapshot;
    private SalesOrderDetailsDTO detailsDTO;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        productId  = UUID.randomUUID();
        itemId     = UUID.randomUUID();
        userId     = UUID.randomUUID();

        productSnapshot = new RetailProductSnapshot(
                productId, "SKU-001", "Camiseta", "Vestuário",
                "M", "Azul", "Nike", new BigDecimal("89.90"),
                null, 30, 5);

        SalesOrderItem pendingItem = SalesOrderItem.builder()
                .id(itemId).retailProductId(productId)
                .sku("SKU-001").description("Camiseta — M Azul")
                .unitPrice(new BigDecimal("89.90")).quantity(2)
                .discountValue(BigDecimal.ZERO)
                .itemStatus(SalesItemStatus.PENDING)
                .build();

        reservedItem = SalesOrderItem.builder()
                .id(itemId).retailProductId(productId)
                .sku("SKU-001").description("Camiseta — M Azul")
                .unitPrice(new BigDecimal("89.90")).quantity(2)
                .discountValue(BigDecimal.ZERO)
                .itemStatus(SalesItemStatus.RESERVED)
                .build();

        draftOrder = SalesOrder.builder()
                .id(orderId).status(SalesOrderStatus.DRAFT)
                .discountValue(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(pendingItem)))
                .payments(new ArrayList<>())
                .build();

        confirmedOrder = SalesOrder.builder()
                .id(orderId).status(SalesOrderStatus.CONFIRMED)
                .discountValue(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(reservedItem)))
                .payments(new ArrayList<>())
                .build();

        detailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId).status("CONFIRMED").statusDescription("Confirmado")
                .subtotal(new BigDecimal("179.80")).totalValue(new BigDecimal("179.80"))
                .paidValue(BigDecimal.ZERO).remainingValue(new BigDecimal("179.80"))
                .discountValue(BigDecimal.ZERO).items(List.of()).payments(List.of())
                .build();

        // Simula usuário autenticado no SecurityContext.
        // Usa setContext(new SecurityContextImpl(auth)) em vez de getContext().setAuthentication()
        // para garantir que um mock de SecurityContext deixado por outro teste não sobreviva.
        UserAccount userAccount = new UserAccount();
        userAccount.setId(userId);
        userAccount.setUsername("test@test.com");
        var auth = new UsernamePasswordAuthenticationToken(userAccount, null, List.of());
        SecurityContext context = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── confirm ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("Deve confirmar pedido DRAFT com estoque suficiente")
        void deveConfirmarComEstoqueSuficiente() {
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(orderRepository.save(draftOrder)).thenReturn(draftOrder);
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);
            when(mapper.computeSubtotal(any())).thenReturn(new BigDecimal("179.80"));
            when(mapper.computePaidValue(any())).thenReturn(BigDecimal.ZERO);

            SalesOrderDetailsDTO result = workflowService.confirm(orderId);

            assertThat(result).isNotNull();
            assertThat(draftOrder.getStatus()).isEqualTo(SalesOrderStatus.CONFIRMED);
            verify(productPort).reserveStock(productId, 2, userId);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pedido não está em DRAFT")
        void deveLancarQuandoNaoDraft() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> workflowService.confirm(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pedido não tem itens")
        void deveLancarQuandoSemItens() {
            draftOrder.setItems(new ArrayList<>());
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);

            assertThatThrownBy(() -> workflowService.confirm(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("sem itens");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando estoque insuficiente")
        void deveLancarQuandoEstoqueInsuficiente() {
            RetailProductSnapshot semEstoque = new RetailProductSnapshot(
                    productId, "SKU-001", "Camiseta", "Vestuário",
                    "M", "Azul", "Nike", new BigDecimal("89.90"),
                    null, 30, 1); // quantityAvailable=1, mas pedido quer 2

            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);
            when(productPort.findById(productId)).thenReturn(Optional.of(semEstoque));

            assertThatThrownBy(() -> workflowService.confirm(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Estoque insuficiente")
                    .hasMessageContaining("SKU-001");
        }

        @Test
        @DisplayName("Deve transitar CONFIRMED → PAID quando pagamentos já cobrem o total")
        void deveTransitarParaPaidAutomaticamente() {
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);
            when(productPort.findById(productId)).thenReturn(Optional.of(productSnapshot));
            when(orderRepository.save(draftOrder)).thenReturn(draftOrder);
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);
            // Simula pagamentos que cobrem o total
            when(mapper.computeSubtotal(any())).thenReturn(new BigDecimal("179.80"));
            when(mapper.computePaidValue(any())).thenReturn(new BigDecimal("179.80"));

            workflowService.confirm(orderId);

            assertThat(draftOrder.getStatus()).isEqualTo(SalesOrderStatus.PAID);
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("Deve cancelar pedido em DRAFT sem liberar estoque")
        void deveCancelarDraftSemLiberarEstoque() {
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);
            when(orderRepository.save(draftOrder)).thenReturn(draftOrder);
            when(mapper.toDetailsDTO(draftOrder, null)).thenReturn(detailsDTO);

            workflowService.cancel(orderId, new CancelSalesOrderDTO("Desistência do cliente"));

            assertThat(draftOrder.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
            verify(productPort, never()).releaseStock(any(), anyInt(), any());
        }

        @Test
        @DisplayName("Deve cancelar pedido CONFIRMED e liberar estoque dos itens RESERVED")
        void deveCancelarConfirmedELiberarEstoque() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            workflowService.cancel(orderId, new CancelSalesOrderDTO("Fora de estoque"));

            assertThat(confirmedOrder.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
            verify(productPort).releaseStock(productId, 2, userId);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pedido está PAID")
        void deveLancarQuandoPago() {
            SalesOrder paidOrder = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.PAID)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();

            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> workflowService.cancel(orderId, new CancelSalesOrderDTO("Motivo")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT ou CONFIRMED");
        }
    }

    // ── markItemReady ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markItemReady")
    class MarkItemReady {

        @Test
        @DisplayName("Deve marcar item como READY quando pedido CONFIRMED e item RESERVED")
        void deveMarcarItemComoReady() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            workflowService.markItemReady(orderId, itemId);

            assertThat(reservedItem.getItemStatus()).isEqualTo(SalesItemStatus.READY);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando item não está RESERVED")
        void deveLancarQuandoItemNaoReserved() {
            reservedItem.setItemStatus(SalesItemStatus.PENDING);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> workflowService.markItemReady(orderId, itemId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("RESERVED");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pedido está em DRAFT")
        void deveLancarQuandoPedidoDraft() {
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);

            assertThatThrownBy(() -> workflowService.markItemReady(orderId, itemId))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // ── deliverItem ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deliverItem")
    class DeliverItem {

        @Test
        @DisplayName("Deve entregar item e auto-completar pedido quando todos entregues")
        void deveEntregarItemEAutoCompletar() {
            reservedItem.setItemStatus(SalesItemStatus.READY);
            SalesOrder paidOrder = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.PAID)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>(List.of(reservedItem)))
                    .payments(new ArrayList<>())
                    .build();

            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);
            when(orderRepository.save(paidOrder)).thenReturn(paidOrder);
            when(mapper.toDetailsDTO(paidOrder, null)).thenReturn(detailsDTO);

            workflowService.deliverItem(orderId, itemId, employeeId());

            assertThat(reservedItem.getItemStatus()).isEqualTo(SalesItemStatus.DELIVERED);
            assertThat(paidOrder.getStatus()).isEqualTo(SalesOrderStatus.COMPLETED);
            verify(productPort).removeStock(productId, 2, userId);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando item não está READY")
        void deveLancarQuandoItemNaoReady() {
            reservedItem.setItemStatus(SalesItemStatus.RESERVED);
            SalesOrder paidOrder = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.PAID)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>(List.of(reservedItem)))
                    .payments(new ArrayList<>())
                    .build();

            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> workflowService.deliverItem(orderId, itemId, employeeId()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("READY");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pedido não está ao menos PAID")
        void deveLancarQuandoPedidoNaoPaid() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> workflowService.deliverItem(orderId, itemId, employeeId()))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando itemId não pertence ao pedido")
        void deveLancarQuandoItemNaoEncontrado() {
            SalesOrder paidOrder = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.PAID)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>(List.of(reservedItem)))
                    .payments(new ArrayList<>())
                    .build();

            UUID itemInexistente = UUID.randomUUID();
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> workflowService.deliverItem(orderId, itemInexistente, employeeId()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(itemInexistente.toString());
        }
    }

    private UUID employeeId() {
        return UUID.randomUUID();
    }
}

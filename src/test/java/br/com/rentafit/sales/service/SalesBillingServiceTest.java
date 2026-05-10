package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.config.SalesBillingProperties;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.enums.InvoiceStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesBillingService - Testes Unitários")
class SalesBillingServiceTest {

    @Mock private SalesBillingProperties billingProperties;
    @Mock private SalesOrderRepository orderRepository;
    @Mock private SalesOrderService orderService;
    @Mock private SalesMapper mapper;

    @InjectMocks
    private SalesBillingService billingService;

    private UUID orderId;
    private SalesOrder paidOrder;
    private SalesOrder cancelledOrder;
    private SalesOrderDetailsDTO detailsDTO;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        paidOrder = SalesOrder.builder()
                .id(orderId).legacyId("V-20260510-1")
                .status(SalesOrderStatus.PAID)
                .invoiceStatus(InvoiceStatus.NONE)
                .discountValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of())
                .build();

        cancelledOrder = SalesOrder.builder()
                .id(orderId).legacyId("V-20260510-2")
                .status(SalesOrderStatus.CANCELLED)
                .invoiceStatus(InvoiceStatus.NONE)
                .discountValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of())
                .build();

        detailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId).legacyId("V-20260510-1")
                .status("PAID").statusDescription("Pago")
                .invoiceStatus("EMITTED")
                .customerId(UUID.randomUUID()).customerName("João")
                .subtotal(BigDecimal.ZERO).totalValue(BigDecimal.ZERO)
                .paidValue(BigDecimal.ZERO).remainingValue(BigDecimal.ZERO)
                .discountValue(BigDecimal.ZERO).items(List.of()).payments(List.of())
                .build();
    }

    @Nested
    @DisplayName("emitInvoice")
    class EmitInvoice {

        @Test
        @DisplayName("deve emitir NFS-e com sucesso para pedido PAID")
        void deveEmitirParaPedidoPago() {
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);
            when(orderRepository.save(paidOrder)).thenReturn(paidOrder);
            when(mapper.toDetailsDTO(eq(paidOrder), any())).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = billingService.emitInvoice(orderId);

            assertThat(result).isNotNull();
            assertThat(paidOrder.getInvoiceStatus()).isEqualTo(InvoiceStatus.EMITTED);
            assertThat(paidOrder.getInvoiceId()).startsWith("NFSE-PLACEHOLDER-");
            verify(orderRepository).save(paidOrder);
        }

        @Test
        @DisplayName("deve emitir NFS-e com sucesso para pedido COMPLETED")
        void deveEmitirParaPedidoConcluido() {
            paidOrder.setStatus(SalesOrderStatus.COMPLETED);
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);
            when(orderRepository.save(paidOrder)).thenReturn(paidOrder);
            when(mapper.toDetailsDTO(eq(paidOrder), any())).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = billingService.emitInvoice(orderId);

            assertThat(result).isNotNull();
            assertThat(paidOrder.getInvoiceStatus()).isEqualTo(InvoiceStatus.EMITTED);
        }

        @Test
        @DisplayName("deve lançar ValidationException para pedido DRAFT")
        void deveRejeitarParaDraft() {
            paidOrder.setStatus(SalesOrderStatus.DRAFT);
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> billingService.emitInvoice(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("deve lançar ValidationException para pedido CONFIRMED")
        void deveRejeitarParaConfirmed() {
            paidOrder.setStatus(SalesOrderStatus.CONFIRMED);
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> billingService.emitInvoice(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("BUG-2026-05-10-5 REGRESSION: deve lançar ValidationException para pedido CANCELLED")
        void bug5_deveRejeitarParaCancelled() {
            // BUG-5: CANCELLED.ordinal()=4 >= PAID.ordinal()=2, então a guarda
            // `order.getStatus().ordinal() < PAID.ordinal()` não dispara para CANCELLED.
            // Após a correção, deve lançar ValidationException.
            when(orderService.findEntityById(orderId)).thenReturn(cancelledOrder);

            assertThatThrownBy(() -> billingService.emitInvoice(orderId))
                    .as("BUG-5: pedido CANCELLED não deve permitir emissão de NFS-e")
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CANCELLED");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ValidationException se NFS-e já emitida")
        void deveRejeitarSeJaEmitida() {
            paidOrder.setInvoiceStatus(InvoiceStatus.EMITTED);
            paidOrder.setInvoiceId("NFSE-PLACEHOLDER-V-20260510-1");
            when(orderService.findEntityById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> billingService.emitInvoice(orderId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("já foi emitida");
        }
    }

    @Nested
    @DisplayName("onOrderPaid")
    class OnOrderPaid {

        @Test
        @DisplayName("deve marcar PENDING_EMISSION quando autoEmitOnPayment=false")
        void deveMarcarPendingQuandoAutoEmitDesabilitado() {
            when(billingProperties.isAutoEmitOnPayment()).thenReturn(false);

            billingService.onOrderPaid(paidOrder);

            assertThat(paidOrder.getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING_EMISSION);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve emitir NFS-e automaticamente quando autoEmitOnPayment=true")
        void deveEmitirAutomaticamenteQuandoAutoEmitHabilitado() {
            when(billingProperties.isAutoEmitOnPayment()).thenReturn(true);

            billingService.onOrderPaid(paidOrder);

            assertThat(paidOrder.getInvoiceStatus()).isEqualTo(InvoiceStatus.EMITTED);
            assertThat(paidOrder.getInvoiceId()).startsWith("NFSE-PLACEHOLDER-");
        }
    }
}

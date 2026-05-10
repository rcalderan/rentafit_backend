package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentInputDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import br.com.rentafit.sales.repository.SalesPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesPaymentService - Testes Unitários")
class SalesPaymentServiceTest {

    @Mock private SalesPaymentRepository paymentRepository;
    @Mock private SalesOrderRepository orderRepository;
    @Mock private SalesOrderService orderService;
    @Mock private SalesBillingService billingService;
    @Mock private SalesMapper mapper;

    @InjectMocks
    private SalesPaymentService paymentService;

    private UUID orderId;
    private UUID paymentId;
    private UUID employeeId;
    private SalesOrder confirmedOrder;
    private SalesOrder draftOrder;
    private SalesPayment pendingPayment;
    private SalesPaymentInputDTO validPaymentDTO;
    private SalesOrderDetailsDTO detailsDTO;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        paymentId  = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        SalesOrderItem item = SalesOrderItem.builder()
                .id(UUID.randomUUID()).retailProductId(UUID.randomUUID())
                .sku("SKU-001").description("Camiseta — M Azul")
                .unitPrice(new BigDecimal("100.00")).quantity(1)
                .discountValue(BigDecimal.ZERO)
                .build();

        pendingPayment = SalesPayment.builder()
                .id(paymentId).installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(3))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("100.00"))
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        confirmedOrder = SalesOrder.builder()
                .id(orderId).status(SalesOrderStatus.CONFIRMED)
                .discountValue(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(item)))
                .payments(new ArrayList<>(List.of(pendingPayment)))
                .build();

        draftOrder = SalesOrder.builder()
                .id(orderId).status(SalesOrderStatus.DRAFT)
                .discountValue(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(item)))
                .payments(new ArrayList<>())
                .build();

        validPaymentDTO = new SalesPaymentInputDTO(
                1, LocalDate.now().plusDays(3), "PIX",
                new BigDecimal("100.00"), 1, employeeId, "PENDING");

        detailsDTO = SalesOrderDetailsDTO.builder()
                .id(orderId).status("CONFIRMED").statusDescription("Confirmado")
                .subtotal(new BigDecimal("100.00")).totalValue(new BigDecimal("100.00"))
                .paidValue(BigDecimal.ZERO).remainingValue(new BigDecimal("100.00"))
                .discountValue(BigDecimal.ZERO).items(List.of()).payments(List.of())
                .build();
    }

    // ── listPayments ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listPayments")
    class ListPayments {

        @Test
        @DisplayName("Deve retornar lista de parcelas do pedido")
        void deveRetornarListaDeParcelas() {
            SalesPaymentDetailsDTO paymentDetails = new SalesPaymentDetailsDTO(
                    paymentId, 1, LocalDate.now(), "PIX", "Pix",
                    new BigDecimal("100.00"), 1, null, "PENDING", "Pendente");

            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(paymentRepository.findBySalesOrderIdOrderByInstallmentNumber(orderId))
                    .thenReturn(List.of(pendingPayment));
            when(mapper.toPaymentDetailsDTO(pendingPayment)).thenReturn(paymentDetails);

            List<SalesPaymentDetailsDTO> result = paymentService.listPayments(orderId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).installmentNumber()).isEqualTo(1);
        }
    }

    // ── addPayment ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addPayment")
    class AddPayment {

        @Test
        @DisplayName("Deve adicionar pagamento ao pedido CONFIRMED")
        void deveAdicionarPagamentoConfirmed() {
            confirmedOrder.setPayments(new ArrayList<>());
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(mapper.toPaymentEntity(validPaymentDTO, confirmedOrder)).thenReturn(pendingPayment);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.computeSubtotal(any())).thenReturn(new BigDecimal("100.00"));
            when(mapper.computePaidValue(any())).thenReturn(BigDecimal.ZERO);
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = paymentService.addPayment(orderId, validPaymentDTO);

            assertThat(result).isNotNull();
            verify(orderRepository).save(confirmedOrder);
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao adicionar pagamento em pedido DRAFT")
        void deveLancarQuandoDraft() {
            when(orderService.findEntityById(orderId)).thenReturn(draftOrder);

            assertThatThrownBy(() -> paymentService.addPayment(orderId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao adicionar pagamento em pedido CANCELLED")
        void deveLancarQuandoCancelled() {
            SalesOrder cancelledOrder = SalesOrder.builder()
                    .id(orderId).status(SalesOrderStatus.CANCELLED)
                    .discountValue(BigDecimal.ZERO)
                    .items(new ArrayList<>()).payments(new ArrayList<>())
                    .build();
            when(orderService.findEntityById(orderId)).thenReturn(cancelledOrder);

            assertThatThrownBy(() -> paymentService.addPayment(orderId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("Deve transitar pedido para PAID automaticamente quando pagamento cobre total")
        void deveTransitarParaPaidAuto() {
            SalesPaymentInputDTO paidDTO = new SalesPaymentInputDTO(
                    1, LocalDate.now(), "PIX", new BigDecimal("100.00"), 1, employeeId, "PAID");

            SalesPayment paidPayment = SalesPayment.builder()
                    .id(paymentId).installmentNumber(1)
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.PIX)
                    .value(new BigDecimal("100.00"))
                    .installments(1).status(PaymentStatus.PAID)
                    .build();

            confirmedOrder.setPayments(new ArrayList<>());
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(mapper.toPaymentEntity(paidDTO, confirmedOrder)).thenReturn(paidPayment);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.computeSubtotal(any())).thenReturn(new BigDecimal("100.00"));
            when(mapper.computePaidValue(any())).thenReturn(new BigDecimal("100.00"));
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            paymentService.addPayment(orderId, paidDTO);

            assertThat(confirmedOrder.getStatus()).isEqualTo(SalesOrderStatus.PAID);
            verify(billingService).onOrderPaid(confirmedOrder);
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando atingido limite de 24 parcelas")
        void deveLancarQuandoAtingiuLimite() {
            List<SalesPayment> maxPagamentos = new ArrayList<>();
            for (int i = 1; i <= 24; i++) {
                maxPagamentos.add(SalesPayment.builder()
                        .id(UUID.randomUUID()).installmentNumber(i)
                        .paymentDate(LocalDate.now()).paymentMethod(PaymentMethod.PIX)
                        .value(BigDecimal.TEN).installments(1).status(PaymentStatus.PENDING)
                        .build());
            }
            confirmedOrder.setPayments(maxPagamentos);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.addPayment(orderId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("24");
        }
    }

    // ── updatePayment ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updatePayment")
    class UpdatePayment {

        @Test
        @DisplayName("Deve atualizar parcela PENDING em pedido CONFIRMED")
        void deveAtualizarParcelaPending() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.computeSubtotal(any())).thenReturn(new BigDecimal("100.00"));
            when(mapper.computePaidValue(any())).thenReturn(BigDecimal.ZERO);
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = paymentService.updatePayment(orderId, paymentId, validPaymentDTO);

            assertThat(result).isNotNull();
            verify(orderRepository).save(confirmedOrder);
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao tentar alterar parcela PAID")
        void deveLancarQuandoPaid() {
            pendingPayment.setStatus(PaymentStatus.PAID);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.updatePayment(orderId, paymentId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao alterar parcela em pedido PAID")
        void deveLancarQuandoPedidoPaid() {
            confirmedOrder.setStatus(SalesOrderStatus.PAID);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.updatePayment(orderId, paymentId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("PAID");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando paymentId não pertence ao pedido")
        void deveLancarQuandoPaymentNaoEncontrado() {
            UUID paymentInexistente = UUID.randomUUID();
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.updatePayment(orderId, paymentInexistente, validPaymentDTO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── cancelPayment ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("Deve cancelar parcela PENDING em pedido CONFIRMED")
        void deveCancelarParcelaPending() {
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);
            when(orderRepository.save(confirmedOrder)).thenReturn(confirmedOrder);
            when(mapper.toDetailsDTO(confirmedOrder, null)).thenReturn(detailsDTO);

            SalesOrderDetailsDTO result = paymentService.cancelPayment(orderId, paymentId);

            assertThat(result).isNotNull();
            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao tentar cancelar parcela PAID")
        void deveLancarQuandoPaid() {
            pendingPayment.setStatus(PaymentStatus.PAID);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.cancelPayment(orderId, paymentId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("Deve lançar ValidationException ao cancelar em pedido COMPLETED")
        void deveLancarQuandoCompleted() {
            confirmedOrder.setStatus(SalesOrderStatus.COMPLETED);
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.cancelPayment(orderId, paymentId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("pagamento completo");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando paymentId não pertence ao pedido")
        void deveLancarQuandoPaymentNaoEncontrado() {
            UUID paymentInexistente = UUID.randomUUID();
            when(orderService.findEntityById(orderId)).thenReturn(confirmedOrder);

            assertThatThrownBy(() -> paymentService.cancelPayment(orderId, paymentInexistente))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

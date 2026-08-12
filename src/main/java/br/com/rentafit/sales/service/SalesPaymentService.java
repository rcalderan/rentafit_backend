package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentInputDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import br.com.rentafit.sales.repository.SalesPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gerencia pagamentos (parcelas) de um pedido de venda.
 *
 * <p>Regras:
 * <ul>
 *   <li>Adicionar: permitido em CONFIRMED, PAID</li>
 *   <li>Atualizar/Cancelar: somente PENDING antes de PAID</li>
 *   <li>Auto-transition: quando soma PAID >= total → pedido muda para PAID</li>
 *   <li>Máximo 24 parcelas por pedido</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalesPaymentService {

    private static final int MAX_INSTALLMENTS = 24;

    private final SalesPaymentRepository paymentRepository;
    private final SalesOrderRepository orderRepository;
    private final SalesOrderService orderService;
    private final SalesMapper mapper;

    @Transactional(readOnly = true)
    public List<SalesPaymentDetailsDTO> listPayments(UUID orderId) {
        orderService.findEntityById(orderId);
        return paymentRepository.findBySalesOrderIdOrderByInstallmentNumber(orderId).stream()
                .map(mapper::toPaymentDetailsDTO)
                .collect(Collectors.toList());
    }

    public SalesOrderDetailsDTO addPayment(UUID orderId, SalesPaymentInputDTO dto) {
        SalesOrder order = orderService.findEntityById(orderId);

        if (order.getStatus() == SalesOrderStatus.DRAFT
            || order.getStatus() == SalesOrderStatus.CANCELLED
            || order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new ValidationException(
                    "Pagamento não pode ser adicionado no status: " + order.getStatus());
        }

        if (order.getPayments().size() >= MAX_INSTALLMENTS) {
            throw new ValidationException(
                    "Máximo de " + MAX_INSTALLMENTS + " parcelas por pedido atingido");
        }

        BigDecimal subtotal = mapper.computeSubtotal(order.getItems());
        BigDecimal totalValue = subtotal.subtract(order.getDiscountValue()).max(BigDecimal.ZERO);
        BigDecimal alreadyScheduled = order.getPayments().stream()
                .map(SalesPayment::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyScheduled.add(dto.value()).compareTo(totalValue) > 0) {
            throw new ValidationException(
                    "Soma das parcelas (" + alreadyScheduled.add(dto.value()) +
                    ") excede o total a pagar (" + totalValue + ")");
        }

        SalesPayment payment = mapper.toPaymentEntity(dto, order);
        order.getPayments().add(payment);

        checkAndTransitionToPaid(order);

        SalesOrder saved = orderRepository.save(order);
        log.info("Payment added to sales order {}: installment {}", orderId, dto.installmentNumber());
        return mapper.toDetailsDTO(saved, null);
    }

    public SalesOrderDetailsDTO updatePayment(UUID orderId, UUID paymentId, SalesPaymentInputDTO dto) {
        SalesOrder order = orderService.findEntityById(orderId);
        SalesPayment payment = findPayment(order, paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException(
                    "Somente parcelas PENDING podem ser alteradas, status: " + payment.getStatus());
        }

        if (order.getStatus() == SalesOrderStatus.PAID
            || order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new ValidationException(
                    "Parcelas não podem ser alteradas após pagamento completo, status: " + order.getStatus());
        }

        // Atualizar campos
        payment.setInstallmentNumber(dto.installmentNumber());
        payment.setPaymentDate(dto.paymentDate());
        payment.setPaymentMethod(
                br.com.rentafit.rental.domain.enums.PaymentMethod.valueOf(dto.paymentMethod().toUpperCase()));
        payment.setValue(dto.value());
        payment.setInstallments(dto.installments() != null ? dto.installments() : 1);
        payment.setProcessedByEmployeeId(dto.processedByEmployeeId());
        if (dto.status() != null) {
            payment.setStatus(PaymentStatus.valueOf(dto.status().toUpperCase()));
        }

        checkAndTransitionToPaid(order);

        SalesOrder saved = orderRepository.save(order);
        log.info("Payment {} updated in sales order {}", paymentId, orderId);
        return mapper.toDetailsDTO(saved, null);
    }

    public SalesOrderDetailsDTO cancelPayment(UUID orderId, UUID paymentId) {
        SalesOrder order = orderService.findEntityById(orderId);
        SalesPayment payment = findPayment(order, paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException(
                    "Somente parcelas PENDING podem ser canceladas, status: " + payment.getStatus());
        }

        if (order.getStatus() == SalesOrderStatus.PAID
            || order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new ValidationException(
                    "Parcelas não podem ser canceladas após pagamento completo");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        SalesOrder saved = orderRepository.save(order);
        log.info("Payment {} cancelled in sales order {}", paymentId, orderId);
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Auto-transition ──────────────────────────────────────────────────────

    /**
     * Verifica se soma dos pagamentos PAID >= total e transita para PAID.
     * A emissão de NFS-e foi delegada ao microsserviço externo costume-rental-nfe;
     * o fluxo de pagamento apenas transita para PAID sem marcar PENDING_EMISSION.
     */
    private void checkAndTransitionToPaid(SalesOrder order) {
        if (order.getStatus() != SalesOrderStatus.CONFIRMED) return;

        BigDecimal subtotal = mapper.computeSubtotal(order.getItems());
        BigDecimal totalValue = subtotal.subtract(order.getDiscountValue()).max(BigDecimal.ZERO);
        BigDecimal paidValue = mapper.computePaidValue(order.getPayments());

        if (paidValue.compareTo(totalValue) >= 0) {
            order.setStatus(SalesOrderStatus.PAID);
            log.info("Sales order {} auto-transitioned to PAID (paid={}, total={})",
                    order.getId(), paidValue, totalValue);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SalesPayment findPayment(SalesOrder order, UUID paymentId) {
        return order.getPayments().stream()
                .filter(p -> p.getId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SalesPayment", "id", paymentId.toString()));
    }
}

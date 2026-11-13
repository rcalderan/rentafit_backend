package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.*;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.port.RetailProductPort;
import br.com.rentafit.sales.port.RetailProductPort.RetailProductSnapshot;
import br.com.rentafit.sales.port.SalesCustomerPort;
import br.com.rentafit.sales.port.SalesCustomerPort.CustomerSnapshot;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.service.TerminalSectorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CRUD de pedidos de venda. Transições de estado ficam no SalesWorkflowService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository orderRepository;
    private final SalesCustomerPort customerPort;
    private final RetailProductPort productPort;
    private final SalesMapper mapper;
    private final TerminalSectorResolver sectorResolver;

    @Value("${rentafit.legacy-id.pattern:yyMMdd}")
    private String legacyIdPattern;

    public SalesOrderDetailsDTO create(CreateSalesOrderDTO dto) {
        Sector sector = sectorResolver.requireTerminalSector();
        CustomerSnapshot customer = resolveCustomer(dto.customerId());

        SalesOrder order = mapper.toEntity(dto, customer);
        order.setLegacyId(generateLegacyId());
        order.setSectorId(sector.getId());

        // Adicionar itens resolvendo produto via port
        if (dto.items() != null) {
            List<SalesOrderItem> items = dto.items().stream()
                    .map(itemDto -> {
                        RetailProductSnapshot product = productPort.findById(itemDto.retailProductId())
                                .orElseThrow(() -> new ValidationException(
                                        "Produto retail não encontrado: " + itemDto.retailProductId()));
                        validateItemDiscount(itemDto, product);
                        return mapper.toItemEntity(itemDto, order, product);
                    })
                    .collect(Collectors.toList());
            order.setItems(items);
        }
        validateOrderDiscount(dto.discountValue(), order.getItems());

        // Adicionar pagamentos
        if (dto.payments() != null) {
            validateInputPaymentStatuses(dto.payments(), false);
            order.setPayments(dto.payments().stream()
                    .map(p -> mapper.toPaymentEntity(p, order))
                    .collect(Collectors.toList()));
            // Parcela que já nasce quitada (baixa no ato) registra o setor do terminal recebedor
            order.getPayments().forEach(p -> {
                if (p.getStatus().isCashMovement()) {
                    p.setSectorId(sector.getId());
                }
            });
        }

        SalesOrder saved = orderRepository.save(order);
        log.info("Sales order created: {} ({})", saved.getId(), saved.getLegacyId());
        return mapper.toDetailsDTO(saved, null);
    }

    public SalesOrderDetailsDTO update(UUID orderId, UpdateSalesOrderDTO dto) {
        Sector sector = sectorResolver.requireTerminalSector();
        SalesOrder order = findEntityById(orderId);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new ValidationException(
                    "Pedido só pode ser editado no status DRAFT, status atual: " + order.getStatus());
        }

        validateInputPaymentStatuses(dto.payments(), true);
        validateExistingPaidPaymentsPreserved(dto.payments(), order.getPayments());

        // Parcelas já quitadas preservam o setor em que foram recebidas (recriadas por installmentNumber)
        Map<Integer, UUID> settledSectorByInstallment = new HashMap<>();
        order.getPayments().stream()
                .filter(p -> p.getStatus().isCashMovement() && p.getInstallmentNumber() != null)
                .forEach(p -> settledSectorByInstallment.put(p.getInstallmentNumber(), p.getSectorId()));

        CustomerSnapshot customer = resolveCustomer(dto.customerId());
        mapper.updateEntityFromDTO(order, dto, customer);

        // Replace items
        order.getItems().clear();
        if (dto.items() != null) {
            dto.items().stream()
                    .map(itemDto -> {
                        RetailProductSnapshot product = productPort.findById(itemDto.retailProductId())
                                .orElseThrow(() -> new ValidationException(
                                        "Produto retail não encontrado: " + itemDto.retailProductId()));
                        validateItemDiscount(itemDto, product);
                        return mapper.toItemEntity(itemDto, order, product);
                    })
                    .forEach(order.getItems()::add);
        }
        validateOrderDiscount(dto.discountValue(), order.getItems());

        // Replace payments
        order.getPayments().clear();
        if (dto.payments() != null) {
            dto.payments().stream()
                    .map(p -> mapper.toPaymentEntity(p, order))
                    .forEach(order.getPayments()::add);
            order.getPayments().forEach(p -> {
                if (!p.getStatus().isCashMovement()) {
                    return;
                }
                UUID priorSector = settledSectorByInstallment.get(p.getInstallmentNumber());
                p.setSectorId(priorSector != null ? priorSector : sector.getId());
            });
        }

        SalesOrder saved = orderRepository.save(order);
        log.info("Sales order updated: {}", saved.getId());
        return mapper.toDetailsDTO(saved, null);
    }

    @Transactional(readOnly = true)
    public SalesOrderDetailsDTO findById(UUID orderId) {
        SalesOrder order = findEntityById(orderId);
        return mapper.toDetailsDTO(order, null);
    }

    @Transactional(readOnly = true)
    public SalesOrderDetailsDTO findByLegacyId(String legacyId) {
        SalesOrder order = orderRepository.findByLegacyId(legacyId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "legacyId", legacyId));
        return mapper.toDetailsDTO(order, null);
    }

    @Transactional(readOnly = true)
    public List<SalesOrderSummaryDTO> findByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(mapper::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryDTO> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(mapper::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryDTO> findWithFilters(SalesOrderStatus status,
                                                       OffsetDateTime dateFrom,
                                                       OffsetDateTime dateTo,
                                                       UUID sectorId,
                                                       Pageable pageable) {
        if (status != null && dateFrom == null && dateTo == null && sectorId == null) {
            return orderRepository.findByStatus(status, pageable).map(mapper::toSummaryDTO);
        }
        return orderRepository.findWithFilters(status, dateFrom, dateTo, sectorId, pageable)
                .map(mapper::toSummaryDTO);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public SalesOrder findEntityById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.forId("SalesOrder", orderId));
    }

    /** Resolve customer snapshot. Null customerId = venda balcão (retorna null). */
    private CustomerSnapshot resolveCustomer(UUID customerId) {
        if (customerId == null) return null;
        return customerPort.findById(customerId)
                .orElseThrow(() -> new ValidationException("Cliente não encontrado: " + customerId));
    }

    private void validateItemDiscount(SalesOrderItemInputDTO itemDto, RetailProductSnapshot product) {
        BigDecimal itemDiscount = itemDto.discountValue() != null ? itemDto.discountValue() : BigDecimal.ZERO;
        if (itemDiscount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal grossLine = product.value().multiply(BigDecimal.valueOf(itemDto.quantity()));
        if (itemDiscount.compareTo(grossLine) >= 0) {
            throw new ValidationException(
                    "Desconto do item (" + itemDiscount + ") deve ser menor que o valor bruto do item (" + grossLine + ")");
        }
    }

    private void validateOrderDiscount(BigDecimal discountValue, List<SalesOrderItem> items) {
        BigDecimal orderDiscount = discountValue != null ? discountValue : BigDecimal.ZERO;
        if (orderDiscount.compareTo(BigDecimal.ZERO) <= 0 || items == null || items.isEmpty()) return;
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())).subtract(i.getDiscountValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (orderDiscount.compareTo(subtotal) >= 0) {
            throw new ValidationException(
                    "Desconto do pedido (" + orderDiscount + ") deve ser menor que o subtotal dos itens (" + subtotal + ")");
        }
    }

    private void validateInputPaymentStatuses(List<SalesPaymentInputDTO> payments, boolean allowCancelled) {
        if (payments == null) return;
        java.util.Set<Integer> seenInstallments = new java.util.HashSet<>();
        for (SalesPaymentInputDTO p : payments) {
            if (p.installmentNumber() != null && !seenInstallments.add(p.installmentNumber())) {
                throw new ValidationException(
                        "Parcela #" + p.installmentNumber() + ": número da parcela duplicado no pedido");
            }
            if (p.status() == null) continue;
            PaymentStatus status = parsePaymentStatus(p.status());
            boolean allowed = status == PaymentStatus.PENDING || status == PaymentStatus.PAID
                    || (allowCancelled && status == PaymentStatus.CANCELLED);
            if (!allowed) {
                throw new ValidationException(
                        "Parcela #" + p.installmentNumber() + ": status '" + p.status()
                        + "' não é permitido na entrada (esperado: PENDING ou PAID)");
            }
            if (status == PaymentStatus.PAID && p.processedByEmployeeId() == null) {
                throw new ValidationException(
                        "Parcela #" + p.installmentNumber()
                        + ": pagamentos com status PAGO devem informar o funcionário responsável (processedByEmployeeId)");
            }
        }
    }

    private void validateExistingPaidPaymentsPreserved(List<SalesPaymentInputDTO> incoming, List<SalesPayment> existing) {
        if (existing == null || existing.isEmpty()) return;
        List<SalesPayment> paidExisting = existing.stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .toList();
        if (paidExisting.isEmpty()) return;
        List<SalesPaymentInputDTO> incomingList = incoming != null ? incoming : List.of();
        for (SalesPayment paid : paidExisting) {
            boolean preserved = incomingList.stream().anyMatch(dto -> matchesPaidPayment(paid, dto));
            if (!preserved) {
                throw new ValidationException(
                        "Parcela PAGA #" + paid.getInstallmentNumber()
                        + " (R$ " + paid.getValue() + ") não pode ser removida ou alterada");
            }
        }
    }

    private boolean matchesPaidPayment(SalesPayment paid, SalesPaymentInputDTO dto) {
        if (dto.status() == null || !"PAID".equalsIgnoreCase(dto.status())) return false;
        int dtoInstallments = dto.installments() != null ? dto.installments() : 1;
        int paidInstallments = paid.getInstallments() != null ? paid.getInstallments() : 1;
        return Objects.equals(paid.getInstallmentNumber(), dto.installmentNumber())
                && Objects.equals(paid.getPaymentDate(), dto.paymentDate())
                && paid.getPaymentMethod() != null && dto.paymentMethod() != null
                && paid.getPaymentMethod().name().equalsIgnoreCase(dto.paymentMethod())
                && paid.getValue() != null && dto.value() != null
                && paid.getValue().compareTo(dto.value()) == 0
                && paidInstallments == dtoInstallments
                && Objects.equals(paid.getProcessedByEmployeeId(), dto.processedByEmployeeId());
    }

    private PaymentStatus parsePaymentStatus(String raw) {
        try {
            return PaymentStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Status de pagamento inválido: " + raw + " (esperado: PENDING ou PAID)");
        }
    }

    /** Gera legacyId no formato V-YYYYMMDD-N. */
    private String generateLegacyId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(legacyIdPattern);
        String prefix = "V-" + LocalDate.now().format(formatter) + "-";
        long count = orderRepository.countByLegacyIdPrefix(prefix);
        return prefix + (count + 1);
    }
}

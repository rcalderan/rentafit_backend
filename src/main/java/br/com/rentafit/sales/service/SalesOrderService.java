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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    private static final DateTimeFormatter LEGACY_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public SalesOrderDetailsDTO create(CreateSalesOrderDTO dto) {
        CustomerSnapshot customer = resolveCustomer(dto.customerId());

        SalesOrder order = mapper.toEntity(dto, customer);
        order.setLegacyId(generateLegacyId());

        // Adicionar itens resolvendo produto via port
        if (dto.items() != null) {
            List<SalesOrderItem> items = dto.items().stream()
                    .map(itemDto -> {
                        RetailProductSnapshot product = productPort.findById(itemDto.retailProductId())
                                .orElseThrow(() -> new ValidationException(
                                        "Produto retail não encontrado: " + itemDto.retailProductId()));
                        return mapper.toItemEntity(itemDto, order, product);
                    })
                    .collect(Collectors.toList());
            order.setItems(items);
        }

        // Adicionar pagamentos
        if (dto.payments() != null) {
            order.setPayments(dto.payments().stream()
                    .map(p -> mapper.toPaymentEntity(p, order))
                    .collect(Collectors.toList()));
        }

        SalesOrder saved = orderRepository.save(order);
        log.info("Sales order created: {} ({})", saved.getId(), saved.getLegacyId());
        return mapper.toDetailsDTO(saved, null);
    }

    public SalesOrderDetailsDTO update(UUID orderId, UpdateSalesOrderDTO dto) {
        SalesOrder order = findEntityById(orderId);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new ValidationException(
                    "Pedido só pode ser editado no status DRAFT, status atual: " + order.getStatus());
        }

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
                        return mapper.toItemEntity(itemDto, order, product);
                    })
                    .forEach(order.getItems()::add);
        }

        // Replace payments
        order.getPayments().clear();
        if (dto.payments() != null) {
            dto.payments().stream()
                    .map(p -> mapper.toPaymentEntity(p, order))
                    .forEach(order.getPayments()::add);
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
                                                       Pageable pageable) {
        return orderRepository.findWithFilters(status, dateFrom, dateTo, pageable)
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

    /** Gera legacyId no formato V-YYYYMMDD-N. */
    private String generateLegacyId() {
        String prefix = "V-" + LocalDate.now().format(LEGACY_DATE_FMT) + "-";
        long count = orderRepository.countByLegacyIdPrefix(prefix);
        return prefix + (count + 1);
    }
}

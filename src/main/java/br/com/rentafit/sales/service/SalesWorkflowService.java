package br.com.rentafit.sales.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.enums.InvoiceStatus;
import br.com.rentafit.sales.domain.enums.SalesItemStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.CancelSalesOrderDTO;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.port.RetailProductPort;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transições de estado do pedido de venda:
 * DRAFT → CONFIRMED → (PAID via payment) → COMPLETED
 * DRAFT|CONFIRMED → CANCELLED
 *
 * <p>Cada transição executa as operações de estoque correspondentes.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalesWorkflowService {

    private final SalesOrderRepository orderRepository;
    private final SalesOrderService orderService;
    private final RetailProductPort productPort;
    private final SalesMapper mapper;

    /**
     * DRAFT → CONFIRMED: valida estoque e reserva.
     */
    public SalesOrderDetailsDTO confirm(UUID orderId) {
        SalesOrder order = orderService.findEntityById(orderId);
        assertStatus(order, SalesOrderStatus.DRAFT, "confirmar");

        if (order.getItems().isEmpty()) {
            throw new ValidationException("Pedido sem itens não pode ser confirmado");
        }

        UUID userId = getCurrentUserId();

        // Validar e reservar estoque para cada item
        for (SalesOrderItem item : order.getItems()) {
            var product = productPort.findById(item.getRetailProductId())
                    .orElseThrow(() -> new ValidationException(
                            "Produto não encontrado: " + item.getRetailProductId()));

            if (product.quantityAvailable() < item.getQuantity()) {
                throw new ValidationException(
                        "Estoque insuficiente para " + item.getSku()
                        + ": disponível=" + product.quantityAvailable()
                        + ", solicitado=" + item.getQuantity());
            }

            productPort.reserveStock(item.getRetailProductId(), item.getQuantity(), userId);
            item.setItemStatus(SalesItemStatus.RESERVED);
        }

        order.setStatus(SalesOrderStatus.CONFIRMED);
        SalesOrder saved = orderRepository.save(order);
        log.info("Sales order confirmed: {} by user {}", saved.getId(), userId);
        return mapper.toDetailsDTO(saved, null);
    }

    /**
     * DRAFT|CONFIRMED → CANCELLED: libera reservas se CONFIRMED.
     */
    public SalesOrderDetailsDTO cancel(UUID orderId, CancelSalesOrderDTO dto) {
        SalesOrder order = orderService.findEntityById(orderId);

        if (order.getStatus() != SalesOrderStatus.DRAFT
            && order.getStatus() != SalesOrderStatus.CONFIRMED) {
            throw new ValidationException(
                    "Somente pedidos DRAFT ou CONFIRMED podem ser cancelados, status atual: " + order.getStatus());
        }

        // Liberar reservas se estava CONFIRMED
        UUID userId = getCurrentUserId();
        if (order.getStatus() == SalesOrderStatus.CONFIRMED) {
            for (SalesOrderItem item : order.getItems()) {
                if (item.getItemStatus() == SalesItemStatus.RESERVED
                    || item.getItemStatus() == SalesItemStatus.READY) {
                    productPort.releaseStock(item.getRetailProductId(), item.getQuantity(), userId);
                }
                item.setItemStatus(SalesItemStatus.PENDING);
            }
        }

        order.setStatus(SalesOrderStatus.CANCELLED);
        order.setCancellationReason(dto.reason());
        SalesOrder saved = orderRepository.save(order);
        log.info("Sales order cancelled: {} — reason: {}", saved.getId(), dto.reason());
        return mapper.toDetailsDTO(saved, null);
    }

    /**
     * Marca item como READY (pronto para entrega).
     * Requer pedido CONFIRMED ou PAID.
     */
    public SalesOrderDetailsDTO markItemReady(UUID orderId, UUID itemId) {
        SalesOrder order = orderService.findEntityById(orderId);
        assertMinStatus(order, SalesOrderStatus.CONFIRMED, "marcar como pronto");

        SalesOrderItem item = findItem(order, itemId);
        if (item.getItemStatus() != SalesItemStatus.RESERVED) {
            throw new ValidationException(
                    "Item deve estar RESERVED para marcar como READY, status atual: " + item.getItemStatus());
        }

        item.setItemStatus(SalesItemStatus.READY);
        SalesOrder saved = orderRepository.save(order);
        log.info("Sales item {} marked as READY in order {}", itemId, orderId);
        return mapper.toDetailsDTO(saved, null);
    }

    /**
     * Confirma entrega do item: READY → DELIVERED.
     * Remove estoque definitivamente. Quando todos itens entregues → COMPLETED.
     */
    public SalesOrderDetailsDTO deliverItem(UUID orderId, UUID itemId, UUID employeeId) {
        SalesOrder order = orderService.findEntityById(orderId);
        assertMinStatus(order, SalesOrderStatus.PAID, "entregar item");

        SalesOrderItem item = findItem(order, itemId);
        if (item.getItemStatus() != SalesItemStatus.READY) {
            throw new ValidationException(
                    "Item deve estar READY para entrega, status atual: " + item.getItemStatus());
        }

        UUID userId = getCurrentUserId();

        // Saída definitiva do estoque: reserved--, total--
        productPort.removeStock(item.getRetailProductId(), item.getQuantity(), userId);
        item.setItemStatus(SalesItemStatus.DELIVERED);
        item.setDeliveredAt(OffsetDateTime.now());
        item.setDeliveredByEmployeeId(employeeId);

        // Auto-complete: se todos entregues, ordem → COMPLETED
        boolean allDelivered = order.getItems().stream()
                .allMatch(i -> i.getItemStatus() == SalesItemStatus.DELIVERED);

        if (allDelivered) {
            order.setStatus(SalesOrderStatus.COMPLETED);
            log.info("All items delivered — sales order {} auto-completed", orderId);
        }

        SalesOrder saved = orderRepository.save(order);
        log.info("Sales item {} delivered in order {}", itemId, orderId);
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SalesOrderItem findItem(SalesOrder order, UUID itemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Item " + itemId + " não encontrado no pedido " + order.getId()));
    }

    /**
     * Obtém o ID do usuário autenticado do contexto de segurança.
     *
     * <p>O {@code SecurityFilter} popula o contexto com {@code UserAccount} como principal.
     * Extraímos o UUID diretamente sem tentar parsear o username como UUID.</p>
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserAccount userAccount) {
            return userAccount.getId();
        }
        // Fallback: nunca deve ocorrer em produção com JWT configurado corretamente
        log.warn("Authenticated principal is not a UserAccount — principal type: {}",
                auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null");
        throw new ValidationException("Usuário autenticado não identificado. Faça login novamente.");
    }

    private void assertStatus(SalesOrder order, SalesOrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw new ValidationException(
                    "Para " + action + ", pedido deve estar " + expected
                    + ", status atual: " + order.getStatus());
        }
    }

    /** Verifica se o status do pedido é >= expected na ordem do enum. */
    private void assertMinStatus(SalesOrder order, SalesOrderStatus minStatus, String action) {
        if (order.getStatus().ordinal() < minStatus.ordinal()
            || order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new ValidationException(
                    "Para " + action + ", pedido deve estar ao menos " + minStatus
                    + ", status atual: " + order.getStatus());
        }
    }
}

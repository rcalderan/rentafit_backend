package br.com.rentafit.sales.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sales.config.SalesBillingProperties;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.enums.InvoiceStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gerencia emissão de NFS-e para vendas.
 *
 * <p>A emissão real é delegada ao módulo Billing existente.
 * Este service coordena a flag e o status da invoice no pedido.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalesBillingService {

    private final SalesBillingProperties billingProperties;
    private final SalesOrderRepository orderRepository;
    private final SalesOrderService orderService;
    private final SalesMapper mapper;

    /** Chamado automaticamente pelo SalesPaymentService ao transitar para PAID. */
    public void onOrderPaid(SalesOrder order) {
        if (billingProperties.isAutoEmitOnPayment()) {
            emitInvoiceInternal(order);
        } else {
            order.setInvoiceStatus(InvoiceStatus.PENDING_EMISSION);
            log.info("NFS-e pendente de emissão manual para pedido {}", order.getId());
        }
    }

    /** Endpoint manual para emitir NFS-e. */
    public SalesOrderDetailsDTO emitInvoice(UUID orderId) {
        SalesOrder order = orderService.findEntityById(orderId);

        if (order.getStatus() != SalesOrderStatus.PAID && order.getStatus() != SalesOrderStatus.COMPLETED) {
            throw new ValidationException(
                    "NFS-e só pode ser emitida após pagamento completo, status atual: " + order.getStatus());
        }

        if (order.getInvoiceStatus() == InvoiceStatus.EMITTED) {
            throw new ValidationException("NFS-e já foi emitida para este pedido: " + order.getInvoiceId());
        }

        emitInvoiceInternal(order);
        SalesOrder saved = orderRepository.save(order);
        return mapper.toDetailsDTO(saved, null);
    }

    /**
     * Integração com módulo Billing.
     * TODO: chamar BillingService.emit() quando módulo estiver funcional.
     * Por ora, apenas marca como EMITTED com placeholder.
     */
    private void emitInvoiceInternal(SalesOrder order) {
        // TODO: integração real com billing service
        // String invoiceId = billingService.emit(order);
        String invoiceId = "NFSE-PLACEHOLDER-" + order.getLegacyId();
        order.setInvoiceStatus(InvoiceStatus.EMITTED);
        order.setInvoiceId(invoiceId);
        log.info("NFS-e emitida para pedido {}: {}", order.getId(), invoiceId);
    }
}

package br.com.rentafit.sales.port;

import br.com.rentafit.sales.domain.SalesOrder;

import java.util.Optional;

/**
 * Porta de acesso ao componente Billing para emissão de NFS-e a partir de vendas.
 *
 * <p>Abstrai a dependência do módulo Sales sobre o módulo Billing.
 * Ao migrar para microserviço, apenas o adapter é substituído por um cliente HTTP.</p>
 */
public interface BillingInvoicePort {

    /**
     * Emite NFS-e para um pedido de venda.
     *
     * @param order pedido de venda pago/completado
     * @return snapshot da emissão com chave de acesso e protocolo
     * @throws IllegalStateException se a emissão falhar ou cliente não encontrado
     */
    InvoiceSnapshot emitInvoice(SalesOrder order);

    /**
     * Snapshot da NFS-e emitida, desacoplando o resultado da implementação interna.
     */
    record InvoiceSnapshot(
            String accessKey,
            String protocol,
            String status,
            String invoiceId
    ) {}
}

package br.com.rentafit.sales.adapter;

import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.nfse.NfseEmissionService;
import br.com.rentafit.sales.config.SalesBillingProperties;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.mapper.SalesMapper;
import br.com.rentafit.sales.port.BillingInvoicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter que implementa BillingInvoicePort usando NfseEmissionService.
 *
 * <p>Ao migrar para microserviço, substituir por cliente HTTP do serviço Billing.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingInvoiceAdapter implements BillingInvoicePort {

    private final NfseEmissionService nfseEmissionService;
    private final SalesBillingProperties billingProperties;
    private final SalesMapper mapper;

    @Override
    public InvoiceSnapshot emitInvoice(SalesOrder order) {
        if (order.getCustomerId() == null) {
            throw new IllegalStateException("Pedido sem cliente vinculado não pode emitir NFS-e: " + order.getId());
        }

        BigDecimal subtotal = mapper.computeSubtotal(order.getItems());
        BigDecimal serviceValue = subtotal.subtract(order.getDiscountValue()).max(BigDecimal.ZERO);

        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(order.getCustomerId())
                .serviceValue(serviceValue)
                .nbsCode(billingProperties.getNbsCode())
                .serviceDescription(billingProperties.getServiceDescription())
                .cityCode(billingProperties.getCityCode())
                .origin("SALES")
                .originId(order.getId())
                .build();

        log.info("Emitindo NFS-e para pedido {}: cliente={}, valor={}",
                order.getId(), order.getCustomerId(), serviceValue);

        InvoiceEmissionResponseDTO response = nfseEmissionService.emit(request).block();

        if (response == null || response.getAccessKey() == null) {
            throw new IllegalStateException("Falha na emissão da NFS-e: resposta inválida do serviço billing");
        }

        log.info("NFS-e emitida com sucesso: chave={}, protocolo={}",
                response.getAccessKey(), response.getProtocol());

        return new InvoiceSnapshot(
                response.getAccessKey(),
                response.getProtocol(),
                response.getStatus(),
                response.getAccessKey()
        );
    }
}

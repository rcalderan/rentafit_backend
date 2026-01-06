package br.com.rentafit.billing.service;

import br.com.rentafit.billing.client.NfsePortalClient;
import br.com.rentafit.billing.domain.Invoice;
import br.com.rentafit.billing.domain.TaxInfo;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final NfsePortalClient nfsePortalClient;
    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;

    public Mono<Invoice> emitInvoice(UUID customerId, BigDecimal serviceValue) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        DpsRequest dpsRequest = buildDpsRequest(customer, serviceValue);

        // TODO: Obter token OAuth2 real do portal
        String mockToken = "mock-token";

        return nfsePortalClient.sendDps(dpsRequest, mockToken)
                .map(response -> {
                    Invoice invoice = Invoice.builder()
                            .accessKey(response.getAccessKey())
                            .invoiceNumber(123L) // TODO: Pegar do retorno real
                            .customer(customer)
                            .issueDate(OffsetDateTime.now())
                            .serviceValue(serviceValue)
                            .status(Invoice.InvoiceStatus.AUTHORIZED)
                            .taxes(TaxInfo.builder()
                                    .ibsValue(serviceValue.multiply(new BigDecimal("0.025")))
                                    .cbsValue(serviceValue.multiply(new BigDecimal("0.015")))
                                    .build())
                            .build();
                    return invoiceService.save(invoice);
                });
    }

    private DpsRequest buildDpsRequest(Customer customer, BigDecimal serviceValue) {
        // Mapeamento simplificado para exemplo
        return DpsRequest.builder()
                .infDPS(DpsRequest.InfDPS.builder()
                        .dhEmi(OffsetDateTime.now())
                        .tpAmb("2") // Homologação
                        .prest(DpsRequest.Prestador.builder()
                                .CNPJ("00000000000000") // TODO: Configurações da empresa
                                .build())
                        .toma(DpsRequest.Tomador.builder()
                                .nNome(customer.getName())
                                .identif(DpsRequest.Identificacao.builder()
                                        .CNPJ(customer.getDocument()) // Assumindo CNPJ no documento
                                        .build())
                                .build())
                        .vals(DpsRequest.Valores.builder()
                                .vServ(serviceValue)
                                .build())
                        .build())
                .build();
    }
}

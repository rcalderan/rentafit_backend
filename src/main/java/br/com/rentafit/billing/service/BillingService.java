package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.nfse.NfseEmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final NfseEmissionService emissionService;

    public Mono<InvoiceEmissionResponseDTO> emitInvoice(InvoiceEmissionRequestDTO request) {
        return emissionService.emit(request);
    }
}

package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.nfse.NfseEmissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private NfseEmissionService emissionService;

    @InjectMocks
    private BillingService billingService;

    @Test
    @DisplayName("emitInvoice delega para o caso de uso NFS-e canônico")
    void emitInvoice_delegaParaEmissaoCanonica() {
        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .serviceValue(BigDecimal.TEN)
                .nbsCode("1.0101")
                .serviceDescription("Locação de traje")
                .cityCode("3550308")
                .build();
        InvoiceEmissionResponseDTO expected = InvoiceEmissionResponseDTO.builder()
                .accessKey("chave-fiscal")
                .build();
        when(emissionService.emit(request)).thenReturn(Mono.just(expected));

        StepVerifier.create(billingService.emitInvoice(request))
                .expectNext(expected)
                .verifyComplete();

        verify(emissionService).emit(request);
    }
}

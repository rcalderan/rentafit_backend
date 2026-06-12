package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfseController - endpoints de emissão NFS-e")
class NfseControllerTest {

    @Mock NfseEmissionService emissionService;
    @InjectMocks NfseController controller;

    @Test
    @DisplayName("emitir() retorna 201 quando service entrega resposta")
    void emitir_retorna201() {
        when(emissionService.emit(any())).thenReturn(Mono.just(responsePadrao()));

        Mono<ResponseEntity<InvoiceEmissionResponseDTO>> result = controller.emitir(requestPadrao());

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(resp.getBody()).isNotNull();
                    assertThat(resp.getBody().getAccessKey()).isEqualTo("CHAVE-50");
                    assertThat(resp.getBody().getStatus()).isEqualTo("AUTHORIZED");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("emitir() retorna 500 quando service lança RuntimeException")
    void emitir_retorna500_erroService() {
        when(emissionService.emit(any())).thenReturn(Mono.error(new RuntimeException("Portal indisponível")));

        Mono<ResponseEntity<InvoiceEmissionResponseDTO>> result = controller.emitir(requestPadrao());

        StepVerifier.create(result)
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR))
                .verifyComplete();
    }

    @Test
    @DisplayName("emitir() propaga sem wrapar quando service retorna empty")
    void emitir_propagaVazio_serviceEmpty() {
        when(emissionService.emit(any())).thenReturn(Mono.empty());

        Mono<ResponseEntity<InvoiceEmissionResponseDTO>> result = controller.emitir(requestPadrao());

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("emitir() repassa serviceValue da resposta")
    void emitir_repassaServiceValue() {
        when(emissionService.emit(any())).thenReturn(Mono.just(responsePadrao()));

        Mono<ResponseEntity<InvoiceEmissionResponseDTO>> result = controller.emitir(requestPadrao());

        StepVerifier.create(result)
                .assertNext(resp -> assertThat(resp.getBody().getServiceValue())
                        .isEqualByComparingTo(BigDecimal.valueOf(500)))
                .verifyComplete();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private InvoiceEmissionRequestDTO requestPadrao() {
        return InvoiceEmissionRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .serviceValue(BigDecimal.valueOf(500))
                .nbsCode("1.0101")
                .serviceDescription("Locação de traje")
                .cityCode("3550308")
                .origin("SALES")
                .build();
    }

    private InvoiceEmissionResponseDTO responsePadrao() {
        return InvoiceEmissionResponseDTO.builder()
                .id(UUID.randomUUID())
                .accessKey("CHAVE-50")
                .status("AUTHORIZED")
                .issueDate(OffsetDateTime.now())
                .serviceValue(BigDecimal.valueOf(500))
                .taxes(InvoiceEmissionResponseDTO.TaxInfoDTO.builder()
                        .ibsRate(BigDecimal.valueOf(0.025))
                        .ibsValue(BigDecimal.valueOf(12.5))
                        .cbsRate(BigDecimal.valueOf(0.015))
                        .cbsValue(BigDecimal.valueOf(7.5))
                        .build())
                .build();
    }
}

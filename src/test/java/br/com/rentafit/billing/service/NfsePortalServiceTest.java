package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.NfseConsultaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfsePortalServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private StsTokenService stsTokenService;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec getHeadersSpec;

    private NfsePortalService nfsePortalService;

    @BeforeEach
    void setUp() {
        nfsePortalService = new NfsePortalService(webClient, stsTokenService);
    }

    @Test
    @DisplayName("Should send DPS successfully")
    void shouldSendDpsSuccessfully() {
        DpsRequest request = DpsRequest.builder().build();
        DpsResponse response = DpsResponse.builder().protocol("123").build();

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DpsResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(nfsePortalService.sendDps(request))
                .expectNextMatches(res -> res.getProtocol().equals("123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when sending DPS")
    void shouldHandleErrorWhenSendingDps() {
        DpsRequest request = DpsRequest.builder().build();

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DpsResponse.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        StepVerifier.create(nfsePortalService.sendDps(request))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should consult NFS-e successfully")
    void shouldConsultNfseSuccessfully() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";
        NfseConsultaResponse response = NfseConsultaResponse.builder()
                .status("AUTORIZADA")
                .chaveAcesso(chaveAcesso)
                .build();

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NfseConsultaResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(nfsePortalService.consultarNfse(chaveAcesso))
                .expectNextMatches(res -> res.getStatus().equals("AUTORIZADA"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should download PDF successfully")
    void shouldDownloadPdfSuccessfully() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";
        byte[] pdfBytes = "PDF content".getBytes();
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(pdfBytes);

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

        StepVerifier.create(nfsePortalService.downloadPdf(chaveAcesso))
                .expectNextMatches(bytes -> bytes.length > 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should download XML successfully")
    void shouldDownloadXmlSuccessfully() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";
        String xmlContent = "<?xml version=\"1.0\"?><NFe></NFe>";

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(xmlContent));

        StepVerifier.create(nfsePortalService.downloadXml(chaveAcesso))
                .expectNextMatches(xml -> xml.contains("NFe"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when consulting NFS-e")
    void shouldHandleErrorWhenConsultingNfse() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NfseConsultaResponse.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        StepVerifier.create(nfsePortalService.consultarNfse(chaveAcesso))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle error when downloading PDF")
    void shouldHandleErrorWhenDownloadingPdf() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(DataBuffer.class))
                .thenReturn(Flux.error(new RuntimeException("PDF download error")));

        StepVerifier.create(nfsePortalService.downloadPdf(chaveAcesso))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle error when downloading XML")
    void shouldHandleErrorWhenDownloadingXml() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("XML download error")));

        StepVerifier.create(nfsePortalService.downloadXml(chaveAcesso))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should download PDF with multiple data buffers")
    void shouldDownloadPdfWithMultipleDataBuffers() {
        String chaveAcesso = "12345678901234567890123456789012345678901234";
        byte[] part1 = "PDF part 1 ".getBytes();
        byte[] part2 = "PDF part 2".getBytes();
        DataBuffer buffer1 = new DefaultDataBufferFactory().wrap(part1);
        DataBuffer buffer2 = new DefaultDataBufferFactory().wrap(part2);

        when(stsTokenService.obterToken()).thenReturn(Mono.just("valid-token"));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.headers(any())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(buffer1, buffer2));

        StepVerifier.create(nfsePortalService.downloadPdf(chaveAcesso))
                .expectNextMatches(bytes -> bytes.length == part1.length + part2.length)
                .verifyComplete();
    }
}


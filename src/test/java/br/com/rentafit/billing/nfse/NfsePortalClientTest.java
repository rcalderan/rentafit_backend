package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.NfseConsultaResponse;
import br.com.rentafit.billing.service.StsTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfsePortalClient — cliente unificado Portal Nacional NFS-e")
class NfsePortalClientTest {

    @Mock private WebClient webClient;
    @Mock private StsTokenService stsTokenService;
    @Mock private NfseDpsPayloadEncoder payloadEncoder;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private NfsePortalClient client;

    @BeforeEach
    void setUp() {
        client = new NfsePortalClient(webClient, stsTokenService, payloadEncoder);
        when(stsTokenService.obterToken()).thenReturn(Mono.just("test-token"));
    }

    @Test
    @DisplayName("sendDps() envia para /sefinnacional/nfse com GZip+Base64 JSON")
    @SuppressWarnings("unchecked")
    void sendDps_enviaParaEndpointCorreto() {
        String signedXml = "<DPS><infDPS>test</infDPS></DPS>";
        NfseDpsPayload payload = new NfseDpsPayload("gzipBase64Content");
        DpsResponse expected = DpsResponse.builder().accessKey("chave123").protocol("prot456").build();

        when(payloadEncoder.encode(signedXml)).thenReturn(payload);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/sefinnacional/nfse"))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(payload)).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DpsResponse.class)).thenReturn(Mono.just(expected));

        StepVerifier.create(client.sendDps(signedXml))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("consultarNfse() usa GET /sefinnacional/nfse/{chave}")
    @SuppressWarnings("unchecked")
    void consultarNfse_usaEndpointCorreto() {
        NfseConsultaResponse expected = NfseConsultaResponse.builder()
                .chaveAcesso("chave50digitos")
                .status("AUTORIZADA")
                .build();

        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/sefinnacional/nfse/{chave}"), eq("chave50digitos")))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NfseConsultaResponse.class)).thenReturn(Mono.just(expected));

        StepVerifier.create(client.consultarNfse("chave50digitos"))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("downloadPdf() usa GET /sefinnacional/nfse/{chave}/pdf")
    @SuppressWarnings("unchecked")
    void downloadPdf_usaEndpointCorreto() {
        byte[] pdfBytes = new byte[]{1, 2, 3};

        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/sefinnacional/nfse/{chave}/pdf"), eq("chaveXYZ")))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(pdfBytes));

        StepVerifier.create(client.downloadPdf("chaveXYZ"))
                .expectNext(pdfBytes)
                .verifyComplete();
    }

    @Test
    @DisplayName("downloadXml() usa GET /sefinnacional/nfse/{chave}/xml")
    @SuppressWarnings("unchecked")
    void downloadXml_usaEndpointCorreto() {
        String xmlContent = "<NFSe>...</NFSe>";

        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/sefinnacional/nfse/{chave}/xml"), eq("chaveABC")))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(xmlContent));

        StepVerifier.create(client.downloadXml("chaveABC"))
                .expectNext(xmlContent)
                .verifyComplete();
    }
}

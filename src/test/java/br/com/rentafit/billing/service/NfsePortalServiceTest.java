package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
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
}


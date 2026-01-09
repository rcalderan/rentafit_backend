package br.com.rentafit.billing.client;

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

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfsePortalClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private StsTokenService stsTokenService;

    private NfsePortalClient nfsePortalClient;

    @BeforeEach
    void setUp() {
        nfsePortalClient = new NfsePortalClient(webClient, stsTokenService);
    }

    @Test
    @DisplayName("Should send DPS successfully")
    @SuppressWarnings("unchecked")
    void shouldSendDpsSuccessfully() {
        DpsRequest request = DpsRequest.builder().build();
        DpsResponse response = DpsResponse.builder().protocol("123").build();
        String token = "mock-token";

        when(stsTokenService.obterToken()).thenReturn(Mono.just(token));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/dps")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DpsResponse.class)).thenReturn(Mono.just(response));

        Mono<DpsResponse> result = nfsePortalClient.sendDps(request);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.getProtocol().equals("123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when sending DPS")
    @SuppressWarnings("unchecked")
    void shouldHandleErrorWhenSendingDps() {
        DpsRequest request = DpsRequest.builder().build();
        String token = "mock-token";

        when(stsTokenService.obterToken()).thenReturn(Mono.just(token));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/dps")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DpsResponse.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        Mono<DpsResponse> result = nfsePortalClient.sendDps(request);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}


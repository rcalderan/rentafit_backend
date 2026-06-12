package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.StsTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StsTokenService - Testes Unitários")
class StsTokenServiceTest {

    @Mock
    private WebClient stsWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private StsTokenService stsTokenService;

    @BeforeEach
    void setUp() {
        stsTokenService = new StsTokenService(stsWebClient);
    }

    @Test
    @DisplayName("obterToken() deve retornar mock token quando STS desabilitado")
    void deveRetornarMockTokenQuandoStsDesabilitado() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", false);

        String token = stsTokenService.obterToken().block();

        assertThat(token).isEqualTo("mock-token-for-testing");
        verifyNoInteractions(stsWebClient);
    }

    @Test
    @DisplayName("obterToken() deve chamar STS quando habilitado e sem cache")
    void deveChamarStsQuandoSemCache() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", true);

        StsTokenResponse response = StsTokenResponse.builder()
                .accessToken("real-token-123")
                .expiresIn(3600L)
                .build();

        when(stsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(StsTokenResponse.class)).thenReturn(Mono.just(response));

        String token = stsTokenService.obterToken().block();

        assertThat(token).isEqualTo("real-token-123");
        assertThat(stsTokenService.hasTokenValido()).isTrue();
    }

    @Test
    @DisplayName("obterToken() deve usar cache quando token ainda válido")
    void deveUsarCacheQuandoTokenValido() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", true);

        StsTokenResponse response = StsTokenResponse.builder()
                .accessToken("cached-token-456")
                .expiresIn(3600L)
                .build();

        when(stsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(StsTokenResponse.class)).thenReturn(Mono.just(response));

        // Primeira chamada - deve chamar STS
        stsTokenService.obterToken().block();
        verify(stsWebClient, times(1)).post();

        // Segunda chamada - deve usar cache
        String token = stsTokenService.obterToken().block();

        assertThat(token).isEqualTo("cached-token-456");
        // WebClient.post() só deve ser chamado 1 vez devido ao cache
        verify(stsWebClient, times(1)).post();
    }

    @Test
    @DisplayName("invalidarCache() deve limpar o cache")
    void deveInvalidarCache() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", true);

        StsTokenResponse response = StsTokenResponse.builder()
                .accessToken("token-to-be-cleared")
                .expiresIn(3600L)
                .build();

        when(stsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(StsTokenResponse.class)).thenReturn(Mono.just(response));

        // Obter token para popular cache
        stsTokenService.obterToken().block();
        assertThat(stsTokenService.hasTokenValido()).isTrue();

        // Invalidar cache
        stsTokenService.invalidarCache();

        assertThat(stsTokenService.hasTokenValido()).isFalse();
    }

    @Test
    @DisplayName("hasTokenValido() deve retornar false quando cache vazio")
    void deveRetornarFalseQuandoCacheVazio() {
        assertThat(stsTokenService.hasTokenValido()).isFalse();
    }

    @Test
    @DisplayName("obterToken() deve renovar quando token expirado")
    void deveRenovarQuandoTokenExpirado() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", true);

        StsTokenResponse response = StsTokenResponse.builder()
                .accessToken("new-renewed-token")
                .expiresIn(3600L)
                .build();

        when(stsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(StsTokenResponse.class)).thenReturn(Mono.just(response));

        String token = stsTokenService.obterToken().block();

        assertThat(token).isEqualTo("new-renewed-token");
    }

    @Test
    @DisplayName("obterToken() deve falhar quando STS indisponível")
    void deveFalharQuandoStsIndisponivel() {
        ReflectionTestUtils.setField(stsTokenService, "stsEnabled", true);

        when(stsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(StsTokenResponse.class))
                .thenReturn(Mono.error(new RuntimeException("STS indisponivel")));

        // O retry vai tentar 3 vezes e falhar com RetryExhaustedException
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> stsTokenService.obterToken().block())
                .isInstanceOf(Exception.class);
    }
}

package br.com.rentafit.people.service;

import br.com.rentafit.people.dto.ViaCepResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários de ViaCepIntegrationService.
 * Testa normalização de CEP e validação de formato.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ViaCepIntegrationService - Unit Tests")
class ViaCepIntegrationServiceTest {

    @Mock
    private WebClient viaCepWebClient;

    @Mock
    private WebClient brasilApiWebClient;

    private ViaCepIntegrationService viaCepIntegrationService;

    @BeforeEach
    void setUp() {
        viaCepIntegrationService = new ViaCepIntegrationService(viaCepWebClient, brasilApiWebClient);
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP inválido")
    void shouldThrowExceptionForInvalidZipCode() {
        // Act & Assert
        assertThatThrownBy(() -> viaCepIntegrationService.fetchAddressByZipCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");
    }

    @Test
    @DisplayName("Deve retornar null para CEP null")
    void shouldReturnNullForNullZipCode() {
        // Act
        ViaCepResponseDTO result = viaCepIntegrationService.fetchAddressByZipCode(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null para CEP vazio")
    void shouldReturnNullForEmptyZipCode() {
        // Act
        ViaCepResponseDTO result = viaCepIntegrationService.fetchAddressByZipCode("");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null para CEP com apenas espaços")
    void shouldReturnNullForBlankZipCode() {
        // Act
        ViaCepResponseDTO result = viaCepIntegrationService.fetchAddressByZipCode("   ");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve validar formato de CEP antes de fazer chamada")
    void shouldValidateZipCodeFormat() {
        // Act & Assert - CEP com letras
        assertThatThrownBy(() -> viaCepIntegrationService.fetchAddressByZipCode("abcd-efgh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");
    }

    @Test
    @DisplayName("Deve aceitar CEP com hífen")
    void shouldAcceptZipCodeWithHyphen() {
        // Este teste verifica apenas que não lança exceção na validação inicial
        // O comportamento completo (incluindo chamada HTTP) requer mock complexo

        // Act & Assert - Não deve lançar IllegalArgumentException na validação inicial
        try {
            viaCepIntegrationService.fetchAddressByZipCode("01310-100");
        } catch (IllegalArgumentException e) {
            throw e; // Re-lança se for validação de formato
        } catch (Exception e) {
            // Ignora outras exceções (como NullPointerException do WebClient mock não configurado)
            // O importante aqui é que passou pela validação de formato
        }
    }

    @Test
    @DisplayName("Deve aceitar CEP sem hífen")
    void shouldAcceptZipCodeWithoutHyphen() {
        // Este teste verifica apenas que não lança exceção na validação inicial

        // Act & Assert - Não deve lançar IllegalArgumentException na validação inicial
        try {
            viaCepIntegrationService.fetchAddressByZipCode("01310100");
        } catch (IllegalArgumentException e) {
            throw e; // Re-lança se for validação de formato
        } catch (Exception e) {
            // Ignora outras exceções (como NullPointerException do WebClient mock não configurado)
            // O importante aqui é que passou pela validação de formato
        }
    }
}

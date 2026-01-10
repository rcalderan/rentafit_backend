package br.com.rentafit.people.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários de ViaCepIntegrationService.
 * Testa normalização de CEP e validação de formato.
 */
@DisplayName("ViaCepIntegrationService - Unit Tests")
class ViaCepIntegrationServiceTest {

    private final ViaCepIntegrationService viaCepIntegrationService = new ViaCepIntegrationService(null);

    @Test
    @DisplayName("Deve lançar exceção para CEP inválido")
    void shouldThrowExceptionForInvalidZipCode() {
        // Act & Assert
        assertThatThrownBy(() -> viaCepIntegrationService.fetchAddressByZipCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP null")
    void shouldThrowExceptionForNullZipCode() {
        // Act & Assert
        assertThatThrownBy(() -> viaCepIntegrationService.fetchAddressByZipCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP vazio")
    void shouldThrowExceptionForEmptyZipCode() {
        // Act & Assert
        assertThatThrownBy(() -> viaCepIntegrationService.fetchAddressByZipCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


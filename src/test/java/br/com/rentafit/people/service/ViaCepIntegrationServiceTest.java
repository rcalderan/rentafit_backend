package br.com.rentafit.people.service;

import br.com.rentafit.people.dto.ViaCepResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
}


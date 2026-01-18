package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de AddressService.
 * Testa busca, criação e integração com ViaCEP.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService - Unit Tests")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ViaCepIntegrationService viaCepIntegrationService;

    @InjectMocks
    private AddressService addressService;

    @BeforeEach
    void setUp() {
        // Setup comum para todos os testes
    }

    // ==================== findByZipCode Tests ====================

    @Test
    @DisplayName("Deve encontrar endereço por CEP normalizado")
    void shouldFindAddressByNormalizedZipCode() {
        // Arrange
        String zipCode = "01310-100";
        Address expectedAddress = new Address(
                "01310100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP"
        );

        when(addressRepository.findByZipCode("01310100")).thenReturn(Optional.of(expectedAddress));

        // Act
        AddressDTO result = addressService.findByZipCode(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.zipCode()).isEqualTo("01310-100");
        assertThat(result.street()).isEqualTo("Avenida Paulista");
        assertThat(result.city()).isEqualTo("São Paulo");
        verify(addressRepository).findByZipCode("01310100");
    }

    @Test
    @DisplayName("Deve lançar exceção quando endereço não encontrado")
    void shouldThrowExceptionWhenAddressNotFound() {
        // Arrange
        String zipCode = "99999-999";
        when(addressRepository.findByZipCode("99999999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.findByZipCode(zipCode))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP inválido")
    void shouldThrowExceptionForInvalidZipCode() {
        // Act & Assert
        assertThatThrownBy(() -> addressService.findByZipCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== findOrCreateByZipCode Tests ====================

    @Test
    @DisplayName("Deve retornar endereço existente")
    void shouldReturnExistingAddress() {
        // Arrange
        String zipCode = "01310-100";
        Address existingAddress = new Address(
                "01310100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP"
        );

        when(addressRepository.findByZipCode("01310100")).thenReturn(Optional.of(existingAddress));

        // Act
        Address result = addressService.findOrCreateByAddress(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo("01310100");
        assertThat(result.getStreet()).isEqualTo("Avenida Paulista");
        verify(addressRepository).findByZipCode("01310100");
        verify(viaCepIntegrationService, never()).fetchAddressByZipCode(anyString());
    }

    @Test
    @DisplayName("Deve criar novo endereço a partir da ViaCEP quando não encontrado")
    void shouldCreateNewAddressFromViaCepWhenNotFound() {
        // Arrange
        String zipCode = "01310-100";
        String normalized = "01310100";

        ViaCepResponseDTO viaCepData = ViaCepResponseDTO.builder()
                .cep("01310100")
                .logradouro("Avenida Paulista")
                .bairro("Bela Vista")
                .localidade("São Paulo")
                .uf("SP")
                .erro(false)
                .build();

        Address newAddress = new Address(
                normalized,
                viaCepData.logradouro(),
                viaCepData.bairro(),
                viaCepData.localidade(),
                viaCepData.uf()
        );

        when(addressRepository.findByZipCode(normalized)).thenReturn(Optional.empty());
        when(viaCepIntegrationService.fetchAddressByZipCode("01310100")).thenReturn(viaCepData);
        when(addressRepository.save(any(Address.class))).thenReturn(newAddress);

        // Act
        Address result = addressService.findOrCreateByAddress(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo(normalized);
        assertThat(result.getStreet()).isEqualTo("Avenida Paulista");
        verify(addressRepository).findByZipCode(normalized);
        verify(viaCepIntegrationService).fetchAddressByZipCode(normalized);
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve criar endereço mínimo quando ViaCEP falha")
    void shouldCreateMinimalAddressWhenViaCepFails() {
        // Arrange
        String zipCode = "01310-100";
        String normalized = "01310100";

        when(addressRepository.findByZipCode(normalized)).thenReturn(Optional.empty());
        when(viaCepIntegrationService.fetchAddressByZipCode(normalized)).thenReturn(null);

        Address fallbackAddress = new Address(
                normalized,
                "Endereço não encontrado",
                "",
                "Cidade não informada",
                "SP"
        );

        when(addressRepository.save(any(Address.class))).thenReturn(fallbackAddress);

        // Act
        Address result = addressService.findOrCreateByAddress(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo(normalized);
        assertThat(result.getStreet()).isEqualTo("Endereço não encontrado");
        verify(viaCepIntegrationService).fetchAddressByZipCode(normalized);
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve criar endereço mínimo quando ViaCEP retorna erro")
    void shouldCreateMinimalAddressWhenViaCepReturnsError() {
        // Arrange
        String zipCode = "99999-999";
        String normalized = "99999999";

        ViaCepResponseDTO errorResponse = ViaCepResponseDTO.builder()
                .erro(true)
                .build();

        when(addressRepository.findByZipCode(normalized)).thenReturn(Optional.empty());
        when(viaCepIntegrationService.fetchAddressByZipCode("99999999")).thenReturn(errorResponse);

        Address fallbackAddress = new Address(
                normalized,
                "Endereço não encontrado",
                "",
                "Cidade não informada",
                "SP"
        );

        when(addressRepository.save(any(Address.class))).thenReturn(fallbackAddress);

        // Act
        Address result = addressService.findOrCreateByAddress(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStreet()).isEqualTo("Endereço não encontrado");
    }

    @Test
    @DisplayName("Deve normalizar CEP com hiphen antes de buscar")
    void shouldNormalizeZipCodeBeforeSearch() {
        // Arrange
        String zipCode = "  01310-100  ";
        Address expectedAddress = new Address(
                "01310100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP"
        );

        when(addressRepository.findByZipCode("01310100")).thenReturn(Optional.of(expectedAddress));

        // Act
        AddressDTO result = addressService.findByZipCode(zipCode);

        // Assert
        assertThat(result).isNotNull();
        verify(addressRepository).findByZipCode("01310100");
    }

    @Test
    @DisplayName("Deve tratar null fields de ViaCEP graciosamente")
    void shouldHandleNullFieldsFromViaCep() {
        // Arrange
        String zipCode = "01310-100";
        String normalized = "01310100";

        ViaCepResponseDTO viaCepData = ViaCepResponseDTO.builder()
                .cep("01310100")
                .logradouro(null)
                .bairro(null)
                .localidade(null)
                .uf(null)
                .erro(false)
                .build();

        when(addressRepository.findByZipCode(normalized)).thenReturn(Optional.empty());
        when(viaCepIntegrationService.fetchAddressByZipCode(normalized)).thenReturn(viaCepData);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.findOrCreateByAddress(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo(normalized);
        assertThat(result.getStreet()).isEmpty();
    }
}


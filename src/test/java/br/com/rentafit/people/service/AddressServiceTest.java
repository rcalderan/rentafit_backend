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

import java.util.Collections;
import java.util.List;
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
    @DisplayName("Deve encontrar endereço por CEP formatado")
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

        when(addressRepository.findByZipCode("01310100")).thenReturn(List.of(expectedAddress));

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
        when(addressRepository.findByZipCode("99999999")).thenReturn(List.of());

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

        when(addressRepository.findByZipCode("01310100")).thenReturn(List.of(existingAddress));

        // Act
        Address result = addressService.findOrCreateByZipcode(zipCode);

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

        when(addressRepository.findByZipCode(normalized)).thenReturn(List.of());
        when(viaCepIntegrationService.fetchAddressByZipCode("01310100")).thenReturn(viaCepData);
        when(addressRepository.save(any(Address.class))).thenReturn(newAddress);

        // Act
        Address result = addressService.findOrCreateByZipcode(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo(normalized);
        assertThat(result.getStreet()).isEqualTo("Avenida Paulista");
        verify(addressRepository).findByZipCode(normalized);
        verify(viaCepIntegrationService).fetchAddressByZipCode(normalized);
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando ViaCEP falha e não há dados completos")
    void shouldThrowExceptionWhenViaCepFails() {
        // Arrange
        String zipCode = "01310-100";
        String normalized = "01310100";

        when(addressRepository.findByZipCode(normalized)).thenReturn(List.of());
        when(viaCepIntegrationService.fetchAddressByZipCode(normalized)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> addressService.findOrCreateByZipcode(zipCode))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(viaCepIntegrationService).fetchAddressByZipCode(normalized);
    }

    @Test
    @DisplayName("Deve lançar exceção quando ViaCEP retorna erro")
    void shouldThrowExceptionWhenViaCepReturnsError() {
        // Arrange
        String zipCode = "99999-999";
        String normalized = "99999999";

        ViaCepResponseDTO errorResponse = ViaCepResponseDTO.builder()
                .erro(true)
                .build();

        when(addressRepository.findByZipCode(normalized)).thenReturn(List.of());
        when(viaCepIntegrationService.fetchAddressByZipCode(normalized)).thenReturn(errorResponse);

        // Act & Assert
        assertThatThrownBy(() -> addressService.findOrCreateByZipcode(zipCode))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(viaCepIntegrationService).fetchAddressByZipCode(normalized);
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

        when(addressRepository.findByZipCode("01310100")).thenReturn(List.of(expectedAddress));

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

        when(addressRepository.findByZipCode(normalized)).thenReturn(List.of());
        when(viaCepIntegrationService.fetchAddressByZipCode(normalized)).thenReturn(viaCepData);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.findOrCreateByZipcode(zipCode);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getZipCode()).isEqualTo(normalized);
        assertThat(result.getStreet()).isEmpty();
    }

    @Test
    @DisplayName("Deve criar endereço manual quando ZIP não existe e dados manuais são fornecidos")
    void shouldCreateManualAddressWhenZipMissing() {
        // Arrange
        AddressDTO manualDto = AddressDTO.builder()
                .zipCode(null) // No zip
                .street("Estrada Rural, KM 10")
                .neighborhood("Zona Rural")
                .city("Itu")
                .state("SP")
                .build();

        when(addressRepository.findByZipCodeAndStreetAndCityAndState(null, "Estrada Rural, KM 10", "Itu", "SP"))
                .thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.findOrCreateByAddress(manualDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isManual()).isTrue();
        assertThat(result.getZipCode()).isNull();
        assertThat(result.getStreet()).isEqualTo("Estrada Rural, KM 10");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve criar endereço manual quando ViaCEP não encontra o CEP")
    void shouldCreateManualAddressWhenViaCepNotFound() {
        // Arrange
        String zipCode = "99999999";
        AddressDTO dto = AddressDTO.builder()
                .zipCode(zipCode)
                .street("Rua Desconhecida")
                .city("Cidade")
                .state("ZZ")
                .build();

        when(addressRepository.findByZipCodeAndStreetAndCityAndState(zipCode, "Rua Desconhecida", "Cidade", "ZZ"))
                .thenReturn(Optional.empty());
        when(viaCepIntegrationService.fetchAddressByZipCode(zipCode)).thenReturn(null);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.findOrCreateByAddress(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isManual()).isTrue();
        assertThat(result.getStreet()).isEqualTo("Rua Desconhecida");
        verify(viaCepIntegrationService).fetchAddressByZipCode(zipCode);
    }
}

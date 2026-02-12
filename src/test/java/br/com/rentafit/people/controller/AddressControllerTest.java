package br.com.rentafit.people.controller;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AddressController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressController - Unit Tests")
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        addressDTO = AddressDTO.builder()
                .zipCode("01310-100")
                .street("Avenida Paulista")
                .neighborhood("Bela Vista")
                .city("São Paulo")
                .state("SP")
                .build();
    }

    @Test
    @DisplayName("Should find address by ZIP code successfully")
    void testFindByZipCodeSuccess() {
        // Arrange
        String zipCode = "01310-100";
        when(addressService.findByZipCode(zipCode)).thenReturn(addressDTO);

        // Act
        ResponseEntity<AddressDTO> response = addressController.findByZipCode(zipCode);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().zipCode()).isEqualTo("01310-100");
        assertThat(response.getBody().street()).isEqualTo("Avenida Paulista");
        assertThat(response.getBody().city()).isEqualTo("São Paulo");

        verify(addressService, times(1)).findByZipCode(zipCode);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when address not found")
    void testFindByZipCodeNotFound() {
        // Arrange
        String zipCode = "99999-999";
        when(addressService.findByZipCode(zipCode))
                .thenThrow(new ResourceNotFoundException("Address", "zipCode", zipCode));

        // Act & Assert
        assertThatThrownBy(() -> addressController.findByZipCode(zipCode))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address");

        verify(addressService, times(1)).findByZipCode(zipCode);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid ZIP code")
    void testFindByZipCodeInvalid() {
        // Arrange
        String invalidZipCode = "invalid";
        when(addressService.findByZipCode(invalidZipCode))
                .thenThrow(new IllegalArgumentException("Invalid ZIP code format"));

        // Act & Assert
        assertThatThrownBy(() -> addressController.findByZipCode(invalidZipCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");

        verify(addressService, times(1)).findByZipCode(invalidZipCode);
    }

    @Test
    @DisplayName("Should handle normalized ZIP code")
    void testFindByZipCodeNormalized() {
        // Arrange
        String zipCode = "01310100"; // Without hyphen
        when(addressService.findByZipCode(zipCode)).thenReturn(addressDTO);

        // Act
        ResponseEntity<AddressDTO> response = addressController.findByZipCode(zipCode);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(addressService, times(1)).findByZipCode(zipCode);
    }
}


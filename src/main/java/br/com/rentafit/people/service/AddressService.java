package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.repository.AddressRepository;
import br.com.rentafit.people.util.ZipCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing addresses with ViaCEP integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final ViaCepIntegrationService viaCepIntegrationService;

    /**
     * Find address by ZIP code
     * @param zipCode ZIP code (can be formatted or normalized)
     * @return AddressDTO if found
     * @throws ResourceNotFoundException if address not found
     */
    @Transactional(readOnly = true)
    public AddressDTO findByZipCode(String zipCode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipCode);
        Address address = addressRepository.findByZipCode(normalizedZipCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Address", "zipCode", ZipCodeUtils.format(normalizedZipCode)));

        return toDTO(address);
    }

    /**
     * Find address by ZIP code or create from ViaCEP if not exists
     * @param zipCode ZIP code (can be formatted or normalized)
     * @return Address entity (saved if created)
     */
    @Transactional
    public Address findOrCreateByZipCode(String zipCode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipCode);

        // Try to find existing address
        return addressRepository.findByZipCode(normalizedZipCode)
                .orElseGet(() -> createFromViaCep(normalizedZipCode));
    }

    /**
     * Create address from ViaCEP API data
     * @param normalizedZipCode normalized ZIP code (8 digits)
     * @return newly created Address entity
     */
    private Address createFromViaCep(String normalizedZipCode) {
        log.info("Address not found locally, fetching from ViaCEP: {}", normalizedZipCode);

        ViaCepResponseDTO viaCepData = viaCepIntegrationService.fetchAddressByZipCode(normalizedZipCode);

        if (viaCepData == null || viaCepData.hasError()) {
            log.warn("Could not fetch address from ViaCEP for ZIP code: {}", normalizedZipCode);
            // Create minimal address with just ZIP code
            return addressRepository.save(new Address(
                normalizedZipCode,
                "Address not found",
                "",
                "City not provided",
                "SP"
            ));
        }

        // Create address from ViaCEP data
        Address address = new Address(
            normalizedZipCode,
            viaCepData.logradouro() != null ? viaCepData.logradouro() : "",
            viaCepData.bairro() != null ? viaCepData.bairro() : "",
            viaCepData.localidade() != null ? viaCepData.localidade() : "",
            viaCepData.uf() != null ? viaCepData.uf() : ""
        );

        Address savedAddress = addressRepository.save(address);
        log.info("Created new address from ViaCEP: {}", normalizedZipCode);

        return savedAddress;
    }

    /**
     * Convert Address entity to DTO
     */
    private AddressDTO toDTO(Address address) {
        if (address == null) return null;

        return AddressDTO.builder()
                .zipCode(ZipCodeUtils.format(address.getZipCode()))
                .street(address.getStreet())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .build();
    }
}


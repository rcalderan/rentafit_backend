package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ExternalServiceTimeoutException;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.repository.AddressRepository;
import br.com.rentafit.people.repository.PersonAddressDetailsRepository;
import br.com.rentafit.people.repository.PersonAddressHistoryRepository;
import br.com.rentafit.people.util.ZipCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Service for managing addresses with ViaCEP integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final ViaCepIntegrationService viaCepIntegrationService;
    private final PersonAddressDetailsRepository addressDetailsRepository;
    private final PersonAddressHistoryRepository addressHistoryRepository;

    /**
     * Find address by ZIP code
     * @param zipCode ZIP code (can be formatted or normalized)
     * @return AddressDTO if found
     * @throws ResourceNotFoundException if address not found
     */
    @Transactional(readOnly = true)
    public AddressDTO findByZipCode(String zipCode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipCode);

        // 1. Try to find in local database
        Address localAddress = addressRepository.findByZipCode(normalizedZipCode)
                .stream().findFirst()
                .orElse(null);

        if (localAddress != null) {
            return localAddress.toDTO();
        }

        // 2. Try to fetch from ViaCEP
        try {
            ViaCepResponseDTO viaCepData = viaCepIntegrationService.fetchAddressByZipCode(normalizedZipCode);

            if (viaCepData != null && !viaCepData.hasError()) {
                Address newAddress = new Address(viaCepData);
                addressRepository.save(newAddress);
                return newAddress.toDTO();
            }
        } catch (ExternalServiceTimeoutException e) {
            throw new ResponseStatusException(408, "ViaCEP service timeout", e);
        } catch (Exception e) {
            log.error("Error fetching from ViaCEP for ZIP code {}: {}", normalizedZipCode, e.getMessage());
            throw e;
        }

        // 3. Not found anywhere
        throw new ResourceNotFoundException("Address", "zipCode", ZipCodeUtils.format(normalizedZipCode));
    }

    /**
     * Find address by ZIP code or create from ViaCEP if not exists.
     * If ViaCEP doesn't have the address, it creates a manual entry.
     * @param addressDTO address details
     * @return Address entity
     */
    @Transactional
    public Address findOrCreateByAddress(AddressDTO addressDTO) {
        String normalizedZip = ZipCodeUtils.normalize(addressDTO.zipCode());

        // 1. Try to find existing address by composite key to avoid duplicates
        return addressRepository.findByZipCodeAndStreetAndCityAndState(
                        normalizedZip,
                        addressDTO.street(),
                        addressDTO.city(),
                        addressDTO.state())
                .orElseGet(() -> {
                    // 2. If not found locally and has ZIP code, attempt ViaCEP integration
                    if (normalizedZip != null) {
                        return createFromViaCep(addressDTO);
                    }
                    // 3. If no ZIP code, save as manual entry
                    log.info("Saving manual address without ZIP code: {}, {}", addressDTO.street(), addressDTO.city());
                    return addressRepository.save(new Address(addressDTO, true));
                });
    }

    @Transactional
    public Address findOrCreateByZipcode(String zipcode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipcode);
        if (normalizedZipCode == null) {
            throw new IllegalArgumentException("ZIP code cannot be null for Zipcode-only search");
        }

        // Try to find existing address by ZIP code
        Address existingAddress = addressRepository.findByZipCode(normalizedZipCode)
                .stream().findFirst()
                .orElse(null);

        if (existingAddress != null) {
            return existingAddress;
        }

        // Not found locally, try ViaCEP
        try {
            ViaCepResponseDTO viaCepData = viaCepIntegrationService.fetchAddressByZipCode(normalizedZipCode);

            if (viaCepData != null && !viaCepData.hasError()) {
                Address newAddress = new Address(viaCepData);
                return addressRepository.save(newAddress);
            }
        } catch (ExternalServiceTimeoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating address from ViaCEP for ZIP {}: {}", normalizedZipCode, e.getMessage());
        }

        // Could not find or create from ViaCEP
        throw new ResourceNotFoundException("Address", "zipCode", ZipCodeUtils.format(normalizedZipCode));
    }

    /**
     * Create address from ViaCEP API data or fallback to manual
     * @param dto address data
     * @return newly created Address entity
     */
    private Address createFromViaCep(AddressDTO dto) {
        String normalizedZipCode = ZipCodeUtils.normalize(dto.zipCode());
        log.info("Address not found locally, fetching from ViaCEP: {}", normalizedZipCode);

        try {
            ViaCepResponseDTO viaCepData = viaCepIntegrationService.fetchAddressByZipCode(normalizedZipCode);

            if (viaCepData != null && !viaCepData.hasError()) {
                // Create address from ViaCEP data
                Address addressToSave = new Address(viaCepData);
                log.info("Created new address from ViaCEP: {}", addressToSave.getZipCode());
                return addressRepository.save(addressToSave);
            }
        } catch (ExternalServiceTimeoutException e) {
            // Re-throw timeout to be handled at higher level
            throw e;
        } catch (Exception e) {
            log.error("Error fetching from ViaCEP for ZIP {}: {}", normalizedZipCode, e.getMessage());
        }

        // Could not fetch from ViaCEP, save as manual entry
        log.warn("Could not fetch address from ViaCEP for ZIP code: {}. Saving manual entry.", normalizedZipCode);
        Address manualAddress = new Address(dto, true);
        return addressRepository.save(manualAddress);
    }

    /**
     * Handle address update - archives old address if changed
     */
    public void handleAddressUpdate(Customer customer, CustomerDTO dto) {
        String newZipCode = ZipCodeUtils.normalize(dto.address().zipCode());
        PersonAddressDetails currentDetails = customer.getCurrentAddress();

        // Check if address actually changed (compare composite fields if ZIP is null)
        boolean addressChanged;
        if (currentDetails == null) {
            addressChanged = true;
        } else {
            Address cur = currentDetails.getAddress();
            AddressDTO next = dto.address();
            addressChanged = !Objects.equals(cur.getZipCode(), newZipCode) ||
                             !Objects.equals(cur.getStreet(), next.street()) ||
                             !Objects.equals(cur.getCity(), next.city()) ||
                             !Objects.equals(cur.getState(), next.state());
        }

        boolean detailsChanged = currentDetails != null && (
                !Objects.equals(currentDetails.getNumber(), dto.number()) ||
                        !Objects.equals(currentDetails.getComplement(), dto.complement())
        );

        if (addressChanged || detailsChanged) {
            // Archive old address if exists
            if (currentDetails != null) {
                archiveCurrentAddress(customer, currentDetails);
            }

            // Create new address details
            Address newAddress = findOrCreateByAddress(dto.address());

            PersonAddressDetails newDetails = new PersonAddressDetails();
            newDetails.setPerson(customer);
            newDetails.setAddress(newAddress);
            newDetails.setNumber(dto.number());
            newDetails.setComplement(dto.complement());
            newDetails.setStartDate(OffsetDateTime.now());
            newDetails.setEndDate(null);

            customer.setCurrentAddress(newDetails);

            log.info("Updated address for customer {}: {} -> {}",
                    customer.getId(),
                    currentDetails != null ? currentDetails.getAddress().getZipCode() : "none",
                    newZipCode);
        }
    }

    /**
     * Archive current address to history
     */
    private void archiveCurrentAddress(Customer customer, PersonAddressDetails currentDetails) {
        OffsetDateTime now = OffsetDateTime.now();

        // Set end date on current details
        currentDetails.setEndDate(now);
        addressDetailsRepository.save(currentDetails);

        // Create history record
        PersonAddressHistory history = new PersonAddressHistory();
        history.setPersonId(customer.getId());
        Address address = currentDetails.getAddress();
        history.setZipCode(address.getZipCode());
        history.setStreet(address.getStreet());
        history.setNeighborhood(address.getNeighborhood());
        history.setCity(address.getCity());
        history.setState(address.getState());
        history.setNumber(currentDetails.getNumber());
        history.setComplement(currentDetails.getComplement());
        history.setStartDate(currentDetails.getStartDate());
        history.setEndDate(now);
        history.setManual(address.isManual());

        addressHistoryRepository.save(history);

        log.debug("Archived address for customer {}: {}", customer.getId(),
                address.getZipCode());
    }
}


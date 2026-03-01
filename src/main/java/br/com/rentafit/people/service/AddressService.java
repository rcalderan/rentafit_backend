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
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
     * Lookup strategy:
     *   1. If ZIP is present → search by ZIP code first (covers the case of multiple
     *      customers living at the same address, avoiding unique-constraint violations).
     *   2. If not found by ZIP → try composite key (zip + street + city + state).
     *   3. If still not found → fetch from ViaCEP and persist.
     *   4. If no ZIP at all → persist as manual entry.
     *
     * @param addressDTO address details
     * @return Address entity (existing or newly created)
     */
    @Transactional
    public Address findOrCreateByAddress(AddressDTO addressDTO) {
        String normalizedZip = ZipCodeUtils.normalize(addressDTO.zipCode());

        // 1. When a ZIP code is provided, try to find an existing address by ZIP first.
        //    This is the main guard against duplicate-key errors: two customers at the
        //    same address will always reuse the same Address row.
        if (normalizedZip != null) {
            List<Address> byZip = addressRepository.findByZipCode(normalizedZip);
            if (!byZip.isEmpty()) {
                // If there is only one address for this ZIP, reuse it as before.
                if (byZip.size() == 1) {
                    log.debug("Reusing existing address for ZIP code: {}", normalizedZip);
                    return byZip.getFirst();
                }

                // Multiple addresses share this ZIP. Prefer one that matches street/city/state.
                Address matchedByComposition = null;
                for (Address candidate : byZip) {
                    if (Objects.equals(candidate.getStreet(), addressDTO.street())
                            && Objects.equals(candidate.getCity(), addressDTO.city())
                            && Objects.equals(candidate.getState(), addressDTO.state())) {
                        matchedByComposition = candidate;
                        break;
                    }
                }

                if (matchedByComposition != null) {
                    log.debug("Reusing existing address for ZIP + composition: {}, {}, {}, {}",
                            normalizedZip, addressDTO.street(), addressDTO.city(), addressDTO.state());
                    return matchedByComposition;
                }

                // No exact composition match; fall back to first entry for backward compatibility.
                log.warn("Multiple addresses found for ZIP {} but none matched street/city/state; reusing first result.",
                        normalizedZip);
                return byZip.getFirst();
            }

            // 2. Not in the local DB yet — create from ViaCEP (or fall back to manual)
            return createFromViaCep(addressDTO);
        }

        // 3. No ZIP code provided — try to match by full composition before inserting
        Optional<Address> byComposition = addressRepository.findByZipCodeAndStreetAndCityAndState(
                null,
                addressDTO.street(),
                addressDTO.city(),
                addressDTO.state());

        if (byComposition.isPresent()) {
            log.debug("Reusing existing manual address: {}, {}", addressDTO.street(), addressDTO.city());
            return byComposition.get();
        }

        // 4. Truly new manual address (no ZIP, no prior match)
        log.info("Saving new manual address without ZIP code: {}, {}", addressDTO.street(), addressDTO.city());
        return addressRepository.save(new Address(addressDTO, true));
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


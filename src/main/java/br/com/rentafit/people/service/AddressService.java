package br.com.rentafit.people.service;

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
        Address address = addressRepository.findByZipCode(normalizedZipCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Address", "zipCode", ZipCodeUtils.format(normalizedZipCode)));

        return toDTO(address);
    }

    /**
     * Find address by ZIP code or create from ViaCEP if not exists
     * @param addressDTO address
     * @return Address entity (saved if created)
     */
    @Transactional
    public Address findOrCreateByAddress(AddressDTO addressDTO) {
        Address address = new Address(addressDTO);
        // Try to find existing address
        return addressRepository.findByZipCode(address.getZipCode())
                .orElseGet(() -> createFromViaCep(address));
    }

    public Address findOrCreateByZipcode(String zipcode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipcode);
        // Try to find existing address
        return addressRepository.findByZipCode(normalizedZipCode)
                .orElseGet(() -> {
                    Address rawAddress = new Address(normalizedZipCode, "", "", "", "");
                    return createFromViaCep(rawAddress);
                });
    }


    /**
     * Create address from ViaCEP API data
     * @param address with normalizedZipCode (8 digits)
     * @return newly created Address entity
     */
    private Address createFromViaCep(Address address) {
        String normalizedZipCode = address.getZipCode();
        log.info("Address not found locally, fetching from ViaCEP: {}", normalizedZipCode);

        ViaCepResponseDTO viaCepData = viaCepIntegrationService.fetchAddressByZipCode(normalizedZipCode);

        if (viaCepData == null || viaCepData.hasError()) {
            log.warn("Could not fetch address from ViaCEP for ZIP code: {}", normalizedZipCode);
            return addressRepository.save(address);
        }

        // Create address from ViaCEP data
        Address viaCepAddress = new Address(viaCepData);

        Address savedAddress = addressRepository.save(viaCepAddress);
        log.info("Created new address from ViaCEP: {}", savedAddress.getZipCode());

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

    /**
     * Handle address update - archives old address if changed
     */
    public void handleAddressUpdate(Customer customer, CustomerDTO dto) {
        String newZipCode = ZipCodeUtils.normalize(dto.address().zipCode());
        PersonAddressDetails currentDetails = customer.getCurrentAddress();

        // Check if address actually changed
        boolean addressChanged = currentDetails == null ||
                !currentDetails.getAddress().getZipCode().equals(newZipCode);

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
        history.setZipCode(currentDetails.getAddress().getZipCode());
        history.setStreet(currentDetails.getAddress().getStreet());
        history.setNeighborhood(currentDetails.getAddress().getNeighborhood());
        history.setCity(currentDetails.getAddress().getCity());
        history.setState(currentDetails.getAddress().getState());
        history.setNumber(currentDetails.getNumber());
        history.setComplement(currentDetails.getComplement());
        history.setStartDate(currentDetails.getStartDate());
        history.setEndDate(now);

        addressHistoryRepository.save(history);

        log.debug("Archived address for customer {}: {}", customer.getId(),
                currentDetails.getAddress().getZipCode());
    }
}


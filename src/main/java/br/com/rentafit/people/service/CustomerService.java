package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.PersonAddressDetailsRepository;
import br.com.rentafit.people.repository.PersonAddressHistoryRepository;
import br.com.rentafit.people.util.ZipCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PersonAddressDetailsRepository addressDetailsRepository;
    private final PersonAddressHistoryRepository addressHistoryRepository;
    private final AddressService addressService;
    private final PeopleMapper peopleMapper;

    @Transactional(readOnly = true)
    public Page<CustomerDTO> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(peopleMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));
        return peopleMapper.toDTO(customer);
    }

    @Transactional
    public CustomerDTO create(CustomerDTO dto) {
        // Check for duplicate document
        if (dto.document() != null) {
            customerRepository.findByDocument(dto.document())
                    .ifPresent(existing -> {
                        throw new HttpClientErrorException(HttpStatusCode.valueOf(409),
                            "Customer with this document already exists");
                    });
        }

        Customer customer = new Customer();
        peopleMapper.updateBasicFields(customer, dto);

        // Handle address if provided
        if (dto.address() != null && dto.address().zipCode() != null) {
            handleAddressCreation(customer, dto);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Created customer with ID: {}", saved.getId());

        return peopleMapper.toDTO(saved);
    }

    @Transactional
    public CustomerDTO update(UUID id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));

        peopleMapper.updateBasicFields(customer, dto);

        // Handle address update if provided
        if (dto.address() != null && dto.address().zipCode() != null) {
            handleAddressUpdate(customer, dto);
        }

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer with ID: {}", updated.getId());

        return peopleMapper.toDTO(updated);
    }

    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Customer", id);
        }
        customerRepository.deleteById(id);
        log.info("Deleted customer with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<AddressHistoryDTO> getAddressHistory(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw ResourceNotFoundException.forId("Customer", customerId);
        }

        List<PersonAddressHistory> history = addressHistoryRepository
                .findByPersonIdOrderByStartDateDesc(customerId);

        return history.stream()
                .map(peopleMapper::toHistoryDTO)
                .toList();
    }

    /**
     * Handle address creation for a new customer
     */
    private void handleAddressCreation(Customer customer, CustomerDTO dto) {
        String normalizedZipCode = ZipCodeUtils.normalize(dto.address().zipCode());

        // Find or create address from ViaCEP
        Address address = addressService.findOrCreateByZipCode(normalizedZipCode);

        // Create PersonAddressDetails
        PersonAddressDetails details = new PersonAddressDetails();
        details.setPerson(customer);
        details.setAddress(address);
        details.setNumber(dto.number());
        details.setComplement(dto.complement());
        details.setStartDate(OffsetDateTime.now());
        details.setEndDate(null); // Current address

        customer.setCurrentAddress(details);

        log.debug("Created address details for customer with ZIP code: {}", normalizedZipCode);
    }

    /**
     * Handle address update - archives old address if changed
     */
    private void handleAddressUpdate(Customer customer, CustomerDTO dto) {
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
            Address newAddress = addressService.findOrCreateByZipCode(newZipCode);

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

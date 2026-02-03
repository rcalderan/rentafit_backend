package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.PersonAddressHistoryRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PersonAddressHistoryRepository addressHistoryRepository;
    private final AddressService addressService;
    private final PeopleMapper peopleMapper;

    @Transactional(readOnly = true)
    public Page<CustomerDetailsDTO> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(Customer::toDTO);
    }

    @Transactional(readOnly = true)
    public CustomerDetailsDTO findById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));
        return customer.toDTO();
    }

    @Transactional(readOnly = true)
    public CustomerDetailsDTO findByDocument(String document) {
        Customer customer = customerRepository.findByDocument(document)
                .orElseThrow(() ->new ResourceNotFoundException("Customer", "Document", document));
        return customer.toDTO();
    }

    @Transactional(readOnly = true)
    public CustomerDetailsDTO findByLegacyId(Integer legacyId) {
        try{
            Customer customer = customerRepository.findByLegacyId(legacyId)
                    .orElseThrow(() ->new ResourceNotFoundException("Customer", "LegacyId", legacyId));
            return customer.toDTO();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @Transactional
    public CustomerDetailsDTO create(CustomerDTO dto) {
        // Check for duplicate document
        if (dto.document() != null) {
            customerRepository.findByDocument(dto.document())
                    .ifPresent(existing -> {
                        throw new HttpClientErrorException(HttpStatusCode.valueOf(409),
                            "Customer with this document already exists");
                    });
        }

        Customer customer = new Customer(dto);

        Integer nextLegacyId = getNextLegacyId();
        customer.setLegacyId(nextLegacyId);


        if (dto.address() != null && dto.address().zipCode() != null) {
            handleAddressCreation(customer, dto);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Created customer with ID: {}", saved.getId());

        return saved.toDTO();
    }

    @Transactional
    public CustomerDetailsDTO update(CustomerDTO dto) {
        Customer customer = customerRepository.findById(dto.id())
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", dto.id()));

        customer.updateBasicFieldsFromDTO(dto);

        // Handle address update if provided
        if (dto.address() != null && dto.address().zipCode() != null) {
            addressService.handleAddressUpdate(customer, dto);
        }

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer with ID: {}", updated.getId());

        return updated.toDTO();
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

    @Transactional(readOnly = true)
    public Integer getNextLegacyId() {
        Integer maxLegacyId = customerRepository.findMaxLegacyId();
        if (maxLegacyId == null) {
            return 1;
        }
        return maxLegacyId + 1;
    }

    /**
     * Handle address creation for a new customer
     */
    private void handleAddressCreation(Customer customer, CustomerDTO dto) {

        // Find or create address from ViaCEP
        Address address = addressService.findOrCreateByAddress(dto.address());

        // Create PersonAddressDetails
        PersonAddressDetails details = new PersonAddressDetails();
        details.setPerson(customer);
        details.setAddress(address);
        details.setNumber(dto.number());
        details.setComplement(dto.complement());
        details.setStartDate(OffsetDateTime.now());
        details.setEndDate(null); // Current address

        customer.setCurrentAddress(details);

        log.debug("Created address details for customer with ZIP code: {}", address.getZipCode());
    }

}

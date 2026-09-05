package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.PersonAddressHistoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    public Page<CustomerDetailsDTO> findByName(String name, @Valid Pageable pageable) {
        if (name == null || name.isBlank()) {
            return findAll(pageable);
        }

        return customerRepository.findByNameContainingIgnoreCase(name.trim(), pageable)
                .map(Customer::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDetailsDTO> findByNamePrefix(String namePrefix, @Valid Pageable pageable) {
        if (namePrefix == null || namePrefix.isBlank()) {
            return findAll(pageable);
        }

        return customerRepository.findByNamePrefixIgnoreCase(namePrefix.trim(), pageable)
                .map(Customer::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDetailsDTO> search(String name, @Valid Pageable pageable) {
        return findByName(name, pageable);
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

        Customer customer = customerRepository.findByLegacyId(legacyId)
                .orElseThrow(() ->new ResourceNotFoundException("Customer", "LegacyId", legacyId));
        return customer.toDTO();
    }


    @Transactional
    public CustomerDetailsDTO create(CustomerDTO dto) {
        // Check for duplicate document
        if (dto.document() != null) {
            customerRepository.findByDocument(dto.document())
                    .ifPresent(existing -> {
                        throw new ValidationException("Customer with this document already exists");
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
    public CustomerDetailsDTO updateFromSignUp(Customer customer, SignUpRequestDTO dto) {
        customer.setEmail(dto.email().trim().toLowerCase());
        customer.setIsAuthenticated(true);

        // Merge phones: add new ones that are not already present
        List<String> currentPhones = customer.getPhones();
        if (currentPhones == null) {
            currentPhones = new ArrayList<>();
        }
        for (String phone : dto.phones()) {
            String normalized = normalizePhone(phone);
            if (normalized != null && !currentPhones.contains(normalized)) {
                currentPhones.add(normalized);
            }
        }
        customer.setPhones(currentPhones);

        // Update address if provided
        if (dto.address() != null && dto.address().zipCode() != null) {
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .name(customer.getName())
                    .email(customer.getEmail())
                    .document(customer.getDocument())
                    .phones(currentPhones)
                    .address(dto.address())
                    .number(dto.number())
                    .complement(dto.complement())
                    .isAuthenticated(true)
                    .build();
            addressService.handleAddressUpdate(customer, customerDTO);
        }

        Customer updated = customerRepository.save(customer);
        log.info("Merged legacy customer on sign-up: id={}", updated.getId());
        return updated.toDTO();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
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

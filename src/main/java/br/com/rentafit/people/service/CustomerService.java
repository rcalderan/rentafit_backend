package br.com.rentafit.people.service;

import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PeopleMapper peopleMapper;

    @Transactional(readOnly = true)
    public Page<CustomerDTO> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(peopleMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID id) {
        return customerRepository.findById(id)
                .map(peopleMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerDTO create(CustomerDTO dto) {
        Customer customer = new Customer();
        peopleMapper.updateFromDTO(customer, dto);
        return peopleMapper.toDTO(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDTO update(UUID id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        peopleMapper.updateFromDTO(customer, dto);
        return peopleMapper.toDTO(customerRepository.save(customer));
    }

    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }
}

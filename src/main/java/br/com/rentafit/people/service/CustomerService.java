package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

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
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));
    }

    @Transactional
    public CustomerDTO create(CustomerDTO dto) {
        Customer customer = new Customer();
        peopleMapper.updateFromDTO(customer, dto);

        Customer existingCustomer = customerRepository.findAll().stream()
                .filter(c -> c.getDocument().equals(customer.getDocument()))
                .findFirst()
                .orElse(null);
        if(existingCustomer!=null){
            throw new HttpClientErrorException(HttpStatusCode.valueOf(409), "Unprocessable Entity.");
        }

        return peopleMapper.toDTO(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDTO update(UUID id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));
        peopleMapper.updateFromDTO(customer, dto);
        return peopleMapper.toDTO(customerRepository.save(customer));
    }

    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Customer", id);
        }
        customerRepository.deleteById(id);
    }
}

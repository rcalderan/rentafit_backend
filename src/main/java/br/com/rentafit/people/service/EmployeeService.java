package br.com.rentafit.people.service;

import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PeopleMapper peopleMapper;

    @Transactional(readOnly = true)
    public Page<EmployeeDTO> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(peopleMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO findById(UUID id) {
        return employeeRepository.findById(id)
                .map(peopleMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        Employee employee = new Employee();
        peopleMapper.updateFromDTO(employee, dto);
        return peopleMapper.toDTO(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDTO update(UUID id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        peopleMapper.updateFromDTO(employee, dto);
        return peopleMapper.toDTO(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }
}

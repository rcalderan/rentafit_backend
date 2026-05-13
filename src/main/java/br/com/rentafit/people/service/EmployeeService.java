package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.EmployeeAuthResponseDTO;
import br.com.rentafit.people.dto.EmployeeCheckRequestDTO;
import br.com.rentafit.people.dto.EmployeeCheckResponseDTO;
import br.com.rentafit.people.dto.EmployeeCreateRequestDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PeopleMapper peopleMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<EmployeeDTO> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(peopleMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO findById(UUID id) {
        return employeeRepository.findById(id)
                .map(peopleMapper::toDTO)
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", id));
    }

    @Transactional
    public EmployeeCheckResponseDTO create(EmployeeCreateRequestDTO dto) {
        String normalizedInitials = normalizeInitials(dto.initials());

        Employee employee = new Employee();
        employee.setName(dto.name());
        employee.setDocument(dto.document());
        employee.setEmail(dto.email());
        employee.setInitials(normalizedInitials);
        employee.setRoleLevel(dto.roleLevel());

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);
        createUserAccount(savedEmployee, dto.pin(), normalizedInitials);

        return toCheckResponseDTO(savedEmployee);
    }

    @Transactional
    public EmployeeDTO update(UUID id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", id));
        peopleMapper.updateFromDTO(employee, dto);
        employee.setInitials(normalizeInitials(employee.getInitials()));
        return peopleMapper.toDTO(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeCheckResponseDTO findByInitials(String initials) {
        String normalizedInitials = normalizeInitials(initials);
        Employee employee = employeeRepository.findByInitials(normalizedInitials)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "Initials", normalizedInitials));
        return toCheckResponseDTO(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeCheckResponseDTO check(EmployeeCheckRequestDTO dto) {
        String normalizedInitials = normalizeInitials(dto.initials());
        Employee employee = employeeRepository.findByInitials(normalizedInitials)
                .orElseThrow(() -> new ValidationException("Credenciais inválidas. Verifique as iniciais e o PIN."));

        UserAccount account = userAccountRepository.findById(employee.getId())
                .orElseThrow(() -> new ValidationException("Credenciais inválidas. Verifique as iniciais e o PIN."));

        if (account.getPin() == null || !account.getPin().equals(dto.pin())) {
            throw new ValidationException("Credenciais inválidas. Verifique as iniciais e o PIN.");
        }

        return new EmployeeCheckResponseDTO(employee.getId(), employee.getInitials(), employee.getName());
    }

    @Transactional
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Employee", id);
        }
        employeeRepository.deleteById(id);
    }

    private void createUserAccount(Employee employee, String pin, String normalizedInitials) {
        Role employeeRole = roleRepository.findByRole(RoleName.EMPLOYEE)
                .orElseThrow(() -> new ValidationException("EMPLOYEE role not configured"));

        String randomPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        entityManager.createNativeQuery(
                        "INSERT INTO user_accounts (id, username, password, pin, is_active) VALUES (:id, :username, :password, :pin, :active)")
                .setParameter("id", employee.getId())
                .setParameter("username", normalizedInitials)
                .setParameter("password", randomPasswordHash)
                .setParameter("pin", pin)
                .setParameter("active", true)
                .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)")
                .setParameter("userId", employee.getId())
                .setParameter("roleId", employeeRole.getId())
                .executeUpdate();
    }
    private EmployeeCheckResponseDTO toCheckResponseDTO(Employee employee) {
        return EmployeeCheckResponseDTO.builder()
                .id(employee.getId())
                .initials(employee.getInitials())
                .name(employee.getName())
                .build();
    }

    private EmployeeAuthResponseDTO toAuthResponseDTO(Employee employee) {
        return EmployeeAuthResponseDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .document(employee.getDocument())
                .email(employee.getEmail())
                .initials(employee.getInitials())
                .roleLevel(employee.getRoleLevel())
                .build();
    }

    private String normalizeInitials(String initials) {
        if (initials == null) {
            return null;
        }
        return initials.trim().toUpperCase(Locale.ROOT);
    }
}

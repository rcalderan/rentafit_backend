package br.com.rentafit.migration.processor;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.domain.UserRole;
import br.com.rentafit.migration.dto.FuncionarioDocument;
import br.com.rentafit.migration.util.LegacyIdMapper;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.common.security.DatabaseEncryptionConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Processor que transforma documento MongoDB Funcionário → Domain Entities Employee + UserAccount
 *
 * Responsabilidades:
 * 1. Mapear campos de FuncionarioDocument para Employee entity
 * 2. Gerar novo UUID mantendo legacy_id
 * 3. Criptografar campo de documento (CPF)
 * 4. Hash da senha com BCrypt
 * 5. Mapear nivel_acesso → role (ROLE_ADMIN, ROLE_EMPLOYEE, etc)
 */
@Component
@RequiredArgsConstructor
public class FuncionarioItemProcessor implements ItemProcessor<FuncionarioDocument, Employee> {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioItemProcessor.class);

    private final LegacyIdMapper legacyIdMapper;
    private final DatabaseEncryptionConverter encryptionConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Employee process(FuncionarioDocument item) throws Exception {
        log.debug("Processing funcionario: {}", item.getNome());

        try {
            // Gerar novo UUID
            UUID newUuid = UUID.randomUUID();

            // Extrair legacy_id do ObjectId
            Integer legacyId = legacyIdMapper.extractLegacyId(item.getId());

            // Criar entidade Employee (Person)
            Employee employee = new Employee();
            employee.setId(newUuid);
            employee.setLegacyId(legacyId);
            employee.setName(item.getNome());

            // Criptografar documento (CPF)
            if (item.getDocumento() != null) {
                String encryptedDocument = encryptionConverter.convertToDatabaseColumn(
                    item.getDocumento()
                );
                employee.setDocument(encryptedDocument);
            }

            employee.setEmail(item.getEmail());
            employee.setCreatedAt(item.getDataCriacao() != null ?
                item.getDataCriacao() : OffsetDateTime.now());
            employee.setUpdatedAt(item.getDataAtualizacao() != null ?
                item.getDataAtualizacao() : OffsetDateTime.now());

            // Campos específicos de Employee
            employee.setInitials(item.getIniciais());
            employee.setRoleLevel(item.getNivelAcesso() != null ?
                item.getNivelAcesso() : 3); // Default: EMPLOYEE

            log.debug("Successfully processed funcionario: {} (legacy_id: {})",
                item.getNome(), legacyId);

            return employee;

        } catch (Exception e) {
            log.error("Error processing funcionario: {}", item.getNome(), e);
            throw e;
        }
    }

    /**
     * Transforma um FuncionarioDocument em UserAccount (entidade de autenticação)
     * Este método é chamado separadamente do processor padrão pois UserAccount
     * é armazenado em tabela separada
     */
    public UserAccount processToUserAccount(FuncionarioDocument item, UUID employeeId) throws Exception {
        log.debug("Processing funcionario to UserAccount: {}", item.getUsuario());

        try {
            UserAccount account = new UserAccount();
            account.setId(employeeId); // FK para Employee (Person)
            account.setUsername(item.getUsuario());

            // Hash da senha com BCrypt
            if (item.getSenha() != null && !item.getSenha().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(item.getSenha());
                account.setPassword(hashedPassword);
            } else {
                log.warn("No password provided for user: {}", item.getUsuario());
                account.setPassword("");
            }

            // Mapear nivel_acesso → UserRole
            UserRole role = mapRoleFromLevel(item.getNivelAcesso());
            account.setRole(role);

            // Status do usuário
            account.setIsActive(item.getAtivo() != null ?
                item.getAtivo() : true);

            log.debug("Successfully processed funcionario to UserAccount: {} (role: {})",
                item.getUsuario(), role);

            return account;

        } catch (Exception e) {
            log.error("Error processing funcionario to UserAccount: {}", item.getUsuario(), e);
            throw e;
        }
    }

    /**
     * Mapeia nivel_acesso (Integer) para UserRole (Enum)
     *
     * Mapeamento:
     * 1 → ROLE_ADMIN (administrador)
     * 2 → ROLE_MANAGER (gerente)
     * 3+ → ROLE_EMPLOYEE (funcionário)
     */
    private UserRole mapRoleFromLevel(Integer nivelAcesso) {
        if (nivelAcesso == null) {
            return UserRole.ROLE_EMPLOYEE;
        }

        return switch (nivelAcesso) {
            case 1 -> UserRole.ROLE_ADMIN;
            case 2 -> UserRole.ROLE_MANAGER;
            default -> UserRole.ROLE_EMPLOYEE;
        };
    }
}


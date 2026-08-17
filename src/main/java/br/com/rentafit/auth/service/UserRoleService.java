package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.UserSummaryDTO;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Handles role elevation/demotion enforcing the hierarchy ADMIN > MANAGER > EMPLOYEE > CUSTOMER.
 *
 * Rules (actor = authenticated user performing the change):
 *  - Nobody can change their own role.
 *  - ADMIN can set any role on any user.
 *  - MANAGER cannot assign ADMIN, and cannot modify users whose current role is MANAGER or ADMIN.
 *
 * Usage: {@code userRoleService.setRole(actor, targetId, RoleName.EMPLOYEE)}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleService {

    private static final Map<RoleName, Integer> RANK = Map.of(
            RoleName.ADMIN, 4,
            RoleName.MANAGER, 3,
            RoleName.EMPLOYEE, 2,
            RoleName.CUSTOMER, 1
    );

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> listUsers(Pageable pageable) {
        return userAccountRepository.findAllWithDetails(pageable).map(UserSummaryDTO::fromEntity);
    }

    @Transactional
    public UserSummaryDTO setRole(UserAccount actor, UUID targetUserId, RoleName newRole) {
        return setRole(actor, targetUserId, newRole, null, null);
    }

    /**
     * Changes a user's role. When elevating to EMPLOYEE or MANAGER, ensures an Employee row
     * exists for the Person (creating one with the provided initials when missing).
     *
     * Usage: {@code userRoleService.setRole(actor, targetId, RoleName.EMPLOYEE, "JD", 1)}
     */
    @Transactional
    public UserSummaryDTO setRole(UserAccount actor, UUID targetUserId, RoleName newRole,
                                   String initials, Integer roleLevel) {
        if (actor == null) {
            throw new ValidationException("Authenticated actor is required to change roles");
        }
        if (actor.getId().equals(targetUserId)) {
            throw new ValidationException("You cannot change your own role");
        }

        RoleName actorRole = actor.getRole();
        UserAccount target = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.forId("UserAccount", targetUserId));
        RoleName targetCurrentRole = target.getRole();

        validateChangeAllowed(actorRole, targetCurrentRole, newRole);

        if (newRole == RoleName.EMPLOYEE || newRole == RoleName.MANAGER) {
            employeeService.ensureEmployeeForPerson(targetUserId, initials, roleLevel);
        }

        applyRole(target, newRole);
        UserAccount saved = userAccountRepository.save(target);
        log.info("User {} role changed from {} to {} by actor {}",
                targetUserId, targetCurrentRole, newRole, actor.getId());
        return UserSummaryDTO.fromEntity(saved);
    }

    private void validateChangeAllowed(RoleName actorRole, RoleName targetCurrentRole, RoleName newRole) {
        if (actorRole == RoleName.ADMIN) {
            return;
        }
        if (actorRole != RoleName.MANAGER) {
            throw new ValidationException(
                    "Actor role '" + actorRole + "' is not allowed to change roles (requires ADMIN or MANAGER)");
        }
        // MANAGER constraints
        if (newRole == RoleName.ADMIN) {
            throw new ValidationException("MANAGER cannot assign role ADMIN");
        }
        int managerRank = RANK.get(RoleName.MANAGER);
        if (rankOf(targetCurrentRole) >= managerRank) {
            throw new ValidationException(
                    "MANAGER cannot modify a user whose current role is '" + targetCurrentRole + "'");
        }
    }

    private int rankOf(RoleName role) {
        if (role == null || !RANK.containsKey(role)) {
            throw new ValidationException("Unknown role: " + role);
        }
        return RANK.get(role);
    }

    private void applyRole(UserAccount target, RoleName newRole) {
        Role role = roleRepository.findByRole(newRole)
                .orElseThrow(() -> new ValidationException("Role '" + newRole + "' is not configured"));
        target.setRoles(new ArrayList<>(java.util.List.of(role)));
    }
}

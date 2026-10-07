package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.*;
import br.com.rentafit.auth.repository.InstallationStateRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstallationCompletionServiceTest {
    private final InstallationStateRepository installations = mock(InstallationStateRepository.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final RefreshTokenService tokens = mock(RefreshTokenService.class);
    private final UserAccountService credentials = mock(UserAccountService.class);
    private final OperatorIdentityService operators = mock(OperatorIdentityService.class);
    private final InstallationCompletionService completion = new InstallationCompletionService(installations, accounts, tokens, credentials, operators);
    private InstallationState state;
    private UserAccount bootstrap;
    private UserAccount successor;

    @BeforeEach
    void setup() {
        state = new InstallationState(); state.setId(1); state.setStatus("PENDING");
        bootstrap = account(UUID.randomUUID(), RoleName.ADMIN);
        successor = account(UUID.randomUUID(), RoleName.ADMIN);
        state.setBootstrapAccountId(bootstrap.getId()); state.setBootstrapUsername("admin");
        lenient().when(installations.lockInstallation()).thenReturn(Optional.of(state));
        lenient().when(accounts.lockById(successor.getId())).thenReturn(Optional.of(successor));
        lenient().when(accounts.lockById(bootstrap.getId())).thenReturn(Optional.of(bootstrap));
        lenient().when(credentials.getPasswordExpiryDays()).thenReturn(90L);
        lenient().when(operators.profile(successor.getUsername())).thenReturn(new br.com.rentafit.auth.dto.OperatorProfileResponseDTO(
                new br.com.rentafit.auth.dto.UserProfileResponseDTO(successor, 90), "OP"));
    }

    @Test
    void removesOnlyBootstrapAfterVerifiedAdministratorLoginAndIsIdempotent() {
        completion.afterVerifiedLogin(successor);
        completion.afterVerifiedLogin(successor);
        verify(accounts).delete(bootstrap);
        verify(tokens).deleteByUserId(bootstrap.getId());
        assertThat(state.getStatus()).isEqualTo("COMPLETED");
        assertThat(state.getCompletedByAccountId()).isEqualTo(successor.getId());
        assertThat(state.getCompletedAt()).isNotNull();
    }

    @Test
    void bootstrapCustomerLegacyAndIncompleteCredentialsNeverRetireAccount() {
        completion.afterVerifiedLogin(bootstrap);
        completion.afterVerifiedLogin(account(UUID.randomUUID(), RoleName.CUSTOMER));
        successor.setPin(null); completion.afterVerifiedLogin(successor);
        successor.setPin("test-hash"); successor.setPasswordChangedAt(OffsetDateTime.now().minusDays(91));
        completion.afterVerifiedLogin(successor);
        state.setStatus("LEGACY"); completion.afterVerifiedLogin(successor);
        verify(accounts, never()).delete(any()); verifyNoInteractions(tokens);
    }

    @Test
    void invalidOperationalProfileAndChangedRoleDoNotRetireBootstrap() {
        when(operators.profile(successor.getUsername())).thenThrow(new org.springframework.security.access.AccessDeniedException("No employee"));
        completion.afterVerifiedLogin(successor);
        successor.setIsActive(false); completion.afterVerifiedLogin(successor);
        verify(accounts, never()).delete(any());
    }

    @Test
    void persistenceFailureIsPropagatedAndNoCompletionIsReported() {
        doThrow(new IllegalStateException("delete failed")).when(accounts).delete(bootstrap);
        assertThatThrownBy(() -> completion.afterVerifiedLogin(successor)).hasMessage("delete failed");
        assertThat(state.getStatus()).isEqualTo("PENDING");
    }

    private UserAccount account(UUID id, RoleName roleName) {
        UserAccount account = new UserAccount(); account.setId(id); account.setUsername(id.toString());
        account.setPin("test-hash"); account.setPasswordChangedAt(OffsetDateTime.now());
        Role role = new Role(); role.setRole(roleName); account.setRoles(List.of(role));
        return account;
    }
}

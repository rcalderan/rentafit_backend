package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.FailedLoginOutcome;
import br.com.rentafit.auth.service.LoginAttemptTracker;
import br.com.rentafit.auth.service.LoginSessionIssuer;
import br.com.rentafit.auth.service.PasswordLoginService;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.auth.service.UserAccountService;
import br.com.rentafit.common.security.CryptoService;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AuthLoginTransactionTest.TestConfig.class)
class AuthLoginTransactionTest {
    @Test
    void rejectedAuthenticationDoesNotCauseUnexpectedRollback(org.springframework.context.ApplicationContext context) {
        var controller = context.getBean(AuthController.class);

        assertThatThrownBy(() -> controller.login(new LoginRequestDTO("missing-user", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    static class RejectingAuthenticationManager implements AuthenticationManager {
        @Override
        @Transactional(readOnly = true)
        public Authentication authenticate(Authentication authentication) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            throw new BadCredentialsException("Credenciais inválidas.");
        }
    }

    @TestConfiguration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return new RejectingAuthenticationManager();
        }

        @Bean
        AuthController authController(AuthenticationManager authenticationManager) {
            LoginAttemptTracker tracker = mock(LoginAttemptTracker.class);
            when(tracker.recordFailure("missing-user")).thenReturn(new FailedLoginOutcome(4, null));
            var loginService = new PasswordLoginService(authenticationManager, tracker,
                    mock(LoginSessionIssuer.class), mock(UserAccountRepository.class));
            return new AuthController(loginService, mock(TokenService.class), mock(RefreshTokenService.class),
                    mock(CryptoService.class), mock(UserAccountService.class));
        }
    }
}

package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.common.security.TokenService;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService")
class UserRegistrationServiceTest {

    @Mock
    CustomerService customerService;

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    EntityManager entityManager;

    @Mock
    TokenService tokenService;

    @Mock
    RefreshTokenService refreshTokenService;

    @InjectMocks
    UserRegistrationService service;

    UUID customerId;
    CustomerDetailsDTO customerDetails;
    Role customerRole;
    UserAccount account;
    RefreshToken refreshToken;
    Query nativeQuery;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerDetails = mock(CustomerDetailsDTO.class);
        lenient().when(customerDetails.id()).thenReturn(customerId);

        customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setRole(RoleName.CUSTOMER);

        account = new UserAccount();
        account.setId(customerId);
        account.setUsername("joao@test.com");
        account.setRoles(List.of(customerRole));

        refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-xyz");
        refreshToken.setUserAccount(account);

        nativeQuery = mock(Query.class);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("registerCustomer cria conta e retorna tokens com sucesso")
    void registerCustomer_sucesso() {
        SignUpRequestDTO dto = buildSignUpRequest("joao@test.com");

        when(userAccountRepository.findByUsername("joao@test.com")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(userAccountRepository.findById(customerId)).thenReturn(Optional.of(account));
        when(tokenService.generateToken("joao@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        var result = service.registerCustomer(dto);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-xyz");
    }

    @Test
    @DisplayName("registerCustomer lança ValidationException quando email já cadastrado")
    void registerCustomer_emailJaCadastrado() {
        SignUpRequestDTO dto = buildSignUpRequest("joao@test.com");

        when(userAccountRepository.findByUsername("joao@test.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("joao@test.com");
    }

    @Test
    @DisplayName("registerCustomer lança ValidationException quando role CUSTOMER não configurada")
    void registerCustomer_roleNaoConfigurada() {
        SignUpRequestDTO dto = buildSignUpRequest("joao@test.com");

        when(userAccountRepository.findByUsername("joao@test.com")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CUSTOMER role not configured");
    }

    @Test
    @DisplayName("registerCustomer lança ValidationException quando conta não encontrada após flush")
    void registerCustomer_contaNaoEncontradaAposFlush() {
        SignUpRequestDTO dto = buildSignUpRequest("joao@test.com");

        when(userAccountRepository.findByUsername("joao@test.com")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(userAccountRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User account not found after creation");
    }

    private SignUpRequestDTO buildSignUpRequest(String email) {
        AddressDTO address = AddressDTO.builder()
                .zipCode("01310-100")
                .street("Avenida Paulista")
                .city("São Paulo")
                .state("SP")
                .build();

        return new SignUpRequestDTO(
                "João Silva",
                email,
                "123.456.789-09",
                List.of("(11) 99999-9999"),
                address,
                "1000",
                "Apt 201"
        );
    }
}

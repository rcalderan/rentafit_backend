package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.AccountLinkIssuer;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import br.com.rentafit.people.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService")
class UserRegistrationServiceTest {

    @Mock
    CustomerService customerService;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    EntityManager entityManager;

    @Mock
    AccountLinkIssuer accountLinkIssuer;

    @InjectMocks
    UserRegistrationService service;

    UUID customerId;
    Customer customer;
    CustomerDetailsDTO customerDetails;
    Role customerRole;
    UserAccount account;
    Query nativeQuery;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerDetails = mock(CustomerDetailsDTO.class);
        lenient().when(customerDetails.id()).thenReturn(customerId);

        customer = new Customer();
        customer.setId(customerId);
        customer.setName("João Silva");
        customer.setDocument("12345678909");
        customer.setEmail("joao@test.com");

        customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setRole(RoleName.CUSTOMER);

        account = new UserAccount();
        account.setId(customerId);
        account.setUsername("joao.silva");
        account.setRoles(List.of(customerRole));

        nativeQuery = mock(Query.class);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);
    }

    private void stubAccountCreation() {
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(userAccountRepository.findById(customerId)).thenReturn(Optional.of(account));
    }

    @Test
    @DisplayName("cria conta com username escolhido, pendente de e-mail, e envia link de ativação sem retornar tokens")
    void registerCustomer_sucesso() {
        SignUpRequestDTO dto = buildSignUpRequest("  Joao.Silva ", "Joao@Test.com");
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        stubAccountCreation();

        var result = service.registerCustomer(dto);

        assertThat(result.maskedEmail()).isEqualTo("j***@test.com");
        verify(nativeQuery).setParameter("username", "joao.silva");
        verify(entityManager).createNativeQuery(contains("email_verification_pending"));
        verify(accountLinkIssuer).issue(account, AccountTokenPurpose.ACTIVATION);
        verify(customerService, never()).updateFromSignUp(any(), any());
    }

    @Test
    @DisplayName("username não é mais derivado do e-mail")
    void registerCustomer_usernameIndependenteDoEmail() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "joao@test.com");
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        stubAccountCreation();

        service.registerCustomer(dto);

        ArgumentCaptor<CustomerDTO> captor = ArgumentCaptor.forClass(CustomerDTO.class);
        verify(customerService).create(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("joao@test.com");
        verify(nativeQuery, never()).setParameter("username", "joao@test.com");
    }

    @Test
    @DisplayName("atualiza cliente legado com mesmo documento")
    void registerCustomer_clienteLegadoAtualizaDados() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "novo-email@test.com");
        CustomerDetailsDTO updatedDetails = mock(CustomerDetailsDTO.class);
        when(updatedDetails.id()).thenReturn(customerId);
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.of(customer));
        when(customerService.updateFromSignUp(customer, dto)).thenReturn(updatedDetails);
        stubAccountCreation();

        service.registerCustomer(dto);

        verify(customerService).updateFromSignUp(customer, dto);
        verify(accountLinkIssuer).issue(account, AccountTokenPurpose.ACTIVATION);
    }

    @Test
    @DisplayName("rejeita username já usado por outra conta, sem diferenciar maiúsculas")
    void registerCustomer_usernameEmUso() {
        SignUpRequestDTO dto = buildSignUpRequest("Joao.Silva", "joao@test.com");
        UserAccount other = new UserAccount();
        other.setId(UUID.randomUUID());
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameIgnoreCase("joao.silva")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("joao.silva");
        verify(customerService, never()).create(any());
    }

    @Test
    @DisplayName("rejeita e-mail já cadastrado por outra pessoa no merge de legado")
    void registerCustomer_emailJaCadastradoPorOutraPessoa() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "joao@test.com");
        UserAccount otherAccount = new UserAccount();
        otherAccount.setId(UUID.randomUUID());
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.of(customer));
        when(userAccountRepository.findAllByPersonEmail("joao@test.com")).thenReturn(List.of(otherAccount));

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("joao@test.com");
    }

    @Test
    @DisplayName("rejeita e-mail já cadastrado sem cliente legado")
    void registerCustomer_emailJaCadastrado() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "joao@test.com");
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(userAccountRepository.findAllByPersonEmail("joao@test.com")).thenReturn(List.of(account));

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("joao@test.com");
    }

    @Test
    @DisplayName("lança ValidationException quando role CUSTOMER não configurada")
    void registerCustomer_roleNaoConfigurada() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "joao@test.com");
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CUSTOMER role not configured");
    }

    @Test
    @DisplayName("lança ValidationException quando conta não encontrada após flush")
    void registerCustomer_contaNaoEncontradaAposFlush() {
        SignUpRequestDTO dto = buildSignUpRequest("joao.silva", "joao@test.com");
        when(customerRepository.findByDocument("12345678909")).thenReturn(Optional.empty());
        when(customerService.create(any())).thenReturn(customerDetails);
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(userAccountRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerCustomer(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User account not found after creation");
        verify(accountLinkIssuer, never()).issue(any(), any());
    }

    private SignUpRequestDTO buildSignUpRequest(String username, String email) {
        AddressDTO address = AddressDTO.builder()
                .zipCode("01310-100")
                .street("Avenida Paulista")
                .city("São Paulo")
                .state("SP")
                .build();

        return new SignUpRequestDTO(
                "João Silva",
                username,
                email,
                "123.456.789-09",
                List.of("(11) 99999-9999"),
                address,
                "1000",
                "Apt 201"
        );
    }
}

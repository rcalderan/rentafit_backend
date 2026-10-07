package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.OperatorLoginResponseDTO;
import br.com.rentafit.auth.dto.OperatorProfileResponseDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.OperatorAuthenticationService;
import br.com.rentafit.auth.service.OperatorIdentityService;
import br.com.rentafit.common.exception.GlobalExceptionHandler;
import br.com.rentafit.common.security.SecurityConfig;
import br.com.rentafit.common.security.SecurityFilter;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.ActiveProfiles("test")
@WebMvcTest(OperatorAuthenticationController.class)
@org.springframework.test.context.ContextConfiguration(classes = OperatorAuthenticationController.class)
@Import({SecurityConfig.class, SecurityFilter.class, GlobalExceptionHandler.class})
class OperatorAuthenticationControllerTest {
    @MockitoBean private OperatorAuthenticationService operatorAuthenticationService;
    @MockitoBean private OperatorIdentityService operatorIdentityService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean(name = "corsConfigurationSource") private CorsConfigurationSource corsConfigurationSource;

    @Test
    void loginIsPublicButCandidateMustBeApproved(@org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        var profile = new OperatorProfileResponseDTO(new UserProfileResponseDTO(new UserAccount()), "OP");
        when(operatorAuthenticationService.login(any()))
                .thenReturn(new OperatorLoginResponseDTO("test-access", "test-refresh", "Bearer", profile));

        mvc.perform(post("/api/auth/operator-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("profile.initials").value("OP"))
                .andExpect(jsonPath("profile.user.active").value(true));
    }

    @Test
    void deniedCandidateReturnsNoTokens(@org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        when(operatorAuthenticationService.login(any())).thenThrow(new AccessDeniedException("Internal detail"));

        mvc.perform(post("/api/auth/operator-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer\",\"password\":\"test-password\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("accessToken").doesNotExist())
                .andExpect(jsonPath("message").value("Credenciais inválidas ou usuário não habilitado para esta operação."));
    }

    @Test
    void internalAuthenticationFailureReturnsGenericServerError(@org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        when(operatorAuthenticationService.login(any()))
                .thenThrow(new org.springframework.security.authentication.AuthenticationServiceException("Internal secret detail"));
        mvc.perform(post("/api/auth/operator-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"test-password\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Não foi possível concluir a autenticação."))
                .andExpect(jsonPath("accessToken").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPLOYEE", "MANAGER", "ADMIN"})
    void operationalUserCanReadOwnProfile(String role, @org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        when(operatorIdentityService.profile("operator"))
                .thenReturn(new OperatorProfileResponseDTO(new UserProfileResponseDTO(new UserAccount()), "OP"));

        mvc.perform(get("/api/auth/operator-profile").with(user("operator").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("initials").value("OP"));
        verify(operatorIdentityService).profile("operator");
    }

    @Test
    void customerCannotReadOperatorProfile(@org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/api/auth/operator-profile").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(operatorIdentityService);
    }

    @Test
    void anonymousCannotReadOperatorProfile(@org.springframework.beans.factory.annotation.Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/api/auth/operator-profile")).andExpect(status().isForbidden());
        verifyNoInteractions(operatorIdentityService);
    }
}

package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.GlobalExceptionHandler;
import br.com.rentafit.common.security.*;
import br.com.rentafit.settings.controller.ApplicationSettingsController;
import br.com.rentafit.settings.dto.SettingEntry;
import br.com.rentafit.settings.service.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationSettingsController.class)
@ContextConfiguration(classes = ApplicationSettingsController.class)
@Import({SecurityConfig.class, SecurityFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ApplicationSettingsAccessTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ApplicationSettingsService settings;
    @MockitoBean private TokenService tokens;
    @MockitoBean private UserAccountRepository accounts;
    @MockitoBean(name = "corsConfigurationSource") private CorsConfigurationSource cors;

    @Test
    void employeeReadsButCannotWriteAndCustomerCannotRead() throws Exception {
        when(settings.get("rental.conflictWindowDays")).thenReturn(new SettingEntry("rental.conflictWindowDays", "2"));
        mvc.perform(get("/api/v1/settings/rental.conflictWindowDays").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("value").value("2"));
        mvc.perform(put("/api/v1/settings/rental.conflictWindowDays").with(user("employee").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"3\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/settings/rental.conflictWindowDays").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verify(settings, never()).put(anyString(), anyString());
    }

    @Test
    void managerCanPersistWindow() throws Exception {
        mvc.perform(put("/api/v1/settings/rental.conflictWindowDays").with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"0\"}"))
                .andExpect(status().isOk());
        verify(settings).put("rental.conflictWindowDays", "0");
    }
}

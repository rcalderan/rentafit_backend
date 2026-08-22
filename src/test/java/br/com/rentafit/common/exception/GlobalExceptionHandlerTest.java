package br.com.rentafit.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler - mapeamento de exceções")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("ResourceNotFoundException retorna 404")
    void handleResourceNotFound_retorna404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.path", is("/test/not-found")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("IllegalArgumentException retorna 400")
    void handleIllegalArgument_retorna400() throws Exception {
        mockMvc.perform(get("/test/invalid-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/test/invalid-argument")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("MethodArgumentTypeMismatchException retorna 400")
    void handleMethodArgumentTypeMismatch_retorna400() throws Exception {
        mockMvc.perform(get("/test/type-mismatch").param("id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/test/type-mismatch")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("RuntimeException generica retorna 500")
    void handleInternal_retorna500() throws Exception {
        mockMvc.perform(get("/test/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.path", is("/test/internal")));
    }
}

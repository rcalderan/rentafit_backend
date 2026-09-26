package br.com.rentafit.printtemplate.controller;

import br.com.rentafit.printtemplate.dto.PrintTemplateRequest;
import br.com.rentafit.printtemplate.dto.PrintTemplateResponse;
import br.com.rentafit.printtemplate.service.PrintTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintTemplateControllerTest {

    @Mock
    private PrintTemplateService service;

    @InjectMocks
    private PrintTemplateController controller;

    @Test
    void listsTemplates() {
        when(service.findAll()).thenReturn(List.of(response()));

        var result = controller.findAll();

        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().getFirst().id()).isEqualTo("contract");
    }

    @Test
    void findsTemplateById() {
        when(service.findById("contract")).thenReturn(response());

        var result = controller.findById("contract");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().id()).isEqualTo("contract");
    }

    @Test
    void returnsCreatedWhenPutAddsTemplate() {
        when(service.existsById("contract")).thenReturn(false);
        when(service.save(any(), any())).thenReturn(response());

        var result = controller.save("contract", request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void returnsOkWhenPutReplacesTemplate() {
        when(service.existsById("contract")).thenReturn(true);
        when(service.save(any(), any())).thenReturn(response());

        var result = controller.save("contract", request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deletesTemplateAndReturnsNoContent() {
        var result = controller.delete("contract");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).delete("contract");
    }

    private PrintTemplateRequest request() {
        return new PrintTemplateRequest(
                "Contrato", null, "RENTAL_CONTRACT", "A4", "PORTRAIT",
                new BigDecimal("210"), new BigDecimal("297"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, "<p>Documento</p>", null, true, true
        );
    }

    private PrintTemplateResponse response() {
        return new PrintTemplateResponse(
                "contract", "Contrato", null, "RENTAL_CONTRACT", "A4", "PORTRAIT",
                new BigDecimal("210"), new BigDecimal("297"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                "<p>Documento</p>", null, true, true, null, null
        );
    }
}

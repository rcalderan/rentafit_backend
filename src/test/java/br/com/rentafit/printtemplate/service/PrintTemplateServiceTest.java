package br.com.rentafit.printtemplate.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.printtemplate.domain.PrintTemplate;
import br.com.rentafit.printtemplate.dto.PrintTemplateRequest;
import br.com.rentafit.printtemplate.repository.PrintTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintTemplateServiceTest {

    @Mock
    private PrintTemplateRepository repository;

    @InjectMocks
    private PrintTemplateService service;

    @Test
    void listsTemplatesByName() {
        var first = template("A Contrato");
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(first));

        var result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("A Contrato");
        verify(repository).findAllByOrderByNameAsc();
    }

    @Test
    void checksTemplateExistence() {
        when(repository.existsById("contract")).thenReturn(true);

        assertThat(service.existsById("contract")).isTrue();
    }

    @Test
    void findsTemplateById() {
        when(repository.findById("contract")).thenReturn(Optional.of(template("Contrato")));

        assertThat(service.findById("contract").name()).isEqualTo("Contrato");
    }

    @Test
    void throwsWhenTemplateIdDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void createsTemplateWithProvidedIdAndMapsFields() {
        when(repository.findById("custom-id")).thenReturn(Optional.empty());
        when(repository.save(any(PrintTemplate.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.save("custom-id", request("Novo", false, true));

        assertThat(result.id()).isEqualTo("custom-id");
        assertThat(result.name()).isEqualTo("Novo");
        assertThat(result.pageWidthMm()).isEqualByComparingTo("210");
        assertThat(result.contentHtml()).isEqualTo("<p>Documento</p>");
    }

    @Test
    void replacesExistingTemplateAndClearsOtherActiveDefault() {
        var existing = template("Original");
        existing.setId("default-new");
        when(repository.findById("default-new")).thenReturn(Optional.of(existing));
        var oldDefault = template("Old default");
        oldDefault.setDefault(true);
        when(repository.findAllByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIdNot(
                "RENTAL_CONTRACT", "default-new")).thenReturn(List.of(oldDefault));
        when(repository.save(any(PrintTemplate.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.save("default-new", request("Contrato Atualizado", true, true));

        assertThat(result.isDefault()).isTrue();
        assertThat(result.name()).isEqualTo("Contrato Atualizado");
        assertThat(oldDefault.isDefault()).isFalse();
        verify(repository).saveAllAndFlush(any());
    }

    @Test
    void doesNotClearDefaultWhenNewTemplateIsInactive() {
        when(repository.findById("inactive")).thenReturn(Optional.empty());
        when(repository.save(any(PrintTemplate.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.save("inactive", request("Inativo", true, false));

        assertThat(result.isDefault()).isTrue();
        verify(repository, org.mockito.Mockito.never())
                .findAllByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIdNot(any(), any());
    }

    @Test
    void deletesExistingTemplate() {
        var existing = template("Contrato");
        when(repository.findById("contract")).thenReturn(Optional.of(existing));

        service.delete("contract");

        verify(repository).delete(existing);
    }

    @Test
    void throwsWhenDeletingMissingTemplate() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PrintTemplate template(String name) {
        var template = new PrintTemplate();
        template.setId("contract");
        template.setName(name);
        template.setTemplateType("RENTAL_CONTRACT");
        template.setPageFormat("A4");
        template.setOrientation("PORTRAIT");
        template.setPageWidthMm(new BigDecimal("210"));
        template.setMarginTopMm(BigDecimal.ZERO);
        template.setMarginBottomMm(BigDecimal.ZERO);
        template.setMarginLeftMm(BigDecimal.ZERO);
        template.setMarginRightMm(BigDecimal.ZERO);
        template.setPrintOffsetMm(BigDecimal.ZERO);
        template.setContentHtml("<p>Documento</p>");
        template.setActive(true);
        return template;
    }

    private PrintTemplateRequest request(String name, boolean isDefault, boolean isActive) {
        return new PrintTemplateRequest(
                name, null, "RENTAL_CONTRACT", "A4", "PORTRAIT",
                new BigDecimal("210"), new BigDecimal("297"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, "<p>Documento</p>", null, isDefault, isActive
        );
    }
}

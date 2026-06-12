package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.billing.dto.NfeResponse;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfeEmissionService - orquestração de emissão NF-e SEFAZ-SP")
class NfeEmissionServiceTest {

    @Mock NfeXmlBuilder xmlBuilder;
    @Mock NfeXmlSigner xmlSigner;
    @Mock NfeXsdValidator xsdValidator;
    @Mock NfeSefazClient sefazClient;
    @Mock FiscalDocumentService fiscalDocumentService;
    @Mock CustomerRepository customerRepository;

    @InjectMocks
    NfeEmissionService emissionService;

    @Test
    @DisplayName("emit() persiste FiscalDocument NFE com status AUTHORIZED quando cStat 100")
    void emit_salvadoComoAuthorized() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(xmlBuilder.buildXml(any(NfeEmissionRequest.class), any(Customer.class))).thenReturn("<NFe/>");
        when(xmlSigner.sign("<NFe/>")).thenReturn("<NFe signed/>");
        when(sefazClient.transmit("<NFe signed/>")).thenReturn(respostaAutorizada());
        when(fiscalDocumentService.save(any(FiscalDocument.class))).thenAnswer(i -> i.getArgument(0));

        NfeResponse resp = emissionService.emit(requestPadrao(customerId, originId));

        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
        verify(xsdValidator).validate("<NFe signed/>");
        verify(fiscalDocumentService).save(argThat(doc ->
                doc.getType() == FiscalDocumentType.NFE
                && doc.getStatus() == FiscalDocumentStatus.AUTHORIZED
                && doc.getModel() == 55
                && doc.getOrigin() == FiscalOrigin.SALES
                && doc.getOriginId().equals(originId)));
    }

    @Test
    @DisplayName("emit() persiste com status REJECTED quando SEFAZ rejeita")
    void emit_salvadoComoRejected() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(xmlBuilder.buildXml(any(), any())).thenReturn("<NFe/>");
        when(xmlSigner.sign(any())).thenReturn("<NFe signed/>");
        when(sefazClient.transmit(any())).thenReturn(respostaRejeitada());
        when(fiscalDocumentService.save(any())).thenAnswer(i -> i.getArgument(0));

        NfeResponse resp = emissionService.emit(requestPadrao(customerId, UUID.randomUUID()));

        assertThat(resp.getStatus()).isEqualTo("REJECTED");
        verify(fiscalDocumentService).save(argThat(doc ->
                doc.getStatus() == FiscalDocumentStatus.REJECTED
                && doc.getRejectionReason() != null));
    }

    @Test
    @DisplayName("emit() lança ResourceNotFoundException quando cliente não existe")
    void emit_lancaExcecao_clienteNaoEncontrado() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emissionService.emit(requestPadrao(customerId, UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(sefazClient);
    }

    @Test
    @DisplayName("emit() não persiste quando validação XSD falha")
    void emit_naoPersiste_validacaoFalha() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(xmlBuilder.buildXml(any(), any())).thenReturn("<NFe/>");
        when(xmlSigner.sign(any())).thenReturn("<NFe signed/>");
        doThrow(new NfeValidationException("infNFe ausente")).when(xsdValidator).validate(any());

        assertThatThrownBy(() -> emissionService.emit(requestPadrao(customerId, UUID.randomUUID())))
                .isInstanceOf(NfeValidationException.class);

        verifyNoInteractions(sefazClient);
        verify(fiscalDocumentService, never()).save(any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Customer clienteBase(UUID id) {
        Customer c = new Customer();
        c.setId(id);
        c.setName("Cliente Teste");
        c.setDocument("12345678901");
        return c;
    }

    private NfeEmissionRequest requestPadrao(UUID customerId, UUID originId) {
        return NfeEmissionRequest.builder()
                .customerId(customerId)
                .natureOperation("Venda de mercadoria")
                .origin("SALES")
                .originId(originId)
                .items(List.of(NfeItemRequest.builder()
                        .productCode("PROD-001")
                        .description("Camiseta")
                        .ncm("61091000")
                        .cfop("5102")
                        .unit("UN")
                        .quantity(BigDecimal.valueOf(2))
                        .unitValue(BigDecimal.valueOf(50))
                        .build()))
                .build();
    }

    private NfeResponse respostaAutorizada() {
        return NfeResponse.builder()
                .accessKey("35200000000000000191550010000000011000000010")
                .protocol("135200000000001")
                .cStat("100")
                .xMotivo("Autorizado o uso da NF-e")
                .status("AUTHORIZED")
                .authorizedXml("<nfeProc/>")
                .build();
    }

    private NfeResponse respostaRejeitada() {
        return NfeResponse.builder()
                .cStat("539")
                .xMotivo("Rejeicao: duplicidade de NF-e")
                .status("REJECTED")
                .build();
    }
}

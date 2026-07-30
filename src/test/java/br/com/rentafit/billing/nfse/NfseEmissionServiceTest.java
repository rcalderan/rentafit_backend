package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfseEmissionService - orquestração de emissão NFS-e Nacional")
class NfseEmissionServiceTest {

    @Mock NfseDpsXmlBuilder dpsXmlBuilder;
    @Mock NfseDpsXsdValidator xsdValidator;
    @Mock NfseXmlSigner xmlSigner;
    @Mock NfsePortalClient portalClient;
    @Mock FiscalDocumentService fiscalDocumentService;
    @Mock CustomerRepository customerRepository;

    @InjectMocks
    NfseEmissionService emissionService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "ibsAliquotaPadrao", new BigDecimal("0.025"));
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "cbsAliquotaPadrao", new BigDecimal("0.015"));
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "prestadorCnpj", "00000000000000");
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "ambiente", "2");
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "cLocEmi", "3550308");
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "serieDps", "1");
        org.springframework.test.util.ReflectionTestUtils.setField(
                emissionService, "cTribNacPadrao", "140201");
    }

    @Test
    @DisplayName("emit() salva FiscalDocument com status AUTHORIZED quando portal aceita")
    void emit_salvadoComoAuthorized() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(dpsXmlBuilder.buildXml(any(DpsRequest.class))).thenReturn("<infDPS/>");
        when(xmlSigner.sign("<infDPS/>")).thenReturn("<infDPS signed/>");
        when(portalClient.sendDps("<infDPS signed/>")).thenReturn(Mono.just(dpsResponse()));
        when(fiscalDocumentService.save(any(FiscalDocument.class))).thenAnswer(i -> i.getArgument(0));

        Mono<InvoiceEmissionResponseDTO> result = emissionService.emit(requestPadrao(customerId, originId));

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
                    assertThat(resp.getAccessKey()).isEqualTo("CHAVE-50");
                })
                .verifyComplete();

        verify(fiscalDocumentService).save(argThat(doc ->
                doc.getType() == FiscalDocumentType.NFSE
                && doc.getStatus() == FiscalDocumentStatus.AUTHORIZED
                && doc.getOrigin() == FiscalOrigin.SALES
                && doc.getOriginId().equals(originId)));
    }

    @Test
    @DisplayName("emit() lança ResourceNotFoundException quando cliente não existe")
    void emit_lancaExcecao_clienteNaoEncontrado() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        Mono<InvoiceEmissionResponseDTO> result = emissionService.emit(requestPadrao(customerId, UUID.randomUUID()));

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("emit() propaga erro do portal como falha no Mono")
    void emit_propagaErroPortal() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(dpsXmlBuilder.buildXml(any())).thenReturn("<infDPS/>");
        when(xmlSigner.sign(any())).thenReturn("<infDPS signed/>");
        when(portalClient.sendDps(any())).thenReturn(Mono.error(new RuntimeException("Portal indisponível")));

        Mono<InvoiceEmissionResponseDTO> result = emissionService.emit(requestPadrao(customerId, UUID.randomUUID()));

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(fiscalDocumentService, never()).save(any());
    }

    @Test
    @DisplayName("emit() aplica alíquotas padrão quando request não informa taxas")
    void emit_aplicaAliquotasPadrao() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(dpsXmlBuilder.buildXml(any(DpsRequest.class))).thenReturn("<infDPS/>");
        when(xmlSigner.sign(any())).thenReturn("<infDPS signed/>");
        when(portalClient.sendDps(any())).thenReturn(Mono.just(dpsResponse()));
        when(fiscalDocumentService.save(any())).thenAnswer(i -> i.getArgument(0));

        InvoiceEmissionRequestDTO req = requestPadrao(customerId, UUID.randomUUID());
        req.setIbsRate(null);
        req.setCbsRate(null);

        Mono<InvoiceEmissionResponseDTO> result = emissionService.emit(req);

        StepVerifier.create(result)
                .assertNext(resp -> assertThat(resp.getTaxes().getIbsRate())
                        .isEqualByComparingTo(new BigDecimal("0.025")))
                .verifyComplete();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("emit() lança ValidationException quando XSD rejeita o XML")
    void emit_lancaValidationException_quandoXsdFalha() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(clienteBase(customerId)));
        when(dpsXmlBuilder.buildXml(any(DpsRequest.class))).thenReturn("<invalid/>");
        doThrow(new ValidationException("XSD falhou: elemento ausente"))
                .when(xsdValidator).validate("<invalid/>");

        Mono<InvoiceEmissionResponseDTO> result = emissionService.emit(requestPadrao(customerId, UUID.randomUUID()));

        StepVerifier.create(result)
                .expectError(ValidationException.class)
                .verify();

        verify(xmlSigner, never()).sign(any());
        verify(portalClient, never()).sendDps(any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Customer clienteBase(UUID id) {
        Customer c = new Customer();
        c.setId(id);
        c.setName("Teste Silva");
        c.setDocument("12345678901");
        return c;
    }

    private InvoiceEmissionRequestDTO requestPadrao(UUID customerId, UUID originId) {
        return InvoiceEmissionRequestDTO.builder()
                .customerId(customerId)
                .serviceValue(BigDecimal.valueOf(500))
                .nbsCode("1.0101")
                .serviceDescription("Locação de traje")
                .cityCode("3550308")
                .originId(originId)
                .origin("SALES")
                .build();
    }

    private DpsResponse dpsResponse() {
        return DpsResponse.builder()
                .accessKey("CHAVE-50")
                .protocol("PROT-12345")
                .status("AUTORIZADA")
                .dhProcessamento(OffsetDateTime.now())
                .build();
    }
}

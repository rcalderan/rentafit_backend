package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private NfsePortalService nfsePortalService;

    @Mock
    private FiscalDocumentService fiscalDocumentService;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(billingService, "ibsAliquotaPadrao", new BigDecimal("0.025"));
        org.springframework.test.util.ReflectionTestUtils.setField(billingService, "cbsAliquotaPadrao", new BigDecimal("0.015"));
    }

    @Test
    @DisplayName("Should emit an invoice successfully")
    void shouldEmitInvoiceSuccessfully() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setDocument("12345678000199");

        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(customerId)
                .serviceValue(new BigDecimal("100.00"))
                .nbsCode("1.0101")
                .serviceDescription("Test service")
                .cityCode("3550308")
                .build();

        DpsResponse dpsResponse = DpsResponse.builder()
                .accessKey("dummy-access-key")
                .protocol("1234567890dummy-protocol")
                .status("AUTORIZADA")
                .dhProcessamento(OffsetDateTime.now())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(nfsePortalService.sendDps(any(DpsRequest.class)))
                .thenReturn(Mono.just(dpsResponse));
        when(fiscalDocumentService.save(any(FiscalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<InvoiceEmissionResponseDTO> result = billingService.emitInvoice(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getAccessKey()).isEqualTo("dummy-access-key");
                    assertThat(response.getServiceValue()).isEqualByComparingTo(new BigDecimal("100.00"));
                    assertThat(response.getStatus()).isEqualTo("AUTHORIZED");
                    assertThat(response.getTaxes()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(customerId)
                .serviceValue(BigDecimal.TEN)
                .nbsCode("1.0101")
                .serviceDescription("Test")
                .cityCode("3550308")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> billingService.emitInvoice(request));
    }

    @Test
    @DisplayName("Should emit invoice with custom tax rates")
    void shouldEmitInvoiceWithCustomTaxRates() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Jane Doe");
        customer.setDocument("12345678901"); // CPF

        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(customerId)
                .serviceValue(new BigDecimal("200.00"))
                .nbsCode("2.0202")
                .serviceDescription("Custom tax service")
                .cityCode("3550308")
                .ibsRate(new BigDecimal("0.03"))
                .cbsRate(new BigDecimal("0.02"))
                .isqnRate(new BigDecimal("0.01"))
                .build();

        DpsResponse dpsResponse = DpsResponse.builder()
                .accessKey("custom-access-key")
                .protocol("custom-protocol")
                .status("AUTORIZADA")
                .dhProcessamento(OffsetDateTime.now())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(nfsePortalService.sendDps(any(DpsRequest.class)))
                .thenReturn(Mono.just(dpsResponse));
        when(fiscalDocumentService.save(any(FiscalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<InvoiceEmissionResponseDTO> result = billingService.emitInvoice(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getAccessKey()).isEqualTo("custom-access-key");
                    assertThat(response.getTaxes().getIbsRate()).isEqualByComparingTo(new BigDecimal("0.03"));
                    assertThat(response.getTaxes().getCbsRate()).isEqualByComparingTo(new BigDecimal("0.02"));
                    assertThat(response.getTaxes().getIsqnRate()).isEqualByComparingTo(new BigDecimal("0.01"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle CPF document correctly")
    void shouldHandleCpfDocument() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Individual Person");
        customer.setDocument("12345678901"); // CPF (11 chars)

        InvoiceEmissionRequestDTO request = InvoiceEmissionRequestDTO.builder()
                .customerId(customerId)
                .serviceValue(new BigDecimal("50.00"))
                .nbsCode("3.0303")
                .serviceDescription("Individual service")
                .cityCode("3550308")
                .build();

        DpsResponse dpsResponse = DpsResponse.builder()
                .accessKey("cpf-access-key")
                .protocol("cpf-protocol")
                .status("AUTORIZADA")
                .dhProcessamento(OffsetDateTime.now())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(nfsePortalService.sendDps(any(DpsRequest.class)))
                .thenReturn(Mono.just(dpsResponse));
        when(fiscalDocumentService.save(any(FiscalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<InvoiceEmissionResponseDTO> result = billingService.emitInvoice(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getAccessKey()).isEqualTo("cpf-access-key");
                })
                .verifyComplete();
    }
}

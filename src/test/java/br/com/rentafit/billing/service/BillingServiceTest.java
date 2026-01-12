package br.com.rentafit.billing.service;

import br.com.rentafit.billing.service.NfsePortalService;
import br.com.rentafit.billing.domain.Invoice;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
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
    private InvoiceService invoiceService;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    @DisplayName("Should emit an invoice successfully")
    void shouldEmitInvoiceSuccessfully() {
        UUID customerId = UUID.randomUUID();
        BigDecimal serviceValue = new BigDecimal("100.00");
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setDocument("12345678000199");

        DpsResponse dpsResponse = DpsResponse.builder()
                .accessKey("dummy-access-key")
                .protocol("dummy-protocol")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(nfsePortalService.sendDps(any(DpsRequest.class)))
                .thenReturn(Mono.just(dpsResponse));
        when(invoiceService.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Invoice> result = billingService.emitInvoice(customerId, serviceValue);

        StepVerifier.create(result)
                .assertNext(invoice -> {
                    assertThat(invoice.getAccessKey()).isEqualTo("dummy-access-key");
                    assertThat(invoice.getServiceValue()).isEqualTo(serviceValue);
                    assertThat(invoice.getCustomer()).isEqualTo(customer);
                    assertThat(invoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.AUTHORIZED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        try {
            billingService.emitInvoice(customerId, BigDecimal.TEN);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Cliente não encontrado");
        }
    }
}


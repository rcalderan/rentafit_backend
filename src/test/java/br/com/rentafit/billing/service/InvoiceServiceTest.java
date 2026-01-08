package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.Invoice;
import br.com.rentafit.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Should save an invoice")
    void shouldSaveInvoice() {
        Invoice invoice = Invoice.builder().id(UUID.randomUUID()).build();
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        Invoice saved = invoiceService.save(invoice);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(invoice.getId());
    }

    @Test
    @DisplayName("Should find an invoice by id")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(id).build();
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        Optional<Invoice> found = invoiceService.getById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should find an invoice by access key")
    void shouldFindByAccessKey() {
        String accessKey = "some-key";
        Invoice invoice = Invoice.builder().accessKey(accessKey).build();
        when(invoiceRepository.findByAccessKey(accessKey)).thenReturn(Optional.of(invoice));

        Optional<Invoice> found = invoiceService.getByAccessKey(accessKey);

        assertThat(found).isPresent();
        assertThat(found.get().getAccessKey()).isEqualTo(accessKey);
    }
}


package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.Invoice;
import br.com.rentafit.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public Optional<Invoice> getById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getByAccessKey(String accessKey) {
        return invoiceRepository.findByAccessKey(accessKey);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getByInvoiceNumber(Long invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Transactional
    public Invoice save(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
}

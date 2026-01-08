package br.com.rentafit.billing.repository;

import br.com.rentafit.billing.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByAccessKey(String accessKey);
    Optional<Invoice> findByInvoiceNumber(Long invoiceNumber);
}

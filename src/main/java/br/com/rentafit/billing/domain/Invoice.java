package br.com.rentafit.billing.domain;

import br.com.rentafit.people.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents an authorized NFS-e")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "access_key", unique = true, nullable = false)
    @Schema(description = "Chave de acesso da NFS-e (50 caracteres)")
    private String accessKey;

    @Column(name = "invoice_number", nullable = false)
    private Long invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "issue_date", nullable = false)
    private OffsetDateTime issueDate;

    @Column(name = "service_value", nullable = false)
    private BigDecimal serviceValue;

    @Embedded
    private TaxInfo taxes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum InvoiceStatus {
        AUTHORIZED, CANCELLED, REPLACED
    }
}

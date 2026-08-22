package br.com.rentafit.billing.domain;

import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.people.domain.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade unificada para NF-e e NFS-e.
 *
 * <p>Substitui {@code Invoice}, unificando os dois fluxos fiscais em uma única tabela.
 * O campo {@code origin}/{@code originId} rastreia qual pedido ou contrato gerou o documento.</p>
 */
@Entity
@Table(name = "fiscal_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalDocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalDocumentStatus status;

    /** Modelo fiscal: 55 = NF-e, 65 = NFC-e, 99 = NFS-e nacional */
    @Column
    private Integer model;

    @Column
    private String series;

    @Column
    private Long number;

    /** Chave de acesso: 44 dígitos para NF-e, 50 para NFS-e */
    @Column(name = "access_key", unique = true)
    private String accessKey;

    @Column
    private String protocol;

    @Column(name = "authorization_date")
    private OffsetDateTime authorizationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "issue_date", nullable = false)
    private OffsetDateTime issueDate;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Embedded
    private TaxInfo taxes;

    @Enumerated(EnumType.STRING)
    @Column
    private FiscalOrigin origin;

    @Column(name = "origin_id")
    private UUID originId;

    @Column(name = "signed_xml", columnDefinition = "TEXT")
    private String signedXml;

    @Column(name = "authorized_xml", columnDefinition = "TEXT")
    private String authorizedXml;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "service_description", length = 500)
    private String serviceDescription;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_protocol", length = 50)
    private String cancelProtocol;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

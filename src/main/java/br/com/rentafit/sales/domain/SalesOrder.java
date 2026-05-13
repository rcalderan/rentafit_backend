package br.com.rentafit.sales.domain;

import br.com.rentafit.sales.domain.enums.InvoiceStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de venda (varejo).
 *
 * <p>Referências cruzadas (customerId, createdByEmployeeId) são UUID puro
 * para garantir desacoplamento do componente People — mesmo padrão de RentalContract.</p>
 *
 * <p>customerName e customerDocument são snapshots imutáveis gravados na criação.
 * Vendas de balcão (sem cliente) mantêm esses campos como null.</p>
 */
@Entity
@Table(name = "sales_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** Código legível gerado automaticamente: V-YYYYMMDD-N */
    @Column(name = "legacy_id", unique = true)
    private String legacyId;

    // ── Snapshot do cliente (nullable para venda balcão) ─────────────────────
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_document")
    private String customerDocument;

    // ── Estado do pedido ─────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String notes = "";

    /** Desconto sobre o total do pedido (R$). */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    /** Motivo obrigatório ao cancelar. */
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // ── NFS-e ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false)
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.NONE;

    /** Referência externa da NFS-e (chave, protocolo, etc.) */
    @Column(name = "invoice_id")
    private String invoiceId;

    // ── Referências cruzadas ─────────────────────────────────────────────────
    @Column(name = "created_by_employee_id")
    private UUID createdByEmployeeId;

    // ── Timestamps ───────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    @SuppressWarnings("unused")
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Relacionamentos ──────────────────────────────────────────────────────
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SalesOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SalesPayment> payments = new ArrayList<>();
}

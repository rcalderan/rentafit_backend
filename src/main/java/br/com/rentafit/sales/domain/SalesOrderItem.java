package br.com.rentafit.sales.domain;

import br.com.rentafit.sales.domain.enums.SalesItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Item de um pedido de venda.
 *
 * <p>retailProductId é UUID puro para desacoplamento do componente Product.
 * sku e description são snapshots imutáveis gravados na adição do item.</p>
 */
@Entity
@Table(name = "sales_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SalesOrder salesOrder;

    /** UUID do RetailProduct no catálogo. */
    @Column(name = "retail_product_id", nullable = false)
    private UUID retailProductId;

    /** Snapshot do SKU no momento da adição. */
    @Column(nullable = false)
    private String sku;

    /** Snapshot da descrição do produto. */
    @Column(nullable = false)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /** Desconto sobre este item (R$). */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private SalesItemStatus itemStatus = SalesItemStatus.PENDING;

    /** UUID do funcionário atendente. */
    @Column(name = "attendant_employee_id")
    private UUID attendantEmployeeId;

    // ── Ajuste/costura (campos opcionais) ────────────────────────────────────
    @Column(name = "needs_tailoring", nullable = false)
    @Builder.Default
    private Boolean needsTailoring = false;

    /** Texto livre: medidas, instruções de costura, etc. */
    @Column(name = "tailoring_notes", columnDefinition = "TEXT")
    private String tailoringNotes;

    // ── Entrega ──────────────────────────────────────────────────────────────
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "delivered_by_employee_id")
    private UUID deliveredByEmployeeId;
}

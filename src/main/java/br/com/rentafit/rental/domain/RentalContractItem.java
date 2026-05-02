package br.com.rentafit.rental.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Item de um contrato de locação.
 *
 * <p>rentalItemId é UUID puro (ON DELETE SET NULL na migration) para preservar o histórico do contrato
 * mesmo que o produto seja removido do catálogo. O snapshot legacyProductCode e description
 * garante legibilidade mesmo com rentalItemId nulo.</p>
 */
@Entity
@Table(name = "rental_contract_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContractItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RentalContract contract;

    /** UUID do RentalItem no catálogo. Pode ser null se o item foi removido do catálogo. */
    @Column(name = "rental_item_id")
    private UUID rentalItemId;

    /** Snapshot do código legado (codigo). Preservado para exibição mesmo se rentalItemId for null. */
    @Column(name = "legacy_product_code")
    private String legacyProductCode;

    /** Snapshot da descrição do produto no momento da criação. */
    @Column(nullable = false)
    private String description;

    /** Valor negociado. Único campo editável pelo usuário. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "is_delivered", nullable = false)
    @Builder.Default
    private Boolean delivered = false;

    /** UUID do funcionário que registrou a entrega (atendente). */
    @Column(name = "attendant_employee_id")
    private UUID attendantEmployeeId;

    @Column(name = "returned", nullable = false)
    @Builder.Default
    private Boolean returned = false;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Column(name = "returned_by_name")
    private String returnedByName;

    @OneToMany(mappedBy = "contractItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RentalContractItemMeta> metadata = new ArrayList<>();
}


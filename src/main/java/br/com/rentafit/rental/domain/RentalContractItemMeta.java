package br.com.rentafit.rental.domain;

import br.com.rentafit.rental.domain.enums.ItemMetaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Metadado de um item de contrato.
 *
 * <p>OBSERVACAO: texto livre, accessoryId sempre null.</p>
 * <p>ACESSORIO: quando accessoryId preenchido, indica acessório catalogado com
 * controle de estoque via AccessoryPort. Quando null, é descrição textual sem reserva.</p>
 */
@Entity
@Table(name = "rental_contract_item_meta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContractItemMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RentalContractItem contractItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemMetaType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * UUID do acessório catalogado (accessories.id). Null para texto livre.
     * Migração define ON DELETE SET NULL para preservar o histórico.
     */
    @Column(name = "accessory_id")
    private UUID accessoryId;
}


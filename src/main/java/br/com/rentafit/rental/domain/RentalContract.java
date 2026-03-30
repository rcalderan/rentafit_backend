package br.com.rentafit.rental.domain;

import br.com.rentafit.rental.domain.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Contrato de locação.
 *
 * <p>Imutabilidade por design: os campos de snapshot do cliente (customerName, customerDocument)
 * são gravados na criação e nunca atualizados — um contrato formalizado não deve ser alterado;
 * para qualquer mudança após FINALIZED, utilizar o endpoint duplicate.</p>
 *
 * <p>Referências cruzadas (customerId, createdByEmployeeId, etc.) são armazenadas como UUID puro
 * (sem @ManyToOne) para garantir desacoplamento do componente People e viabilizar migração futura
 * para microserviço — substituindo apenas os adapters de porta.</p>
 */
@Entity
@Table(name = "rental_contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "legacy_id", unique = true)
    private String legacyId;

    @Column(name = "contract_type", nullable = false)
    @Builder.Default
    private Integer contractType = 0;

    // ── Snapshot imutável do cliente ───────────────────────────────────────────
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_document")
    private String customerDocument;

    // ── Referências cruzadas (UUID puro para desacoplamento) ──────────────────
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "created_by_employee_id")
    private UUID createdByEmployeeId;

    @Column(name = "returned_by_employee_id")
    private UUID returnedByEmployeeId;

    /** ID do contrato que esta revisão substitui (null se for contrato original). */
    @Column(name = "parent_contract_id")
    private UUID parentContractId;

    /** ID do contrato vigente que substituiu este (preenchido quando status = SUPERSEDED). */
    @Column(name = "replaced_by_contract_id")
    private UUID replacedByContractId;

    // ── Datas do contrato ──────────────────────────────────────────────────────
    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    // ── Estado do contrato ─────────────────────────────────────────────────────
    @Column(name = "is_returned", nullable = false)
    @Builder.Default
    private Boolean returned = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String notes = "";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Relacionamentos ────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RentalContractItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RentalPayment> payments = new ArrayList<>();
}

package br.com.rentafit.rental.domain;

import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Parcela de pagamento de um contrato de locação.
 *
 * <p>Regras de negócio (aplicadas no RentalPaymentService):
 * <ul>
 *   <li>installmentNumber entre 1 e 24</li>
 *   <li>paymentDate não pode ser posterior ao eventDate do contrato</li>
 *   <li>soma de PENDING+PAID não pode ultrapassar o totalValue do contrato</li>
 *   <li>addPayment permitido mesmo após FINALIZED; update/cancel bloqueados após FINALIZED</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "rental_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RentalContract contract;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    /** Número de parcelas do cartão (vezes). 1 para pagamentos à vista. */
    @Column(nullable = false)
    @Builder.Default
    private Integer installments = 1;

    /** UUID do funcionário que registrou o pagamento. */
    @Column(name = "processed_by_employee_id")
    private UUID processedByEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
}


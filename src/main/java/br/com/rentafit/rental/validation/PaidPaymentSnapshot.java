package br.com.rentafit.rental.validation;

import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaidPaymentSnapshot(Integer number, LocalDate date, String method, BigDecimal value,
                                  Integer installments, UUID employeeId) {
    public static PaidPaymentSnapshot from(RentalPayment payment) {
        return new PaidPaymentSnapshot(payment.getInstallmentNumber(), payment.getPaymentDate(),
                payment.getPaymentMethod().name(), payment.getValue().stripTrailingZeros(),
                payment.getInstallments(), payment.getProcessedByEmployeeId());
    }

    public static PaidPaymentSnapshot from(RentalPaymentInputDTO payment) {
        return new PaidPaymentSnapshot(payment.installmentNumber(), payment.paymentDate(),
                payment.paymentMethod().toUpperCase(java.util.Locale.ROOT), payment.value().stripTrailingZeros(),
                payment.installments() == null ? 1 : payment.installments(), payment.processedByEmployeeId());
    }

    public static List<PaidPaymentSnapshot> paid(List<RentalPayment> payments) {
        return payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(PaidPaymentSnapshot::from).sorted(java.util.Comparator.comparing(PaidPaymentSnapshot::number)).toList();
    }
}

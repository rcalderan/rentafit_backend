package br.com.rentafit.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RentalPaymentDetailsDTO(
        UUID id,
        Integer installmentNumber,
        LocalDate paymentDate,
        String paymentMethod,
        String paymentMethodLabel,
        BigDecimal value,
        Integer installments,
        UUID processedByEmployeeId,
        String status,
        String statusDescription
) {}


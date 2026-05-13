package br.com.rentafit.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de saída para parcela de pagamento de venda.
 */
public record SalesPaymentDetailsDTO(
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

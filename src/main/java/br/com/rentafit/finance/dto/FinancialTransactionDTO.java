package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa uma parcela financeira individual de locação ou venda.
 */
@Builder
public record FinancialTransactionDTO(
        UUID paymentId,
        String source,
        UUID referenceId,
        String referenceCode,
        String customerName,
        Integer installmentNumber,
        Integer installments,
        LocalDate paymentDate,
        String paymentMethod,
        String status,
        BigDecimal value,
        BigDecimal signedValue,
        UUID sectorId,
        String sectorName,
        String sectorCode,
        String issuerCnpj
) {
}

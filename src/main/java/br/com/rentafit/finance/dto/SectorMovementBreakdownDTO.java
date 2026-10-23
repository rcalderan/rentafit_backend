package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Totais consolidados por setor de caixa (sectorId nulo = lançamentos legados pré-V44).
 */
@Builder
public record SectorMovementBreakdownDTO(
        UUID sectorId,
        String sectorName,
        String sectorCode,
        String issuerCnpj,
        BigDecimal paidAmount,
        BigDecimal fineAmount,
        BigDecimal refundedAmount,
        BigDecimal netRealizedAmount,
        BigDecimal pendingAmount,
        int transactionCount
) {
}

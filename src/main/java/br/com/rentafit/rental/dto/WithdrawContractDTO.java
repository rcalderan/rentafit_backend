package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request de desistência de contrato (SIGNED/FINALIZED → CANCELLED).
 *
 * <p>Chamado apenas após o operador confirmar que o Termo de Desistência foi
 * impresso e assinado pelo cliente (gate no frontend).</p>
 *
 * <p>Exemplo: {"employeeId": "uuid", "refundPaymentIds": ["uuid", ...],
 * "applyFine": true, "fineAmount": 135.00}</p>
 */
public record WithdrawContractDTO(

        @NotNull(message = "employeeId é obrigatório")
        UUID employeeId,

        /** Parcelas PAID a reembolsar. Vazio/nulo = sem devolução de valores. */
        List<UUID> refundPaymentIds,

        @NotNull(message = "applyFine é obrigatório")
        Boolean applyFine,

        /** Multa rescisória (sugerido: 30% do total — Cláusula 6a). */
        @DecimalMin(value = "0.01", message = "fineAmount deve ser maior que zero quando informado")
        BigDecimal fineAmount
) {}

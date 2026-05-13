package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request para fechar o contrato após devolução completa.
 *
 * <p>Exemplo: {"employeeId": "uuid", "applyFine": true, "fineAmount": 50.00}</p>
 */
public record CloseReturnRequestDTO(

        @NotNull(message = "employeeId é obrigatório")
        UUID employeeId,

        @NotNull(message = "applyFine é obrigatório")
        Boolean applyFine,

        @DecimalMin(value = "0.01", message = "fineAmount deve ser maior que zero quando informado")
        BigDecimal fineAmount
) {}

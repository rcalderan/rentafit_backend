package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada para inclusão/atualização de um pagamento (parcela).
 */
public record RentalPaymentInputDTO(

        @NotNull(message = "Número da parcela é obrigatório")
        @Min(value = 1, message = "Número da parcela mínimo é 1")
        @Max(value = 24, message = "Número máximo de parcelas é 24")
        Integer installmentNumber,

        @NotNull(message = "Data do pagamento é obrigatória")
        LocalDate paymentDate,

        @NotNull(message = "Forma de pagamento é obrigatória")
        String paymentMethod,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @DecimalMax(value = "99999999.99", message = "Valor não pode exceder 99.999.999,99")
        BigDecimal value,

        @Min(value = 1, message = "Número de vezes deve ser no mínimo 1")
        @Max(value = 24, message = "Número de vezes deve ser no máximo 24")
        Integer installments,

        UUID processedByEmployeeId,

        String status
) {}


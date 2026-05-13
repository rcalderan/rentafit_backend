package br.com.rentafit.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para marcar itens/acessórios como devolvidos.
 *
 * <p>Quando {@code applyFine=true} e {@code fineAmount > 0}, uma parcela com
 * status {@code MULTA} é criada automaticamente no contrato.</p>
 *
 * <p>Exemplo: {"returnerName": "João", "entries": [...], "applyFine": true, "fineAmount": 50.00}</p>
 */
public record MarkReturnRequestDTO(

        @NotBlank(message = "returnerName é obrigatório")
        String returnerName,

        @NotEmpty(message = "entries não pode ser vazio")
        @Valid
        List<ReturnEntryDTO> entries,

        Boolean applyFine,

        @DecimalMin(value = "0.01", message = "fineAmount deve ser maior que zero quando informado")
        BigDecimal fineAmount
) {}

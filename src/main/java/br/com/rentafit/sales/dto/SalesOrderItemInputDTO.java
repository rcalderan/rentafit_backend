package br.com.rentafit.sales.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para um item do pedido de venda.
 * retailProductId é obrigatório; sku e description são resolvidos pelo service via catálogo.
 */
public record SalesOrderItemInputDTO(

        @NotNull(message = "ID do produto retail é obrigatório")
        UUID retailProductId,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        Integer quantity,

        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @DecimalMax(value = "99999999.99", message = "Desconto não pode exceder 99.999.999,99")
        BigDecimal discountValue,

        UUID attendantEmployeeId,

        Boolean needsTailoring,

        String tailoringNotes
) {}

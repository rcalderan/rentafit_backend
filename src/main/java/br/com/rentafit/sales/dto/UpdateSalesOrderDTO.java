package br.com.rentafit.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para atualização de um pedido de venda (somente DRAFT).
 * Substitui itens e pagamentos por completo (replace semantics).
 */
public record UpdateSalesOrderDTO(

        UUID customerId,

        String notes,

        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @DecimalMax(value = "99999999.99", message = "Desconto não pode exceder 99.999.999,99")
        BigDecimal discountValue,

        @NotNull(message = "Lista de itens não pode ser nula")
        @NotEmpty(message = "O pedido deve ter ao menos um item")
        @Valid
        List<SalesOrderItemInputDTO> items,

        @Valid
        List<SalesPaymentInputDTO> payments
) {}

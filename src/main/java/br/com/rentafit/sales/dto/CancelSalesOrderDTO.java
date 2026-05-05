package br.com.rentafit.sales.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para cancelamento de um pedido de venda.
 */
public record CancelSalesOrderDTO(

        @NotBlank(message = "Motivo do cancelamento é obrigatório")
        String reason
) {}

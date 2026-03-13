package br.com.rentafit.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para criação de um contrato (proposta).
 * Validações de ordem de datas são executadas no RentalContractValidator.
 * Regras de pagamento:
 * - Ao menos uma parcela é obrigatória.
 * - A soma das parcelas (PENDING + PAID) deve ser igual ao valor total dos itens.
 */
public record CreateRentalContractDTO(

        @NotNull(message = "Cliente é obrigatório")
        UUID customerId,

        Integer contractType,

        UUID createdByEmployeeId,

        @NotNull(message = "Data de retirada é obrigatória")
        LocalDate pickupDate,

        @NotNull(message = "Data de uso (evento) é obrigatória")
        LocalDate eventDate,

        @NotNull(message = "Data de devolução é obrigatória")
        LocalDate returnDate,

        String notes,

        @NotNull(message = "Lista de itens não pode ser nula")
        @NotEmpty(message = "O contrato deve ter ao menos um item")
        @Valid
        List<ContractItemInputDTO> items,

        @NotNull(message = "Lista de parcelas não pode ser nula")
        @NotEmpty(message = "O contrato deve ter ao menos uma parcela de pagamento")
        @Valid
        List<RentalPaymentInputDTO> payments
) {}


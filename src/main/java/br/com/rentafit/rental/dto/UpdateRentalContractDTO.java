package br.com.rentafit.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para atualização de um contrato DRAFT.
 * Bloqueado pelo serviço se o contrato não estiver em status DRAFT.
 */
public record UpdateRentalContractDTO(

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
        List<ContractItemInputDTO> items
) {}


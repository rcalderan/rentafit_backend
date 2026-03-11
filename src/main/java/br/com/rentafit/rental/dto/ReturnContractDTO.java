package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada para processar a devolução de um contrato.
 */
public record ReturnContractDTO(

        @NotNull(message = "Data de devolução efetiva é obrigatória")
        LocalDate actualReturnDate,

        UUID returnedByEmployeeId
) {}


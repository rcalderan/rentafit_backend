package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.Size;

public record SignRentalContractDTO(
        @Size(max = 100, message = "printTemplateId deve ter no máximo 100 caracteres")
        String printTemplateId
) {}

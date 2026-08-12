package br.com.rentafit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to link the authenticated user to an issuer CNPJ.
 */
public record SetupIssuerCnpjRequestDTO(

        @NotBlank(message = "CNPJ is required")
        @Pattern(regexp = "^\\d{14}$", message = "CNPJ must contain exactly 14 numeric digits")
        String issuerCnpj
) {
}

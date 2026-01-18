package br.com.rentafit.people.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record AddressDTO(
    @Size(min = 8, max = 10, message = "ZIP code must be 8 or 9 characters (with optional hyphen)")
    @Pattern(regexp = "^[0-9]{5}-?[0-9]{3}$", message = "ZIP code must be in format 12345-678 or 12345678")
    String zipCode,

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must not exceed 255 characters")
    String street,

    @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
    String neighborhood,

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 2, message = "State must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State must be 2 uppercase letters")
    String state
) {}

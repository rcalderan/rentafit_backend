package br.com.rentafit.people.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.UUID;

@Builder
public record AddressDTO(
    UUID id,

    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    @Pattern(regexp = "^[0-9]{5}-?[0-9]{3}$", message = "ZIP code must be in format 12345-678 or 12345678")
    String zipCode,

    @Size(max = 255, message = "Street must not exceed 255 characters")
    String street,

    @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
    String neighborhood,

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(min = 2, max = 2, message = "State must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State must be 2 uppercase letters")
    String state
) {}

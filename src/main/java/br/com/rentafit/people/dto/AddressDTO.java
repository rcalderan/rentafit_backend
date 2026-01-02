package br.com.rentafit.people.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record AddressDTO(
    UUID id,
    String zipCode,
    String street,
    String neighborhood,
    String city,
    String state
) {}

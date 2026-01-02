package br.com.rentafit.people.dto;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record CustomerDTO(
    UUID id,
    String name,
    String document,
    String email,
    boolean isAuthenticated,
    String notes,
    AddressDTO address,
    String number,
    String complement,
    List<String> phones
) {}

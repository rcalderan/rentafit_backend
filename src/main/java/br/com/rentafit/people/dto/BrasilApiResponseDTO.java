package br.com.rentafit.people.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Response DTO from BrasilAPI CEP endpoint.
 *
 * BrasilAPI is a free Brazilian public API aggregator that provides
 * postal code lookup with data from multiple sources including Correios.
 *
 * @see <a href="https://brasilapi.com.br/docs#tag/CEP">BrasilAPI - CEP Documentation</a>
 */
@Builder
public record BrasilApiResponseDTO(
    @JsonProperty("cep")
    String cep,

    @JsonProperty("state")
    String state,

    @JsonProperty("city")
    String city,

    @JsonProperty("neighborhood")
    String neighborhood,

    @JsonProperty("street")
    String street,

    @JsonProperty("service")
    String service,

    @JsonProperty("location")
    Location location
) {
    /**
     * Convert BrasilAPI response to ViaCEP format for compatibility
     */
    public ViaCepResponseDTO toViaCepFormat() {
        return ViaCepResponseDTO.builder()
                .cep(cep)
                .logradouro(street != null ? street : "")
                .bairro(neighborhood != null ? neighborhood : "")
                .localidade(city != null ? city : "")
                .uf(state != null ? state : "")
                .complemento("")
                .ibge(null)
                .gia(null)
                .ddd(null)
                .siafi(null)
                .erro(false)
                .build();
    }

    @Builder
    public record Location(
        @JsonProperty("type")
        String type,

        @JsonProperty("coordinates")
        Coordinates coordinates
    ) {}

    @Builder
    public record Coordinates(
        @JsonProperty("longitude")
        String longitude,

        @JsonProperty("latitude")
        String latitude
    ) {}
}

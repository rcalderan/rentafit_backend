package br.com.rentafit.people.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Response DTO from ViaCEP API
 */
@Builder
public record ViaCepResponseDTO(
    @JsonProperty("cep")
    String cep,

    @JsonProperty("logradouro")
    String logradouro,

    @JsonProperty("complemento")
    String complemento,

    @JsonProperty("bairro")
    String bairro,

    @JsonProperty("localidade")
    String localidade,

    @JsonProperty("uf")
    String uf,

    @JsonProperty("ibge")
    String ibge,

    @JsonProperty("gia")
    String gia,

    @JsonProperty("ddd")
    String ddd,

    @JsonProperty("siafi")
    String siafi,

    @JsonProperty("erro")
    Boolean erro
) {
    public boolean hasError() {
        return erro != null && erro;
    }

    public AddressDTO toAddressDTO() {
        return AddressDTO.builder()
                .zipCode(cep)
                .street(logradouro)
                .neighborhood(bairro)
                .city(localidade)
                .state(uf)
                .build();
    }
}

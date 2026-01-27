package br.com.rentafit.billing.dto.saocarlos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados do Tomador do Serviço")
public class SaoCarlosTomadorDTO {

    @Schema(description = "Identificação do Tomador")
    private IdentificacaoTomador identificacaoTomador;

    @Size(max = 115)
    @Schema(description = "Razão Social")
    private String razaoSocial;

    @Schema(description = "Endereço do Tomador")
    private Endereco endereco;

    @Schema(description = "Contato do Tomador")
    private Contato contato;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentificacaoTomador {
        @Schema(description = "CNPJ do tomador")
        private CpfCnpj cpfCnpj;

        @Size(max = 15)
        @Schema(description = "Inscrição Municipal")
        private String inscricaoMunicipal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpfCnpj {
        @Pattern(regexp = "\\d{11}")
        @Schema(description = "CPF do tomador (11 dígitos)")
        private String cpf;

        @Pattern(regexp = "\\d{14}")
        @Schema(description = "CNPJ do tomador (14 dígitos)")
        private String cnpj;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        @Size(max = 125)
        @Schema(description = "Logradouro")
        private String endereco;

        @Size(max = 10)
        @Schema(description = "Número")
        private String numero;

        @Size(max = 60)
        @Schema(description = "Complemento")
        private String complemento;

        @Size(max = 60)
        @Schema(description = "Bairro")
        private String bairro;

        @Schema(description = "Código IBGE do município")
        private Integer codigoMunicipio;

        @Size(max = 2)
        @Schema(description = "UF")
        private String uf;

        @Pattern(regexp = "\\d{8}")
        @Schema(description = "CEP (8 dígitos)")
        private String cep;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contato {
        @Size(max = 11)
        @Schema(description = "Telefone")
        private String telefone;

        @Size(max = 80)
        @Schema(description = "E-mail")
        private String email;
    }
}

package br.com.rentafit.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resposta completa do Portal Nacional com detalhes da NFS-e autorizada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfseConsultaResponse {
    private String chaveAcesso;
    private Long numero;
    private String status;
    private OffsetDateTime dhAutorizacao;
    private DadosPrestador prestador;
    private DadosTomador tomador;
    private DadosServico servico;
    private DadosValores valores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosPrestador {
        private String cnpj;
        private String inscricaoMunicipal;
        private String razaoSocial;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosTomador {
        private String cpfCnpj;
        private String nome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosServico {
        private String codigoNbs;
        private String descricao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosValores {
        private BigDecimal valorServico;
        private BigDecimal valorIbs;
        private BigDecimal valorCbs;
        private BigDecimal valorIsqn;
        private BigDecimal valorTotal;
    }
}

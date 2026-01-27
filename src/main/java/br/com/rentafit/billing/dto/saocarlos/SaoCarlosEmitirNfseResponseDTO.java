package br.com.rentafit.billing.dto.saocarlos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da emissão de NFS-e São Carlos")
public class SaoCarlosEmitirNfseResponseDTO {

    @Schema(description = "Número do protocolo de processamento")
    private String protocolo;

    @Schema(description = "Data e hora do recebimento do lote")
    private LocalDateTime dataRecebimento;

    @Schema(description = "Situação do processamento: 1-Não processado, 2-Processado com sucesso, 3-Processado com erro, 4-Processamento cancelado")
    private Integer situacao;

    @Schema(description = "Lista de NFS-e geradas")
    private List<NfseGerada> nfsesGeradas;

    @Schema(description = "Lista de mensagens de erro ou aviso")
    private List<Mensagem> mensagens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NfseGerada {
        @Schema(description = "Número da NFS-e")
        private Long numero;

        @Schema(description = "Código de verificação")
        private String codigoVerificacao;

        @Schema(description = "Data de emissão")
        private LocalDateTime dataEmissao;

        @Schema(description = "Número do RPS correspondente")
        private Long numeroRps;

        @Schema(description = "Série do RPS correspondente")
        private String serieRps;

        @Schema(description = "XML completo da NFS-e")
        private String xmlNfse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mensagem {
        @Schema(description = "Código da mensagem")
        private String codigo;

        @Schema(description = "Descrição da mensagem")
        private String mensagem;

        @Schema(description = "Correção sugerida")
        private String correcao;
    }
}

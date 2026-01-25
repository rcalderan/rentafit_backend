package br.com.rentafit.billing.dto.via;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecepcaoResponse {
    private String protocolo;
    private String codigoRetorno;
    private String mensagem;
    private String versaoValidador;
    private String dataHoraProcessamento;
    private String chaveAcesso;
    private String idEvento;
}
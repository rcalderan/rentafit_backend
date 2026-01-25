package br.com.rentafit.billing.dto.via;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelamentoRequest {
    private String cnpjConcessionaria;
    private String identificador;
    private String eventoViaXmlGZipBase64;
}

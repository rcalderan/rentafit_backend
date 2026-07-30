package br.com.rentafit.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO para construção do XML DPS conforme XSD oficial NFS-e Nacional v1.01.
 *
 * <p>Exemplo: {@code DpsRequest.builder().versao("1.01").infDPS(...).build()}</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpsRequest {
    private String versao;
    private InfDPS infDPS;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfDPS {
        private String id;
        private String tpAmb;
        private OffsetDateTime dhEmi;
        private String verAplic;
        private String serie;
        private String nDPS;
        private LocalDate dCompet;
        private String tpEmit;
        private String cLocEmi;
        private Prestador prest;
        private Tomador toma;
        private Servico serv;
        private Valores valores;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prestador {
        private String CNPJ;
        private String CPF;
        private String IM;
        private RegTrib regTrib;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegTrib {
        private String opSimpNac;
        private String regEspTrib;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tomador {
        private String CNPJ;
        private String CPF;
        private String xNome;
        private Endereco end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        private String cMun;
        private String CEP;
        private String xLgr;
        private String nro;
        private String xBairro;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Servico {
        private LocPrest locPrest;
        private CServ cServ;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocPrest {
        private String cLocPrestacao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CServ {
        private String cTribNac;
        private String xDescServ;
        private String cNBS;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Valores {
        private VServPrest vServPrest;
        private Trib trib;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VServPrest {
        private BigDecimal vServ;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trib {
        private TribMun tribMun;
        private TotTrib totTrib;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TribMun {
        private String tribISSQN;
        private String tpRetISSQN;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotTrib {
        private String indTotTrib;
    }
}

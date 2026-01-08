package br.com.rentafit.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpsRequest {
    private InfDPS infDPS;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfDPS {
        private OffsetDateTime dhEmi;
        private String pEmi;
        private String tpAmb;
        private String verAtu;
        private Prestador prest;
        private Tomador toma;
        private Servico serv;
        private Valores vals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prestador {
        private String CNPJ;
        private String IM;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tomador {
        private Identificacao identif;
        private String nNome;
        private Endereco end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Identificacao {
        private String CNPJ;
        private String CPF;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        private String lograd;
        private String nNum;
        private String cMun;
        private String UF;
        private String CEP;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Servico {
        private LocServ locServ;
        private IdServ idServ;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocServ {
        private String cMunServ;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdServ {
        private String cNBS;
        private String desc;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Valores {
        private BigDecimal vServ;
        private Tributos tribut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tributos {
        private Ibs ibs;
        private Cbs cbs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ibs {
        private BigDecimal pAliq;
        private BigDecimal vIBS;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cbs {
        private BigDecimal pAliq;
        private BigDecimal vCBS;
    }
}

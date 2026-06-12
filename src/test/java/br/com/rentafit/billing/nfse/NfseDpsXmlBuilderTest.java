package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfseDpsXmlBuilder - montagem do XML DPS NT004")
class NfseDpsXmlBuilderTest {

    private final NfseDpsXmlBuilder builder = new NfseDpsXmlBuilder();

    @Test
    @DisplayName("buildXml() gera XML com tag raiz infDPS")
    void buildXml_contemTagInfDPS() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("infDPS");
    }

    @Test
    @DisplayName("buildXml() inclui CNPJ do prestador")
    void buildXml_contemCnpjPrestador() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("00000000000000");
    }

    @Test
    @DisplayName("buildXml() inclui código NBS do serviço")
    void buildXml_contemCodigoNbs() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("1.0101");
    }

    @Test
    @DisplayName("buildXml() inclui valor do serviço")
    void buildXml_contemValorServico() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("500");
    }

    @Test
    @DisplayName("buildXml() lança IllegalArgumentException quando request é null")
    void buildXml_lancaExcecao_requestNull() {
        assertThatThrownBy(() -> builder.buildXml(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buildXml() inclui CPF do tomador quando informado")
    void buildXml_contemCpfTomador() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().getIdentif().setCPF("12345678901");
        String xml = builder.buildXml(req);
        assertThat(xml).contains("12345678901");
    }

    @Test
    @DisplayName("buildXml() inclui endereço quando presente")
    void buildXml_contemEndereco() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().setEnd(DpsRequest.Endereco.builder()
                .lograd("Rua Teste")
                .nNum("123")
                .cMun("3550308")
                .UF("SP")
                .CEP("01234567")
                .build());
        String xml = builder.buildXml(req);
        assertThat(xml).contains("<end>");
        assertThat(xml).contains("Rua Teste");
        assertThat(xml).contains("<UF>SP</UF>");
    }

    @Test
    @DisplayName("buildXml() não inclui IM quando prestador não tem IM")
    void buildXml_semImQuandoNaoInformado() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getPrest().setIM(null);
        String xml = builder.buildXml(req);
        assertThat(xml).doesNotContain("<IM>");
    }

    @Test
    @DisplayName("buildXml() não inclui end quando tomador não tem endereço")
    void buildXml_semEnderecoQuandoNaoInformado() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().setEnd(null);
        String xml = builder.buildXml(req);
        assertThat(xml).doesNotContain("<end>");
    }

    @Test
    @DisplayName("buildXml() escapa caracteres especiais no XML")
    void buildXml_escapaCaracteresEspeciais() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().setNNome("Cliente & Teste <b>");
        String xml = builder.buildXml(req);
        assertThat(xml).contains("&amp;");
        assertThat(xml).contains("&lt;b&gt;");
        assertThat(xml).doesNotContain("<b>");
    }

    @Test
    @DisplayName("buildXml() inclui CNPJ quando informado (sem CPF)")
    void buildXml_contemCnpjQuandoSemCpf() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().getIdentif().setCPF(null);
        req.getInfDPS().getToma().getIdentif().setCNPJ("12345678000199");
        String xml = builder.buildXml(req);
        assertThat(xml).contains("<CNPJ>12345678000199</CNPJ>");
        assertThat(xml).doesNotContain("<CPF>");
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private DpsRequest dpsRequestPadrao() {
        return DpsRequest.builder()
                .infDPS(DpsRequest.InfDPS.builder()
                        .dhEmi(OffsetDateTime.now())
                        .pEmi("1")
                        .tpAmb("2")
                        .verAtu("1.00")
                        .prest(DpsRequest.Prestador.builder()
                                .CNPJ("00000000000000")
                                .IM("123456")
                                .build())
                        .toma(DpsRequest.Tomador.builder()
                                .identif(DpsRequest.Identificacao.builder()
                                        .CPF("98765432100")
                                        .build())
                                .nNome("Cliente Teste")
                                .build())
                        .serv(DpsRequest.Servico.builder()
                                .locServ(DpsRequest.LocServ.builder()
                                        .cMunServ("3550308")
                                        .build())
                                .idServ(DpsRequest.IdServ.builder()
                                        .cNBS("1.0101")
                                        .desc("Locação de traje")
                                        .build())
                                .build())
                        .vals(DpsRequest.Valores.builder()
                                .vServ(BigDecimal.valueOf(500))
                                .tribut(DpsRequest.Tributos.builder()
                                        .ibs(DpsRequest.Ibs.builder()
                                                .pAliq(BigDecimal.valueOf(0.025))
                                                .vIBS(BigDecimal.valueOf(12.50))
                                                .build())
                                        .cbs(DpsRequest.Cbs.builder()
                                                .pAliq(BigDecimal.valueOf(0.015))
                                                .vCBS(BigDecimal.valueOf(7.50))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}

package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfseDpsXmlBuilder - montagem do XML DPS conforme XSD v1.01")
class NfseDpsXmlBuilderTest {

    private final NfseDpsXmlBuilder builder = new NfseDpsXmlBuilder();

    @Test
    @DisplayName("buildXml() gera XML com tag raiz DPS e versao")
    void buildXml_contemTagDpsEVersao() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.01\">");
        assertThat(xml).contains("</DPS>");
    }

    @Test
    @DisplayName("buildXml() inclui infDPS com atributo Id")
    void buildXml_contemInfDpsComId() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<infDPS Id=\"DPS3550308");
    }

    @Test
    @DisplayName("buildXml() inclui campos obrigatórios na ordem do XSD")
    void buildXml_camposObrigatoriosNaOrdemXsd() {
        String xml = builder.buildXml(dpsRequestPadrao());
        int tpAmb = xml.indexOf("<tpAmb>");
        int dhEmi = xml.indexOf("<dhEmi>");
        int verAplic = xml.indexOf("<verAplic>");
        int serie = xml.indexOf("<serie>");
        int nDPS = xml.indexOf("<nDPS>");
        int dCompet = xml.indexOf("<dCompet>");
        int tpEmit = xml.indexOf("<tpEmit>");
        int cLocEmi = xml.indexOf("<cLocEmi>");

        assertThat(tpAmb).isLessThan(dhEmi);
        assertThat(dhEmi).isLessThan(verAplic);
        assertThat(verAplic).isLessThan(serie);
        assertThat(serie).isLessThan(nDPS);
        assertThat(nDPS).isLessThan(dCompet);
        assertThat(dCompet).isLessThan(tpEmit);
        assertThat(tpEmit).isLessThan(cLocEmi);
    }

    @Test
    @DisplayName("buildXml() inclui CNPJ do prestador")
    void buildXml_contemCnpjPrestador() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<CNPJ>00000000000000</CNPJ>");
    }

    @Test
    @DisplayName("buildXml() inclui regTrib do prestador")
    void buildXml_contemRegTrib() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<regTrib>");
        assertThat(xml).contains("<opSimpNac>1</opSimpNac>");
        assertThat(xml).contains("<regEspTrib>0</regEspTrib>");
    }

    @Test
    @DisplayName("buildXml() inclui código NBS do serviço")
    void buildXml_contemCodigoNbs() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<cNBS>1.0101</cNBS>");
    }

    @Test
    @DisplayName("buildXml() inclui cTribNac e xDescServ")
    void buildXml_contemCtribNacXDescServ() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<cTribNac>140201</cTribNac>");
        assertThat(xml).contains("<xDescServ>Locação de traje</xDescServ>");
    }

    @Test
    @DisplayName("buildXml() inclui valor do serviço em vServPrest")
    void buildXml_contemValorServico() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<vServPrest>");
        assertThat(xml).contains("<vServ>500</vServ>");
    }

    @Test
    @DisplayName("buildXml() inclui tribMun e totTrib")
    void buildXml_contemTribMunTotTrib() {
        String xml = builder.buildXml(dpsRequestPadrao());
        assertThat(xml).contains("<tribMun>");
        assertThat(xml).contains("<tribISSQN>1</tribISSQN>");
        assertThat(xml).contains("<tpRetISSQN>1</tpRetISSQN>");
        assertThat(xml).contains("<totTrib>");
        assertThat(xml).contains("<indTotTrib>0</indTotTrib>");
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
        req.getInfDPS().getToma().setCPF("12345678901");
        req.getInfDPS().getToma().setCNPJ(null);
        String xml = builder.buildXml(req);
        assertThat(xml).contains("<CPF>12345678901</CPF>");
        assertThat(xml).doesNotContain("<CNPJ>12345678901</CNPJ>");
    }

    @Test
    @DisplayName("buildXml() inclui endereço quando presente")
    void buildXml_contemEndereco() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().setEnd(DpsRequest.Endereco.builder()
                .cMun("3550308")
                .CEP("01234567")
                .xLgr("Rua Teste")
                .nro("123")
                .xBairro("Centro")
                .build());
        String xml = builder.buildXml(req);
        assertThat(xml).contains("<end>");
        assertThat(xml).contains("<endNac>");
        assertThat(xml).contains("Rua Teste");
        assertThat(xml).contains("<cMun>3550308</cMun>");
        assertThat(xml).contains("<CEP>01234567</CEP>");
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
        req.getInfDPS().getToma().setXNome("Cliente & Teste <b>");
        String xml = builder.buildXml(req);
        assertThat(xml).contains("&amp;");
        assertThat(xml).contains("&lt;b&gt;");
        assertThat(xml).doesNotContain("<b>");
    }

    @Test
    @DisplayName("buildXml() inclui CNPJ quando informado (sem CPF)")
    void buildXml_contemCnpjQuandoSemCpf() {
        DpsRequest req = dpsRequestPadrao();
        req.getInfDPS().getToma().setCNPJ("12345678000199");
        req.getInfDPS().getToma().setCPF(null);
        String xml = builder.buildXml(req);
        assertThat(xml).contains("<CNPJ>12345678000199</CNPJ>");
        assertThat(xml).doesNotContain("<CPF>");
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private DpsRequest dpsRequestPadrao() {
        return DpsRequest.builder()
                .versao("1.01")
                .infDPS(DpsRequest.InfDPS.builder()
                        .id("DPS355030810000000000000000001000000000001")
                        .tpAmb("2")
                        .dhEmi(OffsetDateTime.now())
                        .verAplic("Rentafit-1.00")
                        .serie("00001")
                        .nDPS("000000000000001")
                        .dCompet(LocalDate.now())
                        .tpEmit("1")
                        .cLocEmi("3550308")
                        .prest(DpsRequest.Prestador.builder()
                                .CNPJ("00000000000000")
                                .IM("123456")
                                .regTrib(DpsRequest.RegTrib.builder()
                                        .opSimpNac("1")
                                        .regEspTrib("0")
                                        .build())
                                .build())
                        .toma(DpsRequest.Tomador.builder()
                                .CPF("98765432100")
                                .xNome("Cliente Teste")
                                .build())
                        .serv(DpsRequest.Servico.builder()
                                .locPrest(DpsRequest.LocPrest.builder()
                                        .cLocPrestacao("3550308")
                                        .build())
                                .cServ(DpsRequest.CServ.builder()
                                        .cTribNac("140201")
                                        .xDescServ("Locação de traje")
                                        .cNBS("1.0101")
                                        .build())
                                .build())
                        .valores(DpsRequest.Valores.builder()
                                .vServPrest(DpsRequest.VServPrest.builder()
                                        .vServ(BigDecimal.valueOf(500))
                                        .build())
                                .trib(DpsRequest.Trib.builder()
                                        .tribMun(DpsRequest.TribMun.builder()
                                                .tribISSQN("1")
                                                .tpRetISSQN("1")
                                                .build())
                                        .totTrib(DpsRequest.TotTrib.builder()
                                                .indTotTrib("0")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}

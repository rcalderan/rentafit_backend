package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfeXmlBuilder - montagem do XML NF-e v4.00 (modelo 55)")
class NfeXmlBuilderTest {

    private NfeXmlBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new NfeXmlBuilder();
        ReflectionTestUtils.setField(builder, "emitCnpj", "00000000000191");
        ReflectionTestUtils.setField(builder, "ufCode", "35");
        ReflectionTestUtils.setField(builder, "serie", "1");
        ReflectionTestUtils.setField(builder, "tpAmb", "2");
        ReflectionTestUtils.setField(builder, "emitRazaoSocial", "Emitente Homologacao");
        ReflectionTestUtils.setField(builder, "emitIe", "123456789");
        ReflectionTestUtils.setField(builder, "emitCrt", "3");
        ReflectionTestUtils.setField(builder, "emitLogradouro", "Rua Teste");
        ReflectionTestUtils.setField(builder, "emitNumero", "0");
        ReflectionTestUtils.setField(builder, "emitBairro", "Centro");
        ReflectionTestUtils.setField(builder, "emitMunicipioCodigo", "3550308");
        ReflectionTestUtils.setField(builder, "emitMunicipioNome", "Sao Paulo");
        ReflectionTestUtils.setField(builder, "emitUf", "SP");
        ReflectionTestUtils.setField(builder, "emitCep", "00000000");
        ReflectionTestUtils.setField(builder, "emitPaisCodigo", "1058");
        ReflectionTestUtils.setField(builder, "emitPaisNome", "BRASIL");
        ReflectionTestUtils.setField(builder, "verProc", "1.0");
    }

    @Test
    @DisplayName("buildXml() gera XML com elemento infNFe e Id=NFe{chave}")
    void buildXml_contemInfNFeEId() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("infNFe");
        assertThat(xml).contains("Id=\"NFe");
        assertThat(xml).contains("<mod>55</mod>");
    }

    @Test
    @DisplayName("buildXml() preenche ide com campos obrigatórios")
    void buildXml_contemIdeCompleta() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<cUF>35</cUF>");
        assertThat(xml).contains("<nNF>");
        assertThat(xml).contains("<cNF>");
        assertThat(xml).contains("<tpAmb>2</tpAmb>");
        assertThat(xml).contains("<finNFe>1</finNFe>");
        assertThat(xml).contains("<indFinal>0</indFinal>");
        assertThat(xml).contains("<indPres>0</indPres>");
        assertThat(xml).contains("<procEmi>0</procEmi>");
        assertThat(xml).contains("<verProc>1.0</verProc>");
    }

    @Test
    @DisplayName("buildXml() preenche emit com CNPJ, razão social, endereço, IE e CRT")
    void buildXml_contemEmitCompleto() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("00000000000191");
        assertThat(xml).contains("<xNome>Emitente Homologacao</xNome>");
        assertThat(xml).contains("<enderEmit>");
        assertThat(xml).contains("<IE>123456789</IE>");
        assertThat(xml).contains("<CRT>3</CRT>");
    }

    @Test
    @DisplayName("buildXml() preenche dest com documento, nome, endereço e indIEDest")
    void buildXml_contemDestCompleto() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("12345678901");
        assertThat(xml).contains("<xNome>Cliente Teste</xNome>");
        assertThat(xml).contains("<enderDest>");
        assertThat(xml).contains("<indIEDest>9</indIEDest>");
    }

    @Test
    @DisplayName("buildXml() gera det com prod, imposto ICMS00, PIS e COFINS")
    void buildXml_contemDetComImpostos() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<det nItem=\"1\">");
        assertThat(xml).contains("<ICMS00>");
        assertThat(xml).contains("<PISNT>");
        assertThat(xml).contains("<COFINSNT>");
    }

    @Test
    @DisplayName("buildXml() gera total com ICMSTot e vNF igual ao total dos produtos")
    void buildXml_contemTotal() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<ICMSTot>");
        assertThat(xml).contains("<vProd>100.00</vProd>");
        assertThat(xml).contains("<vNF>100.00</vNF>");
    }

    @Test
    @DisplayName("buildXml() gera transp e pag")
    void buildXml_contemTranspEPag() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<transp><modFrete>9</modFrete></transp>");
        assertThat(xml).contains("<pag>");
        assertThat(xml).contains("<detPag>");
    }

    @Test
    @DisplayName("buildXml() gera vPag 0.00 quando meio de pagamento for sem pagamento (tPag=90)")
    void buildXml_vPagZero_quandoSemPagamento() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<tPag>90</tPag>");
        assertThat(xml).contains("<vPag>0.00</vPag>");
    }

    @Test
    @DisplayName("buildXml() inclui natureza da operação")
    void buildXml_contemNaturezaOperacao() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("Venda de mercadoria");
    }

    @Test
    @DisplayName("buildXml() lança IllegalArgumentException quando request é null")
    void buildXml_lancaExcecao_requestNull() {
        assertThatThrownBy(() -> builder.buildXml(null, clientePadrao()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buildXml() lança IllegalArgumentException quando customer é null")
    void buildXml_lancaExcecao_customerNull() {
        assertThatThrownBy(() -> builder.buildXml(requestPadrao(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buildXml() lança IllegalArgumentException quando lista de itens é vazia")
    void buildXml_lancaExcecao_itensVazio() {
        NfeEmissionRequest req = requestPadrao();
        req.setItems(List.of());
        assertThatThrownBy(() -> builder.buildXml(req, clientePadrao()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private NfeEmissionRequest requestPadrao() {
        return NfeEmissionRequest.builder()
                .customerId(UUID.randomUUID())
                .natureOperation("Venda de mercadoria")
                .origin("SALES")
                .originId(UUID.randomUUID())
                .items(List.of(NfeItemRequest.builder()
                        .productCode("PROD-001")
                        .description("Camiseta")
                        .ncm("61091000")
                        .cfop("5102")
                        .unit("UN")
                        .quantity(BigDecimal.valueOf(2))
                        .unitValue(BigDecimal.valueOf(50))
                        .build()))
                .build();
    }

    private Customer clientePadrao() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Cliente Teste");
        c.setDocument("12345678901");
        return c;
    }
}

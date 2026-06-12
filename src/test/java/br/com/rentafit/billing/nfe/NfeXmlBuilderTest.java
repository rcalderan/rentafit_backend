package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfeXmlBuilder - montagem do XML infNFe (modelo 55)")
class NfeXmlBuilderTest {

    private final NfeXmlBuilder builder = new NfeXmlBuilder();

    @Test
    @DisplayName("buildXml() gera XML com elemento infNFe")
    void buildXml_contemInfNFe() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("infNFe");
    }

    @Test
    @DisplayName("buildXml() inclui atributo Id no formato NFe{chave}")
    void buildXml_contemIdComPrefixoNFe() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("Id=\"NFe");
    }

    @Test
    @DisplayName("buildXml() define modelo 55")
    void buildXml_contemModelo55() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("<mod>55</mod>");
    }

    @Test
    @DisplayName("buildXml() inclui CNPJ do emitente")
    void buildXml_contemCnpjEmitente() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("00000000000191");
    }

    @Test
    @DisplayName("buildXml() inclui documento do destinatário")
    void buildXml_contemDocumentoDestinatario() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("12345678901");
    }

    @Test
    @DisplayName("buildXml() inclui NCM e CFOP do item")
    void buildXml_contemNcmCfop() {
        String xml = builder.buildXml(requestPadrao(), clientePadrao());
        assertThat(xml).contains("61091000").contains("5102");
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

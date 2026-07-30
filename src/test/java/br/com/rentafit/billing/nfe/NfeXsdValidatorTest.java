package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfeXsdValidator - validação contra schema oficial NF-e v4.00")
class NfeXsdValidatorTest {

    private final NfeXsdValidator validator = new NfeXsdValidator();

    @Test
    @DisplayName("validate() rejeita XML gerado pelo builder apenas por falta de assinatura")
    void validate_xmlGeradoPeloBuilder_rejeicaoApenasPorAssinatura() {
        String xml = buildXmlValido();
        assertThatThrownBy(() -> validator.validate(xml))
                .isInstanceOf(NfeValidationException.class)
                .satisfies(e -> {
                    String message = e.getMessage();
                    assertThat(message).contains("rejeitado pelo schema");
                    assertThat(message).containsIgnoringCase("signature");
                    assertThat(message).doesNotContain("cvc-pattern-valid");
                    assertThat(message).doesNotContain("cvc-type.3.1.3");
                    assertThat(message).doesNotContain("cvc-complex-type.2.4.a");
                });
    }

    @Test
    @DisplayName("validate() lança quando XML não segue o schema oficial")
    void validate_xmlIncompleto_rejeitaPeloSchema() {
        String xmlIncompleto =
                "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                        + "<infNFe Id=\"NFe35200000000000000191550010000000011000000010\">"
                        + "<ide><mod>55</mod></ide>"
                        + "<emit><CNPJ>00000000000191</CNPJ></emit>"
                        + "<dest><CPF>12345678901</CPF></dest>"
                        + "</infNFe></NFe>";

        assertThatThrownBy(() -> validator.validate(xmlIncompleto))
                .isInstanceOf(NfeValidationException.class)
                .hasMessageContaining("rejeitado pelo schema");
    }

    @Test
    @DisplayName("validate() lança IllegalArgumentException para XML nulo")
    void validate_lancaExcecao_xmlNulo() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validate() lança IllegalArgumentException para XML vazio")
    void validate_lancaExcecao_xmlVazio() {
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validate() lança quando XML é malformado")
    void validate_lancaExcecao_xmlMalformado() {
        assertThatThrownBy(() -> validator.validate("<NFe><infNFe></NFe>"))
                .isInstanceOf(NfeValidationException.class);
    }

    @Test
    @DisplayName("validate() lança quando infNFe está ausente")
    void validate_lancaExcecao_infNFeAusente() {
        String semInfNFe = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><outro/></NFe>";
        assertThatThrownBy(() -> validator.validate(semInfNFe))
                .isInstanceOf(NfeValidationException.class);
    }

    @Test
    @DisplayName("validate() lança quando atributo Id do infNFe está ausente")
    void validate_lancaExcecao_idAusente() {
        String semId = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe><ide><mod>55</mod></ide></infNFe></NFe>";
        assertThatThrownBy(() -> validator.validate(semId))
                .isInstanceOf(NfeValidationException.class);
    }

    private String buildXmlValido() {
        NfeXmlBuilder builder = new NfeXmlBuilder();
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

        NfeEmissionRequest request = NfeEmissionRequest.builder()
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

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Cliente Teste");
        customer.setDocument("12345678901");
        return builder.buildXml(request, customer);
    }
}

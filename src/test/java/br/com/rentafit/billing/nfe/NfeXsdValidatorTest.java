package br.com.rentafit.billing.nfe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfeXsdValidator - validação estrutural do XML NF-e")
class NfeXsdValidatorTest {

    private final NfeXsdValidator validator = new NfeXsdValidator();

    private static final String XML_VALIDO =
            "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
            + "<infNFe Id=\"NFe35200000000000000191550010000000011000000010\">"
            + "<ide><mod>55</mod></ide>"
            + "<emit><CNPJ>00000000000191</CNPJ></emit>"
            + "<dest><CPF>12345678901</CPF></dest>"
            + "</infNFe></NFe>";

    @Test
    @DisplayName("validate() não lança para XML com infNFe, emit e dest")
    void validate_xmlValido_naoLanca() {
        assertThatCode(() -> validator.validate(XML_VALIDO)).doesNotThrowAnyException();
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
}

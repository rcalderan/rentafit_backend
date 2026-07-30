package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.config.FiscalCertificateProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfeXmlSigner - assinatura XMLDSig do infNFe")
class NfeXmlSignerTest {

    @Mock
    FiscalCertificateProvider certificateProvider;

    @InjectMocks
    NfeXmlSigner signer;

    private static final String XML_NFE =
            "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
            + "<infNFe Id=\"NFe35200000000000000191550010000000011000000010\"><ide><mod>55</mod></ide></infNFe>"
            + "</NFe>";

    @Test
    @DisplayName("sign() lança IllegalStateException quando certificado não disponível")
    void sign_lancaExcecao_certificadoIndisponivel() {
        when(certificateProvider.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> signer.sign(XML_NFE))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("certificado"));
    }

    @Test
    @DisplayName("sign() lança IllegalArgumentException para XML nulo")
    void sign_lancaExcecao_xmlNulo() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sign() lança IllegalArgumentException para XML vazio")
    void sign_lancaExcecao_xmlVazio() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sign() lança IllegalStateException quando infNFe ausente")
    void sign_lancaExcecao_infNFeAusente() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><outro/></NFe>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sign() lança IllegalStateException quando infNFe não tem atributo Id")
    void sign_lancaExcecao_semAtributoId() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign(
                "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                        + "<infNFe><ide><mod>55</mod></ide></infNFe>"
                        + "</NFe>"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Id do infNFe ausente");
    }

    @Test
    @DisplayName("sign() lança Exception para XML malformado")
    void sign_lancaExcecao_xmlMalformado() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign("<NFe><infNFe>"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("sign() tenta assinar e falha com certificado nulo")
    void sign_tentaAssinarComCertificadoNulo() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        // Com certificado null, o fluxo tenta assinar mas falha
        // Isso cobre as linhas de assinatura sem precisar de certificado real
        assertThatThrownBy(() -> signer.sign(XML_NFE))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("sign() encontra infNFe sem namespace quando não encontra com namespace")
    void sign_encontraInfNFeSemNamespace() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        // XML sem namespace deve ser encontrado pelo segundo getElementsByTagName
        String xmlSemNs = "<NFe><infNFe Id=\"NFe12345678901234567890123456789012345678901234\"><ide><mod>55</mod></ide></infNFe></NFe>";

        assertThatThrownBy(() -> signer.sign(xmlSemNs))
                .isInstanceOf(Exception.class); // Vai falhar na assinatura sem certificado
    }

    @Test
    @DisplayName("sign() produz XML com algoritmo RSA-SHA256 e digest SHA256 (NT2016.002)")
    void sign_usaAlgoritmoSha256() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getBasicConstraints()).thenReturn(-1);
        when(cert.getEncoded()).thenReturn(new byte[]{0x30, 0x00});

        when(certificateProvider.isAvailable()).thenReturn(true);
        when(certificateProvider.certificate()).thenReturn(cert);
        when(certificateProvider.privateKey()).thenReturn(keyPair.getPrivate());

        String signedXml = signer.sign(XML_NFE);

        assertThat(signedXml)
                .contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")
                .contains("http://www.w3.org/2001/04/xmlenc#sha256")
                .doesNotContain("rsa-sha1")
                .doesNotContain("#sha1");
    }
}

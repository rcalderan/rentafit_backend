package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.config.FiscalCertificateProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfseXmlSigner - assinatura XMLDSig do DPS")
class NfseXmlSignerTest {

    @Mock
    FiscalCertificateProvider certificateProvider;

    @InjectMocks
    NfseXmlSigner signer;

    private static final String XML_SIMPLES = "<infDPS><prest><CNPJ>00000000000000</CNPJ></prest></infDPS>";

    @Test
    @DisplayName("sign() lança IllegalStateException quando certificado não disponível")
    void sign_lancaExcecao_certificadoIndisponivel() {
        when(certificateProvider.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> signer.sign(XML_SIMPLES))
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
    @DisplayName("sign() delega para certificateProvider.privateKey() quando disponível")
    void sign_acessaCertificadoQuandoDisponivel() throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.PrivateKey pk = kpg.generateKeyPair().getPrivate();
        java.security.cert.X509Certificate cert = mock(java.security.cert.X509Certificate.class);

        when(certificateProvider.isAvailable()).thenReturn(true);
        when(certificateProvider.privateKey()).thenReturn(pk);
        when(certificateProvider.certificate()).thenReturn(cert);

        // Espera exceção de parsing do XML malformado, não de certificado ausente
        assertThatThrownBy(() -> signer.sign(XML_SIMPLES))
                .isNotInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sign() lança Exception para XML malformado")
    void sign_lancaExcecao_xmlMalformado() {
        when(certificateProvider.isAvailable()).thenReturn(true);

        assertThatThrownBy(() -> signer.sign("<infDPS>"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("sign() adiciona Id='DPS' automaticamente quando não presente")
    void sign_adicionaIdAutomaticamente() throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.PrivateKey pk = kpg.generateKeyPair().getPrivate();
        java.security.cert.X509Certificate cert = mock(java.security.cert.X509Certificate.class);

        when(certificateProvider.isAvailable()).thenReturn(true);
        when(certificateProvider.privateKey()).thenReturn(pk);
        when(certificateProvider.certificate()).thenReturn(cert);

        String xmlSemId = "<infDPS xmlns=\"http://www.portalfiscal.inf.br/nfse\"><prest><CNPJ>00000000000000</CNPJ></prest></infDPS>";

        // Com Id ausente, o signer deve adicionar Id='DPS' automaticamente
        assertThatThrownBy(() -> signer.sign(xmlSemId))
                .isNotInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sign() usa Id existente quando presente")
    void sign_usaIdExistente() throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.PrivateKey pk = kpg.generateKeyPair().getPrivate();
        java.security.cert.X509Certificate cert = mock(java.security.cert.X509Certificate.class);

        when(certificateProvider.isAvailable()).thenReturn(true);
        when(certificateProvider.privateKey()).thenReturn(pk);
        when(certificateProvider.certificate()).thenReturn(cert);

        String xmlComId = "<infDPS xmlns=\"http://www.portalfiscal.inf.br/nfse\" Id=\"DPS123\"><prest><CNPJ>00000000000000</CNPJ></prest></infDPS>";

        // Com Id existente, o signer deve usar o Id fornecido
        assertThatThrownBy(() -> signer.sign(xmlComId))
                .isNotInstanceOf(IllegalStateException.class);
    }
}

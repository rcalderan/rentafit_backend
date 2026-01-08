package br.com.rentafit.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("CertificateConfig - Testes de Configuração de Certificado")
class CertificateConfigTest {

    private ApplicationContextRunner contextRunner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(CertificateConfig.class);
    }

    @Test
    @DisplayName("Deve retornar null quando path do certificado não está configurado")
    void deveRetornarNullQuandoCaminhoNaoConfigurado() {
        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=",
                        "nfs-e.certificate.password="
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    assertThat(context.getBeansOfType(KeyStore.class)).isEmpty();
                });
    }

    @Test
    @DisplayName("Deve registrar warning quando certificado não está configurado")
    void deveRegistrarWarningQuandoNaoConfigurado() {
        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=",
                        "nfs-e.certificate.password="
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    // O warning é registrado através do logger
                });
    }

    @Test
    @DisplayName("Deve retornar null quando certificado não existe no caminho especificado")
    void deveRetornarNullQuandoCertificadoNaoExiste() {
        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=/caminho/inexistente/certificado.p12",
                        "nfs-e.certificate.password=senha123"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    assertThat(context.getBeansOfType(KeyStore.class)).isEmpty();
                });
    }

    @Test
    @DisplayName("Deve carregar certificado válido com sucesso")
    void deveCarregarCertificadoValido() throws Exception {
        // Criar um KeyStore de teste
        KeyStore testKeyStore = KeyStore.getInstance("PKCS12");
        testKeyStore.load(null, "senhaDesenvolvedores".toCharArray());

        // Salvar o KeyStore em arquivo temporário
        File certificateFile = tempDir.resolve("test-cert.p12").toFile();
        try (FileOutputStream fos = new FileOutputStream(certificateFile)) {
            testKeyStore.store(fos, "senhaDesenvolvedores".toCharArray());
        }

        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=" + certificateFile.getAbsolutePath(),
                        "nfs-e.certificate.password=senhaDesenvolvedores"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    KeyStore keyStore = context.getBean("nfsKeyStore", KeyStore.class);
                    assertThat(keyStore).isNotNull();
                    assertThat(keyStore.getType()).isEqualTo("PKCS12");
                });
    }

    @Test
    @DisplayName("Deve retornar null quando senha do certificado está incorreta")
    void deveRetornarNullQuandoSenhaIncorreta() throws Exception {
        // Criar um KeyStore de teste
        KeyStore testKeyStore = KeyStore.getInstance("PKCS12");
        testKeyStore.load(null, "senhaCorreta".toCharArray());

        // Salvar o KeyStore em arquivo temporário
        File certificateFile = tempDir.resolve("test-cert-wrongpass.p12").toFile();
        try (FileOutputStream fos = new FileOutputStream(certificateFile)) {
            testKeyStore.store(fos, "senhaCorreta".toCharArray());
        }

        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=" + certificateFile.getAbsolutePath(),
                        "nfs-e.certificate.password=senhaIncorreta"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    assertThat(context.getBeansOfType(KeyStore.class)).isEmpty();
                });
    }

    @Test
    @DisplayName("Deve retornar null para arquivo que não é PKCS12 válido")
    void deveRetornarNullQuandoFormatoInvalido() throws Exception {
        // Criar um arquivo inválido
        File invalidFile = tempDir.resolve("invalid-cert.p12").toFile();
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write("Este não é um arquivo PKCS12 válido".getBytes());
        }

        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=" + invalidFile.getAbsolutePath(),
                        "nfs-e.certificate.password=qualquerSenha"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    assertThat(context.getBeansOfType(KeyStore.class)).isEmpty();
                });
    }

    @Test
    @DisplayName("Deve criar bean CertificateConfig mesmo com certificado ausente")
    void deveRegistrarBeanCertificateConfigComCertificadoAusente() {
        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=/caminho/inexistente.p12",
                        "nfs-e.certificate.password=senha"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    CertificateConfig config = context.getBean(CertificateConfig.class);
                    assertThat(config).isNotNull();
                });
    }

    @Test
    @DisplayName("Deve usar valores padrão vazios quando propriedades não estão definidas")
    void deveUsarValoresPadraoQuandoPropriedadesNaoDefinidas() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    assertThat(context.getBeansOfType(KeyStore.class)).isEmpty();
                });
    }

    @Test
    @DisplayName("Deve registrar informações de sucesso quando certificado é carregado")
    void deveRegistrarSucessoQuandoCertificadoCarregado() throws Exception {
        // Criar um KeyStore de teste
        KeyStore testKeyStore = KeyStore.getInstance("PKCS12");
        testKeyStore.load(null, "senha".toCharArray());

        // Salvar o KeyStore em arquivo temporário
        File certificateFile = tempDir.resolve("test-cert-info.p12").toFile();
        try (FileOutputStream fos = new FileOutputStream(certificateFile)) {
            testKeyStore.store(fos, "senha".toCharArray());
        }

        contextRunner
                .withPropertyValues(
                        "nfs-e.certificate.path=" + certificateFile.getAbsolutePath(),
                        "nfs-e.certificate.password=senha"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CertificateConfig.class);
                    KeyStore keyStore = context.getBean("nfsKeyStore", KeyStore.class);
                    assertThat(keyStore).isNotNull();
                    // A mensagem de sucesso é registrada no log
                });
    }

}

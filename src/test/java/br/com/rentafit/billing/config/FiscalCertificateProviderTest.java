package br.com.rentafit.billing.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FiscalCertificateProvider - carregamento de certificado A1")
class FiscalCertificateProviderTest {

    @TempDir
    Path tempDir;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(FiscalCertificateProvider.class);
    }

    @Test
    @DisplayName("Bean é criado mesmo sem certificado configurado")
    void beanCriado_semCertificado() {
        runner()
                .withPropertyValues("nfs-e.certificate.path=", "nfs-e.certificate.password=")
                .run(ctx -> assertThat(ctx).hasSingleBean(FiscalCertificateProvider.class));
    }

    @Test
    @DisplayName("keyStore() retorna null quando path não configurado")
    void keyStore_retornaNull_pathVazio() {
        runner()
                .withPropertyValues("nfs-e.certificate.path=", "nfs-e.certificate.password=")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.keyStore()).isNull();
                });
    }

    @Test
    @DisplayName("keyStore() retorna null quando arquivo não existe")
    void keyStore_retornaNull_arquivoInexistente() {
        runner()
                .withPropertyValues(
                        "nfs-e.certificate.path=/nao/existe/cert.p12",
                        "nfs-e.certificate.password=qualquer")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.keyStore()).isNull();
                });
    }

    @Test
    @DisplayName("keyStore() carrega PKCS12 válido e retorna non-null")
    void keyStore_carregaCertificadoValido() throws Exception {
        File p12 = criarP12Vazio(tempDir, "test.p12", "changeit");

        runner()
                .withPropertyValues(
                        "nfs-e.certificate.path=" + p12.getAbsolutePath(),
                        "nfs-e.certificate.password=changeit")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    KeyStore ks = provider.keyStore();
                    assertThat(ks).isNotNull();
                    assertThat(ks.getType()).isEqualTo("PKCS12");
                });
    }

    @Test
    @DisplayName("privateKey() retorna null quando não há entrada no keystore")
    void privateKey_retornaNull_keystoreVazio() throws Exception {
        File p12 = criarP12Vazio(tempDir, "empty.p12", "changeit");

        runner()
                .withPropertyValues(
                        "nfs-e.certificate.path=" + p12.getAbsolutePath(),
                        "nfs-e.certificate.password=changeit")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.privateKey()).isNull();
                });
    }

    @Test
    @DisplayName("certificate() retorna null quando não há entrada no keystore")
    void certificate_retornaNull_keystoreVazio() throws Exception {
        File p12 = criarP12Vazio(tempDir, "empty2.p12", "changeit");

        runner()
                .withPropertyValues(
                        "nfs-e.certificate.path=" + p12.getAbsolutePath(),
                        "nfs-e.certificate.password=changeit")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.certificate()).isNull();
                });
    }

    @Test
    @DisplayName("isAvailable() retorna false quando path não configurado")
    void isAvailable_retornaFalse_semPath() {
        runner()
                .withPropertyValues("nfs-e.certificate.path=", "nfs-e.certificate.password=")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.isAvailable()).isFalse();
                });
    }

    @Test
    @DisplayName("isAvailable() retorna true quando PKCS12 carregado")
    void isAvailable_retornaTrue_certValido() throws Exception {
        File p12 = criarP12Vazio(tempDir, "avail.p12", "changeit");

        runner()
                .withPropertyValues(
                        "nfs-e.certificate.path=" + p12.getAbsolutePath(),
                        "nfs-e.certificate.password=changeit")
                .run(ctx -> {
                    FiscalCertificateProvider provider = ctx.getBean(FiscalCertificateProvider.class);
                    assertThat(provider.isAvailable()).isTrue();
                });
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static File criarP12Vazio(Path dir, String nome, String senha) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, senha.toCharArray());
        File f = dir.resolve(nome).toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            ks.store(fos, senha.toCharArray());
        }
        return f;
    }
}

package br.com.rentafit.billing.config;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Gera um arquivo cacerts.jks com os certificados das CAs dos servidores SEFAZ/SEFIN.
 *
 * <p>Segue o padrão de Caelum Stella (NFeBuildAllCacerts) e amaica/nfse2
 * (NFSeCadeiaCertificados): conecta a cada servidor SEFAZ via TLS, extrai a
 * cadeia de certificados apresentada e adiciona no truststore JKS.</p>
 *
 * <p>Uso típico:</p>
 * <pre>{@code
 * SefazCacertsGenerator.gerar("sefaz-cacerts.jks", "changeit");
 * // depois configure: nfs-e.truststore.path=sefaz-cacerts.jks
 * }</pre>
 */
@Slf4j
public class SefazCacertsGenerator {

    private static final int TIMEOUT_SEGUNDOS = 30;
    private static final String TIPO_KEYSTORE = "JKS";

    private SefazCacertsGenerator() {
    }

    /**
     * Gera o cacerts na raiz do projeto com senha padrão "changeit".
     *
     * <p>Uso: {@code mvnw exec:java -Dexec.mainClass="br.com.rentafit.billing.config.SefazCacertsGenerator"}</p>
     */
    public static void main(String[] args) {
        String caminho = args.length > 0 ? args[0] : "sefaz-cacerts.jks";
        String senha = args.length > 1 ? args[1] : "changeit";
        gerar(caminho, senha);
    }

    /**
     * Servidores SEFAZ/SEFIN de homologação e produção.
     */
    private static final List<String> SERVIDORES_SEFAZ = List.of(
            "homologacao.nfe.fazenda.sp.gov.br",
            "hom.nfe.fazenda.gov.br",
            "hom.sefazvirtual.fazenda.gov.br",
            "homologacao.nfe.sefaz.rs.gov.br",
            "homologacao.nfe.sefazvirtual.rs.gov.br",
            "hom-nfse.svrs.rs.gov.br",
            "sefin.producaorestrita.nfse.gov.br",
            "sefin.nfse.gov.br",
            "adn.producaorestrita.nfse.gov.br",
            "adn.nfse.gov.br"
    );

    /**
     * Gera um arquivo cacerts.jks com a cadeia de certificados dos servidores SEFAZ/SEFIN.
     *
     * @param caminhoSaida caminho do arquivo JKS a ser criado
     * @param senha        senha do truststore
     */
    public static void gerar(String caminhoSaida, String senha) {
        try {
            KeyStore ks = carregarOuCriarKeyStore(caminhoSaida, senha);

            for (String host : SERVIDORES_SEFAZ) {
                extrairCadeiaServidor(host, 443, ks, senha.toCharArray());
            }

            salvarKeyStore(ks, caminhoSaida, senha.toCharArray());
            log.info("Cacerts gerado em: {} ({} entradas)", caminhoSaida, contarEntradas(ks));

        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar cacerts: " + e.getMessage(), e);
        }
    }

    private static void extrairCadeiaServidor(String host, int porta, KeyStore ks, char[] senha) {
        SavingTrustManager tm = null;
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Usa cacerts padrão do JDK para o SavingTrustManager capturar a cadeia
            // mesmo quando o keystore destino está vazio (primeira geração).
            tmf.init((KeyStore) null);
            X509TrustManager defaultTm = (X509TrustManager) tmf.getTrustManagers()[0];
            tm = new SavingTrustManager(defaultTm);
            context.init(null, new TrustManager[]{tm}, null);

            SSLSocketFactory factory = context.getSocketFactory();
            log.info("Conectando a {}:{} para extrair cadeia...", host, porta);

            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, porta)) {
                socket.setSoTimeout(TIMEOUT_SEGUNDOS * 1000);
                socket.startHandshake();
                log.info("{}: certificado já confiável.", host);
            }
        } catch (Exception e) {
            log.debug("{}: handshake falhou (esperado para CAs não confiáveis): {}", host, e.getMessage());
        }

        // Processa a cadeia capturada independentemente do resultado do handshake
        if (tm == null || tm.chain == null) {
            log.warn("{}: não foi possível obter a cadeia de certificados.", host);
            return;
        }

        adicionarCadeiaAoKeystore(host, tm.chain, ks);
    }

    private static void adicionarCadeiaAoKeystore(String host, X509Certificate[] chain, KeyStore ks) {
        try {
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = chain[i];
                String alias = host + "-" + i;
                if (ks.getCertificate(alias) == null) {
                    ks.setCertificateEntry(alias, cert);
                    log.info("{}: adicionado certificado {} (alias={})", host, cert.getSubjectDN(), alias);
                }
            }
        } catch (Exception e) {
            log.warn("{}: erro ao adicionar certificado ao keystore: {}", host, e.getMessage());
        }
    }

    private static KeyStore carregarOuCriarKeyStore(String caminho, String senha) throws Exception {
        KeyStore ks = KeyStore.getInstance(TIPO_KEYSTORE);
        File file = new File(caminho);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                ks.load(fis, senha.toCharArray());
            }
            log.info("Cacerts existente carregado: {}", caminho);
        } else {
            ks.load(null, senha.toCharArray());
            log.info("Novo cacerts será criado: {}", caminho);
        }
        return ks;
    }

    private static void salvarKeyStore(KeyStore ks, String caminho, char[] senha) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(caminho)) {
            ks.store(fos, senha);
        }
    }

    private static int contarEntradas(KeyStore ks) throws Exception {
        int count = 0;
        var aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            aliases.nextElement();
            count++;
        }
        return count;
    }

    /**
     * TrustManager decorador que captura a cadeia do servidor antes da validação.
     */
    private static class SavingTrustManager implements X509TrustManager {
        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
            this.chain = chain;
            this.tm.checkServerTrusted(chain, authType);
        }
    }
}

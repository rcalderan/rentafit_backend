package br.com.rentafit.billing;

import br.com.rentafit.billing.dto.via.RecepcaoRequest;
import br.com.rentafit.billing.util.NfseViaUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

@SpringBootTest
public class GenerateViaSampleRequest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeyStore nfsKeyStore;

    @Value("${nfs-e.certificate.password}")
    private String certificatePassword;

    @Test
    public void generateJson() throws Exception {
        String cnpj = "00000000000191";
        String id = "TESTE-12345";

        // 1. Gerar XML base (sem assinatura)
        String xml = NfseViaUtil.generateSampleXml(cnpj, id);

        // 2. Extrair Chave Privada e Certificado do KeyStore para assinar
        String alias = "";
        Enumeration<String> aliases = nfsKeyStore.aliases();
        if (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
        }

        PrivateKey privateKey = (PrivateKey) nfsKeyStore.getKey(alias, certificatePassword.toCharArray());
        X509Certificate cert = (X509Certificate) nfsKeyStore.getCertificate(alias);

        // O Id do elemento a ser assinado no generateSampleXml é fixo para o teste
        String targetId = "NFS35503082000000000001910000000000001260101234567891";

        // 3. Assinar o XML Digitalmente (XMLDSig)
        String signedXml = NfseViaUtil.signXml(xml, targetId, privateKey, cert);

        // 4. Comprimir e codificar (GZip + Base64)
        String base64Gzip = NfseViaUtil.compressAndEncode(signedXml);

        RecepcaoRequest request = RecepcaoRequest.builder()
                .cnpjConcessionaria(cnpj)
                .identificador(id)
                .notaFiscalViaXmlGZipBase64(base64Gzip)
                .build();

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

        System.out.println("----- JSON PARA TESTE NO POSTMAN (CONTEÚDO ASSINADO COM A1) -----");
        System.out.println(json);
        System.out.println("------------------------------------------------------------------");
    }
}

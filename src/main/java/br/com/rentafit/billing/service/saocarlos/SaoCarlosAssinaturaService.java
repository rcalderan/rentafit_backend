package br.com.rentafit.billing.service.saocarlos;

import br.com.rentafit.config.CertificateConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaoCarlosAssinaturaService {

    private final CertificateConfig certificateConfig;

    @Value("${nfs-e.certificate.password}")
    private String certificatePassword;

    /**
     * Assina digitalmente um XML utilizando o padrão XMLDSig (Enveloped).
     * Segue a mesma implementação do NfseViaUtil.signXml
     *
     * @param xmlStr      Conteúdo XML a ser assinado (String)
     * @param targetId    O atributo 'Id' do elemento a ser assinado (ex: "lote123")
     * @return XML assinado ou null em caso de erro
     */
    public String assinarXmlString(String xmlStr, String targetId) {
        log.info("Assinando XML com targetId: {}", targetId);

        try {
            // Parse do XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));

            // Obter certificado e chave privada
            KeyStore keyStore = certificateConfig.nfsKeyStore();
            if (keyStore == null) {
                throw new IllegalStateException("Certificado digital não configurado");
            }

            PrivateKey privateKey = null;
            X509Certificate certificate = null;

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    privateKey = (PrivateKey) keyStore.getKey(alias, certificatePassword.toCharArray());
                    certificate = (X509Certificate) keyStore.getCertificate(alias);
                    log.debug("Certificado encontrado com alias: {}", alias);
                    break;
                }
            }

            if (privateKey == null || certificate == null) {
                throw new IllegalStateException("Não foi possível obter chave privada ou certificado do KeyStore");
            }

            // Factory para assinatura
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // Transforms (Enveloped + Inclusive Canonicalization)
            List<Transform> transforms = new ArrayList<>();
            transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
            transforms.add(fac.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null));

            // Reference
            Reference ref = fac.newReference(
                    "#" + targetId,
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    transforms,
                    null,
                    null
            );

            // SignedInfo
            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(ref)
            );

            // KeyInfo com certificado X509
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            List<Object> x509Content = new ArrayList<>();
            x509Content.add(certificate);
            X509Data xd = kif.newX509Data(x509Content);
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

            // Sign Context
            DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());

            // Registrar o atributo Id
            NodeList nodes = doc.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
                if (element.hasAttribute("Id") && element.getAttribute("Id").equals(targetId)) {
                    element.setIdAttribute("Id", true);
                    log.debug("Id attribute registrado para elemento: {}", element.getNodeName());
                }
            }

            // Assinar
            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);

            // Converter Document para String
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(new DOMSource(doc), new StreamResult(os));

            String signedXml = os.toString(StandardCharsets.UTF_8);
            log.info("XML assinado com sucesso. Tamanho: {} bytes", signedXml.length());

            return signedXml;

        } catch (Exception e) {
            log.error("Erro ao assinar XML: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Assina digitalmente um Document XML.
     *
     * @param doc Documento XML a ser assinado
     * @param elementId ID do elemento a ser assinado (ex: "lote123")
     * @return Documento XML assinado
     */
    public Document assinarXmlDocument(Document doc, String elementId) throws Exception {
        log.info("Assinando Document com elementId: {}", elementId);

        // Obter certificado e chave privada
        KeyStore keyStore = certificateConfig.nfsKeyStore();
        if (keyStore == null) {
            throw new IllegalStateException("Certificado digital não configurado");
        }

        PrivateKey privateKey = null;
        X509Certificate certificate = null;

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                privateKey = (PrivateKey) keyStore.getKey(alias, certificatePassword.toCharArray());
                certificate = (X509Certificate) keyStore.getCertificate(alias);
                log.debug("Certificado encontrado com alias: {}", alias);
                break;
            }
        }

        if (privateKey == null || certificate == null) {
            throw new IllegalStateException("Não foi possível obter chave privada ou certificado do KeyStore");
        }

        // Factory para assinatura
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Transforms (Enveloped + Inclusive Canonicalization)
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null));

        // Reference
        Reference ref = fac.newReference(
                "#" + elementId,
                fac.newDigestMethod(DigestMethod.SHA1, null),
                transforms,
                null,
                null
        );

        // SignedInfo
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref)
        );

        // KeyInfo com certificado X509
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(certificate);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Sign Context
        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());

        // Registrar o atributo Id
        NodeList nodes = doc.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
            if (element.hasAttribute("Id") && element.getAttribute("Id").equals(elementId)) {
                element.setIdAttribute("Id", true);
                log.debug("Id attribute registrado para elemento: {}", element.getNodeName());
            }
        }

        // Assinar
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        log.info("Document assinado com sucesso para elementId: {}", elementId);
        return doc;
    }
}

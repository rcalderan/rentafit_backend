package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.config.FiscalCertificateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * Assina o elemento {@code infNFe} usando XMLDSig envelopado (Apache Santuario).
 *
 * <p>Exemplo: {@code signer.sign(nfeXml)} → XML da NF-e com {@code <Signature>} embutida.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NfeXmlSigner {

    static {
        Init.init();
    }

    private static final String NFE_NS = "http://www.portalfiscal.inf.br/nfe";

    /** Algoritmo de assinatura configuravel via {@code nf-e.signature.algorithm} (SHA1 ou SHA256). */
    @Value("${nf-e.signature.algorithm}")
    private String signatureAlgorithm;

    private final FiscalCertificateProvider certificateProvider;

    /**
     * Assina o XML da NF-e referenciando o {@code Id} do {@code infNFe}.
     *
     * @param xml XML da NF-e contendo {@code infNFe}
     * @return XML assinado
     * @throws IllegalStateException    se o certificado não estiver disponível ou infNFe ausente
     * @throws IllegalArgumentException se o XML for nulo ou vazio
     */
    public String sign(String xml) throws Exception {
        if (!certificateProvider.isAvailable()) {
            throw new IllegalStateException("Certificado digital nao disponivel para assinar a NF-e");
        }
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML para assinar nao pode ser nulo ou vazio");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element infNFe = (Element) doc.getElementsByTagNameNS(NFE_NS, "infNFe").item(0);
        if (infNFe == null) {
            infNFe = (Element) doc.getElementsByTagName("infNFe").item(0);
        }
        if (infNFe == null) {
            throw new IllegalStateException("Elemento infNFe nao encontrado no XML da NF-e");
        }

        String id = infNFe.getAttribute("Id");
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Atributo Id do infNFe ausente");
        }
        infNFe.setIdAttribute("Id", true);

        String sigAlgId = resolveSignatureAlgorithm(signatureAlgorithm);
        String digestAlgId = resolveDigestAlgorithm(signatureAlgorithm);
        XMLSignature sig = new XMLSignature(doc, "", sigAlgId,
                Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
        infNFe.getParentNode().appendChild(sig.getElement());

        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, digestAlgId);

        X509Certificate cert = certificateProvider.certificate();
        if (cert == null) {
            throw new IllegalStateException("Certificado final (X509Certificate) nao encontrado no PKCS12");
        }
        if (cert.getBasicConstraints() != -1) {
            throw new IllegalStateException("Certificado selecionado e uma Autoridade Certificadora (CA) e nao pode assinar a NF-e");
        }
        sig.addKeyInfo(cert);

        removeWhitespaceTextNodes(doc.getDocumentElement());

        sig.sign(certificateProvider.privateKey());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        log.debug("NF-e assinada com sucesso (Id={}, algoritmo={})", id, signatureAlgorithm);
        return writer.toString();
    }

    /**
     * Mapeia o valor da property {@code nf-e.signature.algorithm} para o identificador
     * do algoritmo de assinatura do Apache Santuario.
     *
     * @param alias "SHA1" ou "SHA256" (case-insensitive)
     * @return URI do algoritmo (ex.: {@link XMLSignature#ALGO_ID_SIGNATURE_RSA_SHA256})
     * @throws IllegalArgumentException se o alias for diferente de SHA1/SHA256
     */
    private static String resolveSignatureAlgorithm(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("nf-e.signature.algorithm nao pode ser nulo ou vazio (use SHA1 ou SHA256)");
        }
        return switch (alias.trim().toUpperCase()) {
            case "SHA1" -> XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
            case "SHA256" -> XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
            default -> throw new IllegalArgumentException(
                    "nf-e.signature.algorithm invalido: '" + alias + "'. Valores esperados: SHA1 ou SHA256");
        };
    }

    /**
     * Mapeia o valor da property {@code nf-e.signature.algorithm} para o identificador
     * do algoritmo de digest do Apache Santuario.
     *
     * @param alias "SHA1" ou "SHA256" (case-insensitive)
     * @return URI do algoritmo (ex.: {@link MessageDigestAlgorithm#ALGO_ID_DIGEST_SHA256})
     * @throws IllegalArgumentException se o alias for diferente de SHA1/SHA256
     */
    private static String resolveDigestAlgorithm(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("nf-e.signature.algorithm nao pode ser nulo ou vazio (use SHA1 ou SHA256)");
        }
        return switch (alias.trim().toUpperCase()) {
            case "SHA1" -> MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1;
            case "SHA256" -> MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256;
            default -> throw new IllegalArgumentException(
                    "nf-e.signature.algorithm invalido: '" + alias + "'. Valores esperados: SHA1 ou SHA256");
        };
    }

    private void removeWhitespaceTextNodes(org.w3c.dom.Node node) {
        var children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            var child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && text.isBlank()) {
                    node.removeChild(child);
                }
            } else {
                removeWhitespaceTextNodes(child);
            }
        }
    }

}

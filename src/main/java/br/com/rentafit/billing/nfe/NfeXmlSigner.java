package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.config.FiscalCertificateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
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

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        infNFe.getParentNode().appendChild(sig.getElement());

        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);

        sig.addKeyInfo(certificateProvider.certificate());
        sig.sign(certificateProvider.privateKey());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        log.debug("NF-e assinada com sucesso (Id={})", id);
        return writer.toString();
    }
}

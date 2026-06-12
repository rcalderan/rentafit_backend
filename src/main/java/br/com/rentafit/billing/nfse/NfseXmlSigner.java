package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.config.FiscalCertificateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.Init;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
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
 * Assina o XML DPS usando XMLDSig envelopado (Apache Santuario).
 *
 * <p>Exemplo: {@code signer.sign(xmlString)} → XML com assinatura embutida.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NfseXmlSigner {

    static {
        Init.init();
    }

    private final FiscalCertificateProvider certificateProvider;

    /**
     * Assina o XML DPS.
     *
     * @param xml string XML a assinar
     * @return XML assinado
     * @throws IllegalStateException    se o certificado não estiver disponível
     * @throws IllegalArgumentException se o XML for nulo ou vazio
     */
    public String sign(String xml) throws Exception {
        if (!certificateProvider.isAvailable()) {
            throw new IllegalStateException("Certificado digital nao disponivel para assinar o DPS");
        }
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML para assinar não pode ser nulo ou vazio");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element root = doc.getDocumentElement();
        String id = root.getAttribute("Id");
        if (id == null || id.isEmpty()) {
            id = "DPS";
            root.setAttribute("Id", id);
        }

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        root.appendChild(sig.getElement());

        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);

        sig.addKeyInfo(certificateProvider.certificate());
        sig.sign(certificateProvider.privateKey());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        log.debug("DPS assinado com sucesso");
        return writer.toString();
    }
}

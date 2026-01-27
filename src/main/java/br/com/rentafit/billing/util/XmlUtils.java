package br.com.rentafit.billing.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Slf4j
public class XmlUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);

    /**
     * Converte um Document XML para String.
     */
    public String documentToString(Document doc) throws Exception {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String result = writer.getBuffer().toString();

            if (result == null || result.trim().isEmpty()) {
                throw new IllegalStateException("Transformação resultou em string vazia");
            }

            return result;
        } catch (Exception e) {
            log.error("Erro ao converter Document para String", e);
            throw new Exception("Erro ao converter Document para String: " + e.getMessage(), e);
        }
    }

    /**
     * Converte uma String XML para Document.
     */
    public Document stringToDocument(String xmlStr) throws Exception {
        if (xmlStr == null || xmlStr.trim().isEmpty()) {
            throw new IllegalArgumentException("XML string cannot be null or empty");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // Configurações de segurança para prevenir XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

            if (doc == null) {
                log.error("Parser retornou documento nulo para XML: {}", xmlStr.substring(0, Math.min(200, xmlStr.length())));
                throw new IllegalStateException("Failed to parse XML: document is null");
            }

            return doc;
        } catch (Exception e) {
            log.error("Erro ao converter String para Document. XML: {}", xmlStr.substring(0, Math.min(500, xmlStr.length())), e);
            throw new Exception("Erro ao parsear XML: " + e.getMessage(), e);
        }
    }

    /**
     * Cria um novo Document XML.
     */
    public Document createDocument() throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            if (doc == null) {
                throw new IllegalStateException("DocumentBuilder.newDocument() retornou null");
            }

            return doc;
        } catch (Exception e) {
            log.error("Erro ao criar novo Document XML", e);
            throw new Exception("Erro ao criar Document: " + e.getMessage(), e);
        }
    }

    /**
     * Adiciona um elemento filho com texto ao elemento pai.
     */
    public Element addElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        if (value != null && !value.isEmpty()) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }

    /**
     * Adiciona um elemento filho com namespace.
     */
    public Element addElementNS(Document doc, Element parent, String namespace, String name, String value) {
        Element element = doc.createElementNS(namespace, name);
        if (value != null && !value.isEmpty()) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }

    /**
     * Formata um BigDecimal para o formato aceito pela NFS-e (com 2 casas decimais).
     */
    public String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        DecimalFormat df = new DecimalFormat("0.00", SYMBOLS);
        return df.format(value);
    }

    /**
     * Formata um BigDecimal para alíquota (4 casas decimais).
     */
    public String formatAliquota(BigDecimal value) {
        if (value == null) {
            return "0.0000";
        }
        DecimalFormat df = new DecimalFormat("0.0000", SYMBOLS);
        return df.format(value);
    }

    /**
     * Formata uma data/hora para o formato ISO aceito pela NFS-e.
     */
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Remove namespace de um XML (útil para debug).
     */
    public String removeNamespaces(String xml) {
        return xml.replaceAll(" xmlns(:\\w+)?=\"[^\"]*\"", "");
    }

    /**
     * Extrai texto de um elemento XML por tag name.
     */
    public String getElementText(Element parent, String tagName) {
        var nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Pretty print de XML para debug.
     */
    public String prettyPrint(String xml) {
        try {
            Document doc = stringToDocument(xml);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            log.warn("Erro ao formatar XML: {}", e.getMessage());
            return xml;
        }
    }
}

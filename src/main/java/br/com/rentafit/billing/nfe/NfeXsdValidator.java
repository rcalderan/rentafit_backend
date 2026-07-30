package br.com.rentafit.billing.nfe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida o XML da NF-e contra o schema oficial (nfe_v4.00.xsd) do pacote PL.010d.
 *
 * <p>Carrega os XSDs do classpath ({@code classpath:xsd/nfe/...}) e resolve includes/imports
 * automaticamente. Falhas de schema resultam em {@link NfeValidationException} com a linha e
 * a mensagem do parser.</p>
 *
 * <p>Exemplo: {@code validator.validate(xml)} → lança {@link NfeValidationException} se inválido.</p>
 */
@Component
@Slf4j
public class NfeXsdValidator {

    private static final String SCHEMA_PATH = "/xsd/nfe/nfe_v4.00.xsd";

    private final Schema schema;

    public NfeXsdValidator() {
        this.schema = carregarSchema();
    }

    /**
     * Valida o XML da NF-e.
     *
     * @param xml XML a validar
     * @throws IllegalArgumentException se o XML for nulo ou vazio
     * @throws NfeValidationException   se malformado ou contra o schema
     */
    public void validate(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML para validar nao pode ser nulo ou vazio");
        }

        Document doc = parse(xml);

        Element infNFe = (Element) doc.getElementsByTagName("infNFe").item(0);
        if (infNFe == null) {
            throw new NfeValidationException("XML invalido: elemento obrigatorio 'infNFe' ausente");
        }

        String id = infNFe.getAttribute("Id");
        if (id == null || id.isEmpty()) {
            throw new NfeValidationException("XML invalido: atributo 'Id' do infNFe ausente");
        }

        if (schema == null) {
            log.warn("Schema XSD nao encontrado em classpath ({}). Validação limitada a well-formedness.", SCHEMA_PATH);
            return;
        }

        List<String> erros = new ArrayList<>();
        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new SchemaErrorHandler(erros));
            Source source = new StreamSource(new StringReader(xml));
            validator.validate(source);
        } catch (SAXException | IOException e) {
            throw new NfeValidationException("Falha na validação do XML da NF-e: " + e.getMessage(), e);
        }

        if (!erros.isEmpty()) {
            throw new NfeValidationException("XML da NF-e rejeitado pelo schema: " + String.join("; ", erros));
        }

        log.debug("NF-e validada pelo schema oficial (Id={})", id);
    }

    private Schema carregarSchema() {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_PATH)) {
            if (is == null) {
                log.error("Schema {} não encontrado no classpath", SCHEMA_PATH);
                return null;
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new ClasspathResourceResolver());
            return factory.newSchema(new StreamSource(is));
        } catch (Exception e) {
            log.error("Falha ao carregar schema XSD da NF-e: {}", e.getMessage(), e);
            return null;
        }
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new NfeValidationException("XML da NF-e malformado: " + e.getMessage(), e);
        }
    }

    private static final class SchemaErrorHandler implements ErrorHandler {

        private final List<String> erros;

        SchemaErrorHandler(List<String> erros) {
            this.erros = erros;
        }

        @Override
        public void warning(SAXParseException exception) {
            log.warn("Schema warning na linha {}: {}", exception.getLineNumber(), exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) {
            erros.add(formatar(exception));
        }

        @Override
        public void fatalError(SAXParseException exception) {
            erros.add(formatar(exception));
        }

        private static String formatar(SAXParseException e) {
            return String.format("linha %d, coluna %d: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
        }
    }
}

package br.com.rentafit.billing.nfse;

import br.com.rentafit.common.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

/**
 * Valida o XML DPS contra o XSD oficial NFS-e Nacional v1.01 (DPS_v1.01.xsd).
 *
 * <p>Exemplo: {@code validator.validate(xml)} → lança ValidationException se inválido.</p>
 */
@Component
@Slf4j
public class NfseDpsXsdValidator {

    private static final String XSD_DIR = "xsd/nfse/";
    private static final String ROOT_XSD = "DPS_v1.01.xsd";

    private Schema schema;

    @PostConstruct
    void initSchema() throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setResourceResolver(new ClasspathResourceResolver());

        schema = factory.newSchema(new StreamSource(
                new ClassPathResource(XSD_DIR + ROOT_XSD).getInputStream(),
                ROOT_XSD));
    }

    /**
     * Valida o XML DPS contra o XSD oficial.
     *
     * @param xml XML do DPS a ser validado
     * @throws ValidationException se o XML não conforma ao XSD
     */
    public void validate(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new ValidationException("XML DPS não pode ser null ou vazio");
        }

        try {
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            log.debug("XML DPS validado com sucesso contra XSD oficial");
        } catch (SAXParseException e) {
            String msg = String.format("Validação XSD falhou na linha %d, coluna %d: %s",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            log.warn("Falha na validação XSD do DPS: {}", msg);
            throw new ValidationException(msg, e);
        } catch (SAXException | IOException e) {
            String msg = "Erro ao validar XML DPS contra XSD: " + e.getMessage();
            log.warn("Erro de validação XSD do DPS: {}", msg);
            throw new ValidationException(msg, e);
        }
    }

    /**
     * Coleta todos os erros de validação sem lançar exceção.
     *
     * @param xml XML do DPS a ser validado
     * @return lista de mensagens de erro; vazia se válido
     */
    public List<String> collectErrors(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of("XML DPS não pode ser null ou vazio");
        }

        java.util.List<String> errors = new java.util.ArrayList<>();
        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    errors.add(formatError("WARNING", e));
                }

                @Override
                public void error(SAXParseException e) {
                    errors.add(formatError("ERROR", e));
                }

                @Override
                public void fatalError(SAXParseException e) {
                    errors.add(formatError("FATAL", e));
                }

                private String formatError(String level, SAXParseException e) {
                    return String.format("[%s] linha %d, col %d: %s",
                            level, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                }
            });
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            errors.add("Erro ao validar: " + e.getMessage());
        }
        return errors;
    }

    /**
     * Resolve XSD imports/includes from the classpath instead of the file system,
     * bypassing the accessExternalSchema security restriction.
     */
    private static class ClasspathResourceResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(String type, String namespaceURI,
                                       String publicId, String systemId, String baseUri) {
            if (systemId == null) return null;

            String resourcePath = XSD_DIR + systemId;
            try {
                InputStream is = new ClassPathResource(resourcePath).getInputStream();
                return new LSInputImpl(publicId, systemId, is);
            } catch (IOException e) {
                log.warn("Não foi possível resolver XSD do classpath: {}", resourcePath);
                return null;
            }
        }
    }

    private static class LSInputImpl implements LSInput {
        private final String publicId;
        private final String systemId;
        private final InputStream inputStream;

        LSInputImpl(String publicId, String systemId, InputStream inputStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.inputStream = inputStream;
        }

        @Override public InputStream getByteStream() { return inputStream; }
        @Override public void setByteStream(InputStream byteStream) {}
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean certifiedText) {}
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String publicId) {}
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String systemId) {}
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String baseURI) {}
        @Override public String getEncoding() { return null; }
        @Override public void setEncoding(String encoding) {}
        @Override public java.io.Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(java.io.Reader characterStream) {}
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String stringData) {}
    }
}

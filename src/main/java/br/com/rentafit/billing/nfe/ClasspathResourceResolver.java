package br.com.rentafit.billing.nfe;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.io.Reader;

/**
 * Resolve includes e imports de schemas XSD carregando-os do classpath.
 *
 * <p>Usado por {@link NfeXsdValidator} para resolver dependências locais do schema NF-e.</p>
 */
@Slf4j
public class ClasspathResourceResolver implements LSResourceResolver {

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (systemId == null) {
            return null;
        }
        String resourcePath = "/xsd/nfe/" + systemId;
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            log.warn("Recurso de schema não encontrado no classpath: {}", resourcePath);
            return null;
        }
        return new ClasspathInput(publicId, systemId, is);
    }

    private static final class ClasspathInput implements LSInput {

        private final String publicId;
        private final String systemId;
        private final InputStream byteStream;

        ClasspathInput(String publicId, String systemId, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.byteStream = byteStream;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
            // no-op
        }

        @Override
        public InputStream getByteStream() {
            return byteStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            // no-op
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
            // no-op
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            // no-op
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            // no-op
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
            // no-op
        }

        @Override
        public String getEncoding() {
            return "UTF-8";
        }

        @Override
        public void setEncoding(String encoding) {
            // no-op
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
            // no-op
        }
    }
}

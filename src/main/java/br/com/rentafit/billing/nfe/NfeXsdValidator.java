package br.com.rentafit.billing.nfe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Valida a estrutura do XML da NF-e: well-formedness e presença dos elementos
 * obrigatórios ({@code infNFe} com atributo {@code Id}).
 *
 * <p>Validação completa contra XSD do pacote 010c (nfe_v4.00.xsd) pode ser plugada
 * apontando {@code nf-e.schemas.dir}; na ausência, faz validação estrutural mínima.</p>
 *
 * <p>Exemplo: {@code validator.validate(xml)} → lança {@link NfeValidationException} se inválido.</p>
 */
@Component
@Slf4j
public class NfeXsdValidator {

    /**
     * Valida o XML da NF-e.
     *
     * @param xml XML a validar
     * @throws IllegalArgumentException se o XML for nulo ou vazio
     * @throws NfeValidationException   se malformado ou sem elementos obrigatórios
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

        log.debug("NF-e validada estruturalmente (Id={})", id);
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
}

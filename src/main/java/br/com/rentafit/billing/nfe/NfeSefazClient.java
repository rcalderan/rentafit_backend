package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Transmite o lote de NF-e assinado à SEFAZ-SP e interpreta o retorno
 * ({@code retEnviNFe}/{@code protNFe}).
 *
 * <p>Exemplo: {@code client.transmit(signedXml)} → {@link NfeResponse} com cStat/protocolo.</p>
 */
@Component
@Slf4j
public class NfeSefazClient {

    /** cStat que indicam autorização (uso autorizado / autorizado fora de prazo). */
    private static final Set<String> AUTHORIZED_CODES = Set.of("100", "150");

    private final WebClient nfeSefazWebClient;

    public NfeSefazClient(@Qualifier("nfeSefazWebClient") WebClient nfeSefazWebClient) {
        this.nfeSefazWebClient = nfeSefazWebClient;
    }

    /**
     * Envia o XML assinado e retorna a resposta interpretada.
     *
     * @param signedXml XML da NF-e já assinado
     * @return resposta da SEFAZ
     */
    public NfeResponse transmit(String signedXml) {
        if (signedXml == null || signedXml.isBlank()) {
            throw new IllegalArgumentException("XML assinado nao pode ser nulo ou vazio");
        }
        log.debug("Transmitindo NF-e para SEFAZ-SP");
        String retorno = nfeSefazWebClient.post()
                .uri("/ws/nfeautorizacao4.asmx")
                .header("Content-Type", "application/soap+xml;charset=UTF-8")
                .bodyValue(signedXml)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseResponse(retorno);
    }

    /**
     * Interpreta o XML de retorno da SEFAZ extraindo cStat, xMotivo, chNFe e nProt.
     *
     * @param retEnviNfeXml XML de retorno (retEnviNFe/protNFe)
     * @return resposta mapeada
     * @throws IllegalArgumentException se o retorno for nulo/vazio
     * @throws NfeValidationException   se o retorno for malformado
     */
    public NfeResponse parseResponse(String retEnviNfeXml) {
        if (retEnviNfeXml == null || retEnviNfeXml.isBlank()) {
            throw new IllegalArgumentException("Retorno da SEFAZ nao pode ser nulo ou vazio");
        }

        Document doc = parse(retEnviNfeXml);

        String cStat = text(doc, "cStat");
        String xMotivo = text(doc, "xMotivo");
        String chNFe = text(doc, "chNFe");
        String nProt = text(doc, "nProt");

        String status = AUTHORIZED_CODES.contains(cStat) ? "AUTHORIZED" : "REJECTED";

        return NfeResponse.builder()
                .accessKey(chNFe)
                .protocol(nProt)
                .cStat(cStat)
                .xMotivo(xMotivo)
                .status(status)
                .authorizedXml("AUTHORIZED".equals(status) ? retEnviNfeXml : null)
                .build();
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new NfeValidationException("Retorno da SEFAZ malformado: " + e.getMessage(), e);
        }
    }

    private String text(Document doc, String tag) {
        Node n = doc.getElementsByTagName(tag).item(0);
        return n != null ? n.getTextContent() : null;
    }
}

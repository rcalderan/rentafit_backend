package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatusCode;
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

    private static final String SOAP_NS = "http://www.w3.org/2003/05/soap-envelope";
    private static final String NFE_WSDL_NS = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4";
    private static final String NFE_DATA_NS = "http://www.portalfiscal.inf.br/nfe";
    private static final String NFE_VERSAO = "4.00";

    private final WebClient nfeSefazWebClient;
    private final String ufCode;

    public NfeSefazClient(
            @Qualifier("nfeSefazWebClient") WebClient nfeSefazWebClient,
            @Value("${nf-e.emit.uf-code:35}") String ufCode
    ) {
        this.nfeSefazWebClient = nfeSefazWebClient;
        this.ufCode = ufCode;
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
        String soapEnvelope = buildSoapEnvelope(signedXml);
        log.trace("Envelope SOAP enviado:\n{}", soapEnvelope);

        String[] retorno = nfeSefazWebClient.post()
                .uri("/ws/nfeautorizacao4.asmx")
                .header("Content-Type",
                        "application/soap+xml;charset=UTF-8;action=\""
                                + NFE_WSDL_NS + "/nfeAutorizacaoLote\"")
                .bodyValue(soapEnvelope)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    return response.bodyToMono(byte[].class)
                            .defaultIfEmpty(new byte[0])
                            .map(bytes -> {
                                String bodyStr = new String(bytes, StandardCharsets.UTF_8);
                                log.debug("SEFAZ response: HTTP {} | {} bytes | headers: {}",
                                        status.value(), bytes.length,
                                        response.headers().asHttpHeaders().toSingleValueMap());
                                return new String[]{String.valueOf(status.value()), bodyStr};
                            });
                })
                .block();

        String httpStatus = retorno[0];
        String body = retorno[1];

        if (!httpStatus.startsWith("2")) {
            log.error("SEFAZ retornou HTTP {}. Resposta:\n{}", httpStatus, body);
            throw new NfeValidationException(
                    "SEFAZ retornou HTTP " + httpStatus + ": " + body);
        }

        log.debug("SEFAZ retornou HTTP {}. Tamanho da resposta: {} chars", httpStatus, body.length());
        return parseResponse(body);
    }

    /**
     * Monta o envelope SOAP 1.2 exigido pelo endpoint nfeautorizacao4.asmx:
     * Header com nfeCabecMsg (cUF + versaoDados) e Body com nfeDadosMsg/enviNFe.
     *
     * @param signedNfeXml XML da NF-e já assinado
     * @return envelope SOAP completo
     */
    private String buildSoapEnvelope(String signedNfeXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"" + SOAP_NS + "\">"
                + "<soap12:Header>"
                + "<nfeCabecMsg xmlns=\"" + NFE_WSDL_NS + "\">"
                + "<cUF>" + ufCode + "</cUF>"
                + "<versaoDados>" + NFE_VERSAO + "</versaoDados>"
                + "</nfeCabecMsg>"
                + "</soap12:Header>"
                + "<soap12:Body>"
                + "<nfeDadosMsg xmlns=\"" + NFE_WSDL_NS + "\">"
                + "<enviNFe xmlns=\"" + NFE_DATA_NS + "\" versao=\"" + NFE_VERSAO + "\">"
                + "<idLote>1</idLote>"
                + "<indSinc>1</indSinc>"
                + signedNfeXml
                + "</enviNFe>"
                + "</nfeDadosMsg>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
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

package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfeSefazClient - parsing da resposta SEFAZ (retEnviNFe/protNFe)")
class NfeSefazClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private NfeSefazClient client;

    @BeforeEach
    void setUp() {
        client = new NfeSefazClient(webClient, "35", "https://homologacao.nfe.fazenda.sp.gov.br");
    }

    private String retornoComStatus(String cStat, String xMotivo) {
        return "<retEnviNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                + "<protNFe><infProt>"
                + "<chNFe>35200000000000000191550010000000011000000010</chNFe>"
                + "<cStat>" + cStat + "</cStat>"
                + "<xMotivo>" + xMotivo + "</xMotivo>"
                + "<nProt>135200000000001</nProt>"
                + "</infProt></protNFe></retEnviNFe>";
    }

    @Test
    @DisplayName("parseResponse() mapeia cStat 100 para status AUTHORIZED")
    void parseResponse_cStat100_autorizado() {
        NfeResponse resp = client.parseResponse(retornoComStatus("100", "Autorizado o uso da NF-e"));

        assertThat(resp.getCStat()).isEqualTo("100");
        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
    }

    @Test
    @DisplayName("parseResponse() mapeia cStat de rejeição para status REJECTED")
    void parseResponse_cStatRejeicao_rejeitado() {
        NfeResponse resp = client.parseResponse(retornoComStatus("539", "Rejeicao: duplicidade de NF-e"));

        assertThat(resp.getCStat()).isEqualTo("539");
        assertThat(resp.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("parseResponse() extrai chave de acesso e protocolo")
    void parseResponse_extraiChaveEProtocolo() {
        NfeResponse resp = client.parseResponse(retornoComStatus("100", "Autorizado"));

        assertThat(resp.getAccessKey()).isEqualTo("35200000000000000191550010000000011000000010");
        assertThat(resp.getProtocol()).isEqualTo("135200000000001");
    }

    @Test
    @DisplayName("parseResponse() inclui xMotivo da resposta")
    void parseResponse_incluiMotivo() {
        NfeResponse resp = client.parseResponse(retornoComStatus("100", "Autorizado o uso da NF-e"));

        assertThat(resp.getXMotivo()).contains("Autorizado");
    }

    @Test
    @DisplayName("parseResponse() lança NfeValidationException para XML malformado")
    void parseResponse_lancaExcecao_malformado() {
        assertThatThrownBy(() -> client.parseResponse("<retEnviNFe><protNFe>"))
                .isInstanceOf(NfeValidationException.class);
    }

    @Test
    @DisplayName("parseResponse() lança IllegalArgumentException para resposta nula")
    void parseResponse_lancaExcecao_nula() {
        assertThatThrownBy(() -> client.parseResponse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parseResponse() lança IllegalArgumentException para resposta vazia")
    void parseResponse_lancaExcecao_vazia() {
        assertThatThrownBy(() -> client.parseResponse("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parseResponse() prioriza cStat do infProt sobre cStat do lote")
    void parseResponse_priorizaInfProt_sobreLote() {
        String retorno = "<retEnviNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                + "<cStat>104</cStat>"
                + "<xMotivo>Lote processado</xMotivo>"
                + "<protNFe><infProt>"
                + "<chNFe>35200000000000000191550010000000011000000010</chNFe>"
                + "<cStat>100</cStat>"
                + "<xMotivo>Autorizado o uso da NF-e</xMotivo>"
                + "<nProt>135200000000001</nProt>"
                + "</infProt></protNFe></retEnviNFe>";

        NfeResponse resp = client.parseResponse(retorno);

        assertThat(resp.getCStat()).isEqualTo("100");
        assertThat(resp.getXMotivo()).contains("Autorizado");
        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(resp.getProtocol()).isEqualTo("135200000000001");
    }

    @Test
    @DisplayName("parseResponse() mapeia cStat 150 para status AUTHORIZED (fora de prazo)")
    void parseResponse_cStat150_autorizado() {
        NfeResponse resp = client.parseResponse(retornoComStatus("150", "Autorizado fora de prazo"));

        assertThat(resp.getCStat()).isEqualTo("150");
        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
    }

    @SuppressWarnings("unchecked")
    private void stubTransmitChain(int httpStatus, String responseBody) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String[]>> handler = invocation.getArgument(0);
            ClientResponse mockResponse = org.mockito.Mockito.mock(ClientResponse.class);
            ClientResponse.Headers mockHeaders = org.mockito.Mockito.mock(ClientResponse.Headers.class);
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(httpStatus));
            when(mockResponse.bodyToMono(byte[].class))
                    .thenReturn(Mono.just(responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            when(mockResponse.headers()).thenReturn(mockHeaders);
            when(mockHeaders.asHttpHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
            return handler.apply(mockResponse);
        });
    }

    @Test
    @DisplayName("transmit() envia envelope SOAP e retorna resposta parseada")
    void transmit_enviaEnvelopeSoapERetornaResposta() {
        String signedXml = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe>test</infNFe></NFe>";
        String retorno = retornoComStatus("100", "Autorizado");
        stubTransmitChain(200, retorno);

        NfeResponse resp = client.transmit(signedXml);

        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(resp.getCStat()).isEqualTo("100");
    }

    @Test
    @DisplayName("transmit() envia body contendo envelope SOAP com nfeCabecMsg e enviNFe")
    void transmit_enviaEnvelopeSoapComCabecalhoELote() {
        String signedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe>test</infNFe></NFe>";
        String retorno = retornoComStatus("100", "Autorizado");
        stubTransmitChain(200, retorno);

        client.transmit(signedXml);

        org.mockito.ArgumentCaptor<String> bodyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        assertThat(body).contains("soap12:Envelope");
        assertThat(body).contains("nfeCabecMsg");
        assertThat(body).contains("<cUF>35</cUF>");
        assertThat(body).contains("versaoDados>4.00");
        assertThat(body).contains("enviNFe");
        assertThat(body).contains("<indSinc>1</indSinc>");
        assertThat(body).contains("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe>test</infNFe></NFe>");
        assertThat(body).doesNotContain("<enviNFe" + System.lineSeparator());
        assertThat(body).doesNotContain("<?xml version=\"1.0\" encoding=\"UTF-8\"?><enviNFe");
        int enviNFeIdx = body.indexOf("<enviNFe");
        assertThat(enviNFeIdx).isGreaterThan(0);
        String afterEnviNFe = body.substring(enviNFeIdx);
        assertThat(afterEnviNFe).doesNotContain("<?xml version");

        org.mockito.ArgumentCaptor<String> headerValueCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(requestBodySpec).header(org.mockito.ArgumentMatchers.eq("Content-Type"), headerValueCaptor.capture());
        assertThat(headerValueCaptor.getValue()).contains("action=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4/nfeAutorizacaoLote\"");
    }

    @Test
    @DisplayName("transmit() lança NfeValidationException com body da SEFAZ quando HTTP 400")
    void transmit_lancaExcecaoComBody_quandoHttp400() {
        String signedXml = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe>test</infNFe></NFe>";
        String sefazError = "<soap:Fault><faultstring>Erro de validação no XML</faultstring></soap:Fault>";
        stubTransmitChain(400, sefazError);

        assertThatThrownBy(() -> client.transmit(signedXml))
                .isInstanceOf(NfeValidationException.class)
                .hasMessageContaining("SEFAZ retornou HTTP 400")
                .hasMessageContaining("Erro de validação no XML");
    }

    @Test
    @DisplayName("transmit() lança exceção para XML nulo")
    void transmit_lancaExcecao_xmlNulo() {
        assertThatThrownBy(() -> client.transmit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XML assinado");
    }

    @Test
    @DisplayName("transmit() lança exceção para XML vazio")
    void transmit_lancaExcecao_xmlVazio() {
        assertThatThrownBy(() -> client.transmit("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XML assinado");
    }

    @Test
    @DisplayName("parseResponse() inclui authorizedXml quando autorizado")
    void parseResponse_incluiAuthorizedXml_quandoAutorizado() {
        String retorno = retornoComStatus("100", "Autorizado");
        NfeResponse resp = client.parseResponse(retorno);

        assertThat(resp.getAuthorizedXml()).isEqualTo(retorno);
    }

    @Test
    @DisplayName("parseResponse() não inclui authorizedXml quando rejeitado")
    void parseResponse_naoIncluiAuthorizedXml_quandoRejeitado() {
        String retorno = retornoComStatus("539", "Rejeicao");
        NfeResponse resp = client.parseResponse(retorno);

        assertThat(resp.getAuthorizedXml()).isNull();
    }
}

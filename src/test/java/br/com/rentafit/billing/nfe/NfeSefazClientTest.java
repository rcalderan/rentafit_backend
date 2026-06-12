package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private NfeSefazClient client;

    @BeforeEach
    void setUp() {
        client = new NfeSefazClient(webClient);
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
    @DisplayName("parseResponse() mapeia cStat 150 para status AUTHORIZED (fora de prazo)")
    void parseResponse_cStat150_autorizado() {
        NfeResponse resp = client.parseResponse(retornoComStatus("150", "Autorizado fora de prazo"));

        assertThat(resp.getCStat()).isEqualTo("150");
        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
    }

    @Test
    @DisplayName("transmit() envia XML e retorna resposta parseada")
    void transmit_enviaXmlERetornaResposta() {
        String signedXml = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe>test</infNFe></NFe>";
        String retorno = retornoComStatus("100", "Autorizado");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(retorno));

        NfeResponse resp = client.transmit(signedXml);

        assertThat(resp.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(resp.getCStat()).isEqualTo("100");
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

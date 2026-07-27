package br.com.rentafit.billing.nfse;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NfseDpsPayloadEncoderTest {

    private final NfseDpsPayloadEncoder encoder = new NfseDpsPayloadEncoder();

    @Test
    void encode_comprimeXmlUtf8EmBase64() throws IOException {
        String xml = "<DPS><desc>Locação &amp; serviço</desc></DPS>";

        NfseDpsPayload payload = encoder.encode(xml);

        assertThat(uncompress(payload.dpsXmlGZipB64())).isEqualTo(xml);
    }

    @Test
    void encode_rejeitaXmlVazio() {
        assertThatThrownBy(() -> encoder.encode(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não vazio");
    }

    private String uncompress(String encodedPayload) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(encodedPayload);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

package br.com.rentafit.billing.nfse;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

@Component
public class NfseDpsPayloadEncoder {

    public NfseDpsPayload encode(String signedXml) {
        if (signedXml == null || signedXml.isBlank()) {
            throw new IllegalArgumentException("XML DPS assinado deve conter texto não vazio");
        }
        return new NfseDpsPayload(Base64.getEncoder().encodeToString(compress(signedXml)));
    }

    private byte[] compress(String signedXml) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(signedXml.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível compactar o XML DPS para transmissão", exception);
        }
    }
}

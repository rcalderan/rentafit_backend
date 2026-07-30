package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.billing.dto.NfeResponse;
import br.com.rentafit.billing.dto.NfeSignedPayloadResponse;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra a emissão de NF-e (modelo 55) na SEFAZ-SP:
 * carregar cliente → montar XML → assinar → validar → transmitir → persistir.
 *
 * <p>Exemplo: {@code service.emit(request)} → {@link NfeResponse} com cStat/protocolo.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfeEmissionService {

    private final NfeXmlBuilder xmlBuilder;
    private final NfeXmlSigner xmlSigner;
    private final NfeXsdValidator xsdValidator;
    private final NfeSefazClient sefazClient;
    private final FiscalDocumentService fiscalDocumentService;
    private final CustomerRepository customerRepository;

    /**
     * Emite uma NF-e de forma síncrona.
     *
     * @param request dados da emissão
     * @return resposta da SEFAZ
     * @throws ResourceNotFoundException se o cliente não existir
     * @throws NfeValidationException    se o XML assinado falhar na validação
     */
    public NfeResponse emit(NfeEmissionRequest request) {
        log.info("Iniciando emissao de NF-e para cliente: {}", request.getCustomerId());

        Customer customer = loadCustomer(request.getCustomerId());
        String signedXml = buildAndSignXml(request, customer);

        NfeResponse response = sefazClient.transmit(signedXml);

        FiscalDocument saved = persistir(request, customer, signedXml, response);
        log.info("NF-e processada: id={} status={} chave={}",
                saved.getId(), response.getStatus(), response.getAccessKey());

        return response;
    }

    /**
     * Constrói e assina o XML da NF-e sem transmitir à SEFAZ.
     * Retorna o envelope SOAP completo + metadados para o frontend fazer o POST.
     *
     * @param request dados da emissão
     * @return payload assinado com envelope SOAP, URL e headers
     */
    public NfeSignedPayloadResponse buildSignedPayload(NfeEmissionRequest request) {
        log.info("Construindo payload assinado de NF-e para cliente: {}", request.getCustomerId());

        Customer customer = loadCustomer(request.getCustomerId());
        String signedXml = buildAndSignXml(request, customer);
        String soapEnvelope = sefazClient.buildSoapEnvelope(signedXml);

        String accessKey = extractAccessKey(signedXml);

        return NfeSignedPayloadResponse.builder()
                .soapEnvelope(soapEnvelope)
                .signedXml(signedXml)
                .sefazUrl(sefazClient.getSefazUrl())
                .contentType(sefazClient.getContentType())
                .soapAction(sefazClient.getSoapAction())
                .accessKey(accessKey)
                .build();
    }

    private Customer loadCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", customerId));
    }

    private String buildAndSignXml(NfeEmissionRequest request, Customer customer) {
        String rawXml = xmlBuilder.buildXml(request, customer);
        String signedXml = assinar(rawXml);
        xsdValidator.validate(signedXml);
        return signedXml;
    }

    private String extractAccessKey(String signedXml) {
        int idx = signedXml.indexOf("Id=\"NFe");
        if (idx < 0) return null;
        int start = idx + 7;
        int end = signedXml.indexOf("\"", start);
        return end > start ? signedXml.substring(start, end) : null;
    }

    private String assinar(String rawXml) {
        try {
            return xmlSigner.sign(rawXml);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar NF-e: " + e.getMessage(), e);
        }
    }

    private FiscalDocument persistir(NfeEmissionRequest request, Customer customer,
                                     String signedXml, NfeResponse response) {
        boolean autorizada = "AUTHORIZED".equals(response.getStatus());

        FiscalDocument doc = FiscalDocument.builder()
                .type(FiscalDocumentType.NFE)
                .status(autorizada ? FiscalDocumentStatus.AUTHORIZED : FiscalDocumentStatus.REJECTED)
                .model(55)
                .accessKey(response.getAccessKey())
                .protocol(response.getProtocol())
                .authorizationDate(autorizada ? OffsetDateTime.now() : null)
                .customer(customer)
                .issueDate(OffsetDateTime.now())
                .totalValue(totalProdutos(request.getItems()))
                .origin(parseOrigin(request.getOrigin()))
                .originId(request.getOriginId())
                .signedXml(signedXml)
                .authorizedXml(response.getAuthorizedXml())
                .rejectionReason(autorizada ? null : response.getXMotivo())
                .build();

        return fiscalDocumentService.save(doc);
    }

    private BigDecimal totalProdutos(List<NfeItemRequest> items) {
        return items.stream()
                .map(i -> i.getUnitValue().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FiscalOrigin parseOrigin(String origin) {
        if (origin == null) return FiscalOrigin.MANUAL;
        try {
            return FiscalOrigin.valueOf(origin.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FiscalOrigin.MANUAL;
        }
    }
}

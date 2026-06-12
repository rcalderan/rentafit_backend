package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.TaxInfo;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Orquestra o fluxo completo de emissão NFS-e Nacional:
 * carregar cliente → calcular tributos → construir XML → assinar → enviar → persistir.
 *
 * <p>Exemplo: {@code service.emit(request)} → {@code Mono<InvoiceEmissionResponseDTO>}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfseEmissionService {

    private final NfseDpsXmlBuilder dpsXmlBuilder;
    private final NfseXmlSigner xmlSigner;
    private final NfsePortalClient portalClient;
    private final FiscalDocumentService fiscalDocumentService;
    private final CustomerRepository customerRepository;

    @Value("${nfs-e.prestador.cnpj:00000000000000}")
    private String prestadorCnpj;

    @Value("${nfs-e.prestador.im:}")
    private String prestadorIm;

    @Value("${nfs-e.ambiente:2}")
    private String ambiente;

    @Value("${nfs-e.tributos.ibs.aliquota:0.025}")
    private BigDecimal ibsAliquotaPadrao;

    @Value("${nfs-e.tributos.cbs.aliquota:0.015}")
    private BigDecimal cbsAliquotaPadrao;

    public Mono<InvoiceEmissionResponseDTO> emit(InvoiceEmissionRequestDTO request) {
        log.info("Iniciando emissão NFS-e para cliente: {}", request.getCustomerId());

        return Mono.fromCallable(() -> {
                    Customer c = customerRepository.findById(request.getCustomerId())
                            .orElseThrow(() -> ResourceNotFoundException.forId("Customer", request.getCustomerId()));
                    TaxInfo taxes = calcularTributos(request);
                    DpsRequest dpsReq = buildDpsRequest(c, request, taxes);
                    String rawXml = dpsXmlBuilder.buildXml(dpsReq);
                    String signedXml = xmlSigner.sign(rawXml);
                    return new Object[]{c, taxes, signedXml};
                })
                .onErrorMap(e -> !(e instanceof ResourceNotFoundException) && !(e instanceof IllegalStateException)
                                ? new IllegalStateException("Falha ao preparar emissão: " + e.getMessage(), e)
                                : e)
                .flatMap(parts -> {
                    Customer customer = (Customer) parts[0];
                    TaxInfo taxes = (TaxInfo) parts[1];
                    String signedXml = (String) parts[2];
                    return portalClient.sendDps(signedXml)
                            .map(response -> persistirEMapear(customer, request, taxes, response));
                });
    }

    private InvoiceEmissionResponseDTO persistirEMapear(
            Customer customer, InvoiceEmissionRequestDTO request,
            TaxInfo taxes, DpsResponse response) {

        FiscalOrigin origin = parseOrigin(request.getOrigin());

        FiscalDocument doc = FiscalDocument.builder()
                .type(FiscalDocumentType.NFSE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .accessKey(response.getAccessKey())
                .protocol(response.getProtocol())
                .authorizationDate(response.getDhProcessamento())
                .customer(customer)
                .issueDate(OffsetDateTime.now())
                .totalValue(request.getServiceValue())
                .taxes(taxes)
                .origin(origin)
                .originId(request.getOriginId())
                .build();

        FiscalDocument saved = fiscalDocumentService.save(doc);
        log.info("NFS-e salva: id={} chave={}", saved.getId(), saved.getAccessKey());

        return toResponseDTO(saved, response, taxes);
    }

    private TaxInfo calcularTributos(InvoiceEmissionRequestDTO req) {
        BigDecimal val = req.getServiceValue();
        BigDecimal ibsRate = req.getIbsRate() != null ? req.getIbsRate() : ibsAliquotaPadrao;
        BigDecimal cbsRate = req.getCbsRate() != null ? req.getCbsRate() : cbsAliquotaPadrao;
        BigDecimal isqnRate = req.getIsqnRate() != null ? req.getIsqnRate() : BigDecimal.ZERO;

        BigDecimal ibsVal = val.multiply(ibsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cbsVal = val.multiply(cbsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal isqnVal = val.multiply(isqnRate).setScale(2, RoundingMode.HALF_UP);

        return TaxInfo.builder()
                .ibsRate(ibsRate).ibsValue(ibsVal)
                .cbsRate(cbsRate).cbsValue(cbsVal)
                .isqnRate(isqnRate).isqnValue(isqnVal)
                .totalTaxValue(ibsVal.add(cbsVal).add(isqnVal))
                .build();
    }

    private DpsRequest buildDpsRequest(Customer customer, InvoiceEmissionRequestDTO req, TaxInfo taxes) {
        DpsRequest.Identificacao identif = customer.getDocument().length() == 14
                ? DpsRequest.Identificacao.builder().CNPJ(customer.getDocument()).build()
                : DpsRequest.Identificacao.builder().CPF(customer.getDocument()).build();

        return DpsRequest.builder()
                .infDPS(DpsRequest.InfDPS.builder()
                        .dhEmi(OffsetDateTime.now())
                        .pEmi("1")
                        .tpAmb(ambiente)
                        .verAtu("1.00")
                        .prest(DpsRequest.Prestador.builder()
                                .CNPJ(prestadorCnpj)
                                .IM(prestadorIm)
                                .build())
                        .toma(DpsRequest.Tomador.builder()
                                .identif(identif)
                                .nNome(customer.getName())
                                .build())
                        .serv(DpsRequest.Servico.builder()
                                .locServ(DpsRequest.LocServ.builder()
                                        .cMunServ(req.getCityCode())
                                        .build())
                                .idServ(DpsRequest.IdServ.builder()
                                        .cNBS(req.getNbsCode())
                                        .desc(req.getServiceDescription())
                                        .build())
                                .build())
                        .vals(DpsRequest.Valores.builder()
                                .vServ(req.getServiceValue())
                                .tribut(DpsRequest.Tributos.builder()
                                        .ibs(DpsRequest.Ibs.builder()
                                                .pAliq(taxes.getIbsRate())
                                                .vIBS(taxes.getIbsValue())
                                                .build())
                                        .cbs(DpsRequest.Cbs.builder()
                                                .pAliq(taxes.getCbsRate())
                                                .vCBS(taxes.getCbsValue())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private FiscalOrigin parseOrigin(String origin) {
        if (origin == null) return FiscalOrigin.MANUAL;
        try {
            return FiscalOrigin.valueOf(origin.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FiscalOrigin.MANUAL;
        }
    }

    private InvoiceEmissionResponseDTO toResponseDTO(FiscalDocument doc, DpsResponse portal, TaxInfo taxes) {
        return InvoiceEmissionResponseDTO.builder()
                .id(doc.getId())
                .accessKey(doc.getAccessKey())
                .protocol(portal.getProtocol())
                .status(doc.getStatus().name())
                .issueDate(doc.getIssueDate())
                .processingDate(portal.getDhProcessamento())
                .serviceValue(doc.getTotalValue())
                .taxes(InvoiceEmissionResponseDTO.TaxInfoDTO.builder()
                        .ibsRate(taxes.getIbsRate()).ibsValue(taxes.getIbsValue())
                        .cbsRate(taxes.getCbsRate()).cbsValue(taxes.getCbsValue())
                        .isqnRate(taxes.getIsqnRate()).isqnValue(taxes.getIsqnValue())
                        .totalTaxValue(taxes.getTotalTaxValue())
                        .build())
                .build();
    }
}

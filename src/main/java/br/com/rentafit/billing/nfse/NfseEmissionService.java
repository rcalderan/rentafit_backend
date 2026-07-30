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
import java.time.LocalDate;
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
    private final NfseDpsXsdValidator xsdValidator;
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

    @Value("${nfs-e.prestador.cLocEmi:3550308}")
    private String cLocEmi;

    @Value("${nfs-e.dps.serie:1}")
    private String serieDps;

    @Value("${nfs-e.dps.cTribNac:140201}")
    private String cTribNacPadrao;

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
                    xsdValidator.validate(rawXml);
                    String signedXml = xmlSigner.sign(rawXml);
                    return new Object[]{c, taxes, signedXml};
                })
                .onErrorMap(e -> !(e instanceof ResourceNotFoundException)
                                && !(e instanceof IllegalStateException)
                                && !(e instanceof br.com.rentafit.common.exception.ValidationException)
                                ? new IllegalStateException("Falha ao preparar emissão: " + e.getMessage(), e)
                                : e)
                .flatMap(parts -> {
                    Customer customer = (Customer) parts[0];
                    TaxInfo taxes = (TaxInfo) parts[1];
                    String signedXml = (String) parts[2];
                    return portalClient.sendDps(signedXml)
                            .map(response -> persistirEMapear(customer, request, taxes, signedXml, response));
                });
    }

    private InvoiceEmissionResponseDTO persistirEMapear(
            Customer customer, InvoiceEmissionRequestDTO request,
            TaxInfo taxes, String signedXml, DpsResponse response) {

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
                .serviceDescription(request.getServiceDescription())
                .signedXml(signedXml)
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
        String doc = customer.getDocument();
        boolean isCnpj = doc.length() == 14;
        String tpInsc = isCnpj ? "1" : "2";
        String inscFed = isCnpj ? doc : String.format("%14s", doc).replace(' ', '0');
        String serie = String.format("%05d", Integer.parseInt(serieDps));
        String nDPS = String.format("%015d", System.currentTimeMillis() % 1000000000000000L);
        String dpsId = "DPS" + cLocEmi + tpInsc + inscFed + serie + nDPS;

        DpsRequest.Prestador prest = DpsRequest.Prestador.builder()
                .CNPJ(prestadorCnpj)
                .IM(prestadorIm != null && !prestadorIm.isBlank() ? prestadorIm : null)
                .regTrib(DpsRequest.RegTrib.builder()
                        .opSimpNac("1")
                        .regEspTrib("0")
                        .build())
                .build();

        DpsRequest.Tomador.TomadorBuilder tomaBuilder = DpsRequest.Tomador.builder()
                .xNome(customer.getName());
        if (isCnpj) {
            tomaBuilder.CNPJ(doc);
        } else {
            tomaBuilder.CPF(doc);
        }

        return DpsRequest.builder()
                .versao("1.01")
                .infDPS(DpsRequest.InfDPS.builder()
                        .id(dpsId)
                        .tpAmb(ambiente)
                        .dhEmi(OffsetDateTime.now())
                        .verAplic("Rentafit-1.00")
                        .serie(serie)
                        .nDPS(nDPS)
                        .dCompet(LocalDate.now())
                        .tpEmit("1")
                        .cLocEmi(cLocEmi)
                        .prest(prest)
                        .toma(tomaBuilder.build())
                        .serv(DpsRequest.Servico.builder()
                                .locPrest(DpsRequest.LocPrest.builder()
                                        .cLocPrestacao(req.getCityCode())
                                        .build())
                                .cServ(DpsRequest.CServ.builder()
                                        .cTribNac(cTribNacPadrao)
                                        .xDescServ(req.getServiceDescription())
                                        .cNBS(req.getNbsCode())
                                        .build())
                                .build())
                        .valores(DpsRequest.Valores.builder()
                                .vServPrest(DpsRequest.VServPrest.builder()
                                        .vServ(req.getServiceValue())
                                        .build())
                                .trib(DpsRequest.Trib.builder()
                                        .tribMun(DpsRequest.TribMun.builder()
                                                .tribISSQN("1")
                                                .tpRetISSQN("1")
                                                .build())
                                        .totTrib(DpsRequest.TotTrib.builder()
                                                .indTotTrib("0")
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

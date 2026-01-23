package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.TaxInfo;
import br.com.rentafit.billing.domain.Invoice;
import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final NfsePortalService nfsePortalService;
    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;

    @Value("${nfs-e.prestador.cnpj:00000000000000}")
    private String prestadorCnpj;

    @Value("${nfs-e.prestador.im:}")
    private String prestadorInscricaoMunicipal;

    @Value("${nfs-e.ambiente:2}")
    private String ambiente; // 1=Produção, 2=Homologação

    @Value("${nfs-e.tributos.ibs.aliquota:0.025}")
    private BigDecimal ibsAliquotaPadrao;

    @Value("${nfs-e.tributos.cbs.aliquota:0.015}")
    private BigDecimal cbsAliquotaPadrao;

    public Mono<InvoiceEmissionResponseDTO> emitInvoice(InvoiceEmissionRequestDTO request) {
        log.info("Iniciando emissão de NFS-e para cliente: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + request.getCustomerId()));

        // Calcula tributos
        TaxInfo taxInfo = calcularTributos(request);

        // Constrói o DPS conforme NT 004
        DpsRequest dpsRequest = buildDpsRequest(customer, request, taxInfo);

        return nfsePortalService.sendDps(dpsRequest)
                .map(response -> {
                    Invoice invoice = Invoice.builder()
                            .accessKey(response.getAccessKey())
                            .invoiceNumber(Long.parseLong(response.getProtocol().substring(0, 10))) // Extrair do protocolo
                            .customer(customer)
                            .issueDate(OffsetDateTime.now())
                            .serviceValue(request.getServiceValue())
                            .status(Invoice.InvoiceStatus.AUTHORIZED)
                            .taxes(taxInfo)
                            .build();

                    Invoice savedInvoice = invoiceService.save(invoice);
                    log.info("NFS-e emitida e salva com sucesso: {}", savedInvoice.getAccessKey());

                    return mapToResponseDTO(savedInvoice, response);
                });
    }

    private TaxInfo calcularTributos(InvoiceEmissionRequestDTO request) {
        BigDecimal serviceValue = request.getServiceValue();

        // Usa alíquotas informadas ou padrão
        BigDecimal ibsRate = request.getIbsRate() != null ? request.getIbsRate() : ibsAliquotaPadrao;
        BigDecimal cbsRate = request.getCbsRate() != null ? request.getCbsRate() : cbsAliquotaPadrao;
        BigDecimal isqnRate = request.getIsqnRate() != null ? request.getIsqnRate() : BigDecimal.ZERO;

        // Calcula valores dos tributos
        BigDecimal ibsValue = serviceValue.multiply(ibsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cbsValue = serviceValue.multiply(cbsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal isqnValue = serviceValue.multiply(isqnRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTax = ibsValue.add(cbsValue).add(isqnValue);

        log.info("Tributos calculados - IBS: {}, CBS: {}, ISQN: {}, Total: {}",
                ibsValue, cbsValue, isqnValue, totalTax);

        return TaxInfo.builder()
                .ibsRate(ibsRate)
                .ibsValue(ibsValue)
                .cbsRate(cbsRate)
                .cbsValue(cbsValue)
                .isqnRate(isqnRate)
                .isqnValue(isqnValue)
                .totalTaxValue(totalTax)
                .build();
    }

    private DpsRequest buildDpsRequest(Customer customer, InvoiceEmissionRequestDTO request, TaxInfo taxInfo) {
        // Monta identificação do tomador (CPF ou CNPJ)
        DpsRequest.Identificacao identificacao = DpsRequest.Identificacao.builder()
                .CNPJ(customer.getDocument().length() == 14 ? customer.getDocument() : null)
                .CPF(customer.getDocument().length() == 11 ? customer.getDocument() : null)
                .build();

        // Monta endereço do tomador (se disponível)
        DpsRequest.Endereco endereco = null;
        if (customer.getCurrentAddress() != null && customer.getCurrentAddress().getAddress() != null) {
            var addressDetails = customer.getCurrentAddress();
            var address = addressDetails.getAddress();
            endereco = DpsRequest.Endereco.builder()
                    .lograd(address.getStreet())
                    .nNum(addressDetails.getNumber())
                    .cMun(address.getCity())
                    .UF(address.getState())
                    .CEP(address.getZipCode())
                    .build();
        }

        // Monta tributos
        DpsRequest.Tributos tributos = DpsRequest.Tributos.builder()
                .ibs(DpsRequest.Ibs.builder()
                        .pAliq(taxInfo.getIbsRate())
                        .vIBS(taxInfo.getIbsValue())
                        .build())
                .cbs(DpsRequest.Cbs.builder()
                        .pAliq(taxInfo.getCbsRate())
                        .vCBS(taxInfo.getCbsValue())
                        .build())
                .build();

        // Monta valores
        DpsRequest.Valores valores = DpsRequest.Valores.builder()
                .vServ(request.getServiceValue())
                .tribut(tributos)
                .build();

        // Monta serviço
        DpsRequest.Servico servico = DpsRequest.Servico.builder()
                .locServ(DpsRequest.LocServ.builder()
                        .cMunServ(request.getCityCode())
                        .build())
                .idServ(DpsRequest.IdServ.builder()
                        .cNBS(request.getNbsCode())
                        .desc(request.getServiceDescription())
                        .build())
                .build();

        // Monta DPS completo
        return DpsRequest.builder()
                .infDPS(DpsRequest.InfDPS.builder()
                        .dhEmi(OffsetDateTime.now())
                        .pEmi("1") // 1=Próprio prestador
                        .tpAmb(ambiente)
                        .verAtu("1.00") // Versão do layout
                        .prest(DpsRequest.Prestador.builder()
                                .CNPJ(prestadorCnpj)
                                .IM(prestadorInscricaoMunicipal)
                                .build())
                        .toma(DpsRequest.Tomador.builder()
                                .identif(identificacao)
                                .nNome(customer.getName())
                                .end(endereco)
                                .build())
                        .serv(servico)
                        .vals(valores)
                        .build())
                .build();
    }

    private InvoiceEmissionResponseDTO mapToResponseDTO(Invoice invoice,
            br.com.rentafit.billing.dto.DpsResponse portalResponse) {
        return InvoiceEmissionResponseDTO.builder()
                .id(invoice.getId())
                .accessKey(invoice.getAccessKey())
                .invoiceNumber(invoice.getInvoiceNumber())
                .protocol(portalResponse.getProtocol())
                .status(invoice.getStatus().name())
                .issueDate(invoice.getIssueDate())
                .processingDate(portalResponse.getDhProcessamento())
                .serviceValue(invoice.getServiceValue())
                .taxes(InvoiceEmissionResponseDTO.TaxInfoDTO.builder()
                        .ibsRate(invoice.getTaxes().getIbsRate())
                        .ibsValue(invoice.getTaxes().getIbsValue())
                        .cbsRate(invoice.getTaxes().getCbsRate())
                        .cbsValue(invoice.getTaxes().getCbsValue())
                        .isqnRate(invoice.getTaxes().getIsqnRate())
                        .isqnValue(invoice.getTaxes().getIsqnValue())
                        .totalTaxValue(invoice.getTaxes().getTotalTaxValue())
                        .build())
                .build();
    }
}

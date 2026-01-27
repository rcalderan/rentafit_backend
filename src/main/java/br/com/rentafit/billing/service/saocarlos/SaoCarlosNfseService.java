package br.com.rentafit.billing.service.saocarlos;

import br.com.rentafit.billing.domain.saocarlos.SaoCarlosLoteRps;
import br.com.rentafit.billing.domain.saocarlos.SaoCarlosRps;
import br.com.rentafit.billing.dto.saocarlos.*;
import br.com.rentafit.billing.repository.saocarlos.SaoCarlosLoteRpsRepository;
import br.com.rentafit.billing.repository.saocarlos.SaoCarlosNfseRepository;
import br.com.rentafit.billing.repository.saocarlos.SaoCarlosRpsRepository;
import br.com.rentafit.billing.util.XmlUtils;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaoCarlosNfseService {

    private final WebClient saoCarlosWebClient;
    private final SaoCarlosAssinaturaService assinaturaService;
    private final XmlUtils xmlUtils;
    private final CustomerRepository customerRepository;
    private final SaoCarlosLoteRpsRepository loteRpsRepository;
    private final SaoCarlosRpsRepository rpsRepository;
    private final SaoCarlosNfseRepository nfseRepository;

    @Value("${nfs-e.saocarlos.prestador.cnpj}")
    private String cnpjPrestador;

    @Value("${nfs-e.saocarlos.prestador.im}")
    private String inscricaoMunicipalPrestador;

    @Value("${nfs-e.saocarlos.codigo-municipio}")
    private String codigoMunicipioPrestador;

    private static final String GINFES_NS = "http://www.ginfes.com.br/servico_enviar_lote_rps_envio_v03.xsd";
    private static final String TIPOS_NS = "http://www.ginfes.com.br/tipos_v03.xsd";

    /**
     * Emite uma NFS-e através do envio de lote de RPS.
     */
    @Transactional
    public Mono<SaoCarlosEmitirNfseResponseDTO> emitirNfse(SaoCarlosEmitirNfseRequestDTO request) {
        log.info("Iniciando emissão de NFS-e São Carlos para cliente: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + request.getCustomerId()));

        // Gera número do lote
        String numeroLote = gerarNumeroLote();

        // Cria o lote no banco
        SaoCarlosLoteRps lote = SaoCarlosLoteRps.builder()
                .numeroLote(numeroLote)
                .customer(customer)
                .cnpjPrestador(cnpjPrestador)
                .inscricaoMunicipalPrestador(inscricaoMunicipalPrestador)
                .quantidadeRps(request.getRps().size())
                .situacao(SaoCarlosLoteRps.SituacaoLote.PENDENTE)
                .build();
        //final SaoCarlosLoteRps loteSalvo = loteRpsRepository.save(lote);

        // Salva os RPS no banco
//        for (SaoCarlosRpsDTO rpsDTO : request.getRps()) {
//            SaoCarlosRps rps = mapearRpsParaEntidade(rpsDTO, loteSalvo, customer);
//            rpsRepository.save(rps);
//        }

        try {
            // Monta o XML do lote
            String xmlLote = montarXmlLoteRps(numeroLote, request.getRps());
            log.info("XML do lote gerado com sucesso. Tamanho: {} bytes", xmlLote.length());
            log.debug("XML do lote gerado:\n{}", xmlLote);

            // Valida o XML antes de assinar
            if (xmlLote == null || xmlLote.trim().isEmpty()) {
                throw new IllegalStateException("XML do lote está vazio");
            }

            // Assina o XML (retorna como String)
            log.info("Assinando XML do lote...");
            String xmlAssinado = assinaturaService.assinarXmlString(xmlLote, "lote" + numeroLote);

            if (xmlAssinado == null) {
                throw new IllegalStateException("Falha ao assinar XML - retorno nulo");
            }

            log.info("XML assinado com sucesso. Tamanho: {} bytes", xmlAssinado.length());

            // Envelopa em SOAP
            String soapEnvelope = montarSoapEnvelope(xmlAssinado);
            log.debug("SOAP Envelope criado com sucesso");

            // Atualiza o lote com o XML
//            loteSalvo.setXmlEnviado(xmlAssinado);
//            loteSalvo.setDataEnvio(OffsetDateTime.now());
//            loteSalvo.setSituacao(SaoCarlosLoteRps.SituacaoLote.ENVIADO);
//            loteRpsRepository.save(loteSalvo);

            // Envia para o webservice
            return enviarLoteRps(soapEnvelope)
                    .map(xmlResposta -> processarResposta(lote, xmlResposta))
                    .onErrorResume(error -> {
                        log.error("Erro ao enviar lote RPS: {}", error.getMessage(), error);
                        lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.ERRO_ENVIO);
                        lote.setMensagemErro(error.getMessage());
                        //loteRpsRepository.save(loteSalvo);
                        return Mono.error(new RuntimeException("Erro ao enviar lote RPS: " + error.getMessage()));
                    });

        } catch (Exception e) {
            log.error("Erro ao processar emissão de NFS-e: {}", e.getMessage(), e);
            lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.ERRO_ENVIO);
            lote.setMensagemErro(e.getMessage());
            //loteRpsRepository.save(loteSalvo);
            return Mono.error(new RuntimeException("Erro ao processar emissão de NFS-e: " + e.getMessage()));
        }
    }

    /**
     * Consulta a situação do lote por protocolo.
     */
    public Mono<SaoCarlosEmitirNfseResponseDTO> consultarSituacaoLote(String protocolo) {
        log.info("Consultando situação do lote com protocolo: {}", protocolo);

        try {
            String soapEnvelope = montarSoapConsultaSituacaoLote(protocolo);

            return saoCarlosWebClient.post()
                    .bodyValue(soapEnvelope)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(this::processarRespostaConsultaSituacao);

        } catch (Exception e) {
            log.error("Erro ao consultar situação do lote: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Erro ao consultar situação do lote: " + e.getMessage()));
        }
    }

    /**
     * Consulta o lote completo por protocolo (retorna as NFS-e).
     */
    @Transactional
    public Mono<SaoCarlosEmitirNfseResponseDTO> consultarLoteRps(String protocolo) {
        log.info("Consultando lote RPS com protocolo: {}", protocolo);

        SaoCarlosLoteRps lote = loteRpsRepository.findByProtocolo(protocolo)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado: " + protocolo));

        try {
            String soapEnvelope = montarSoapConsultaLote(protocolo);

            return saoCarlosWebClient.post()
                    .bodyValue(soapEnvelope)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(xmlResposta -> processarRespostaConsultaLote(lote, xmlResposta));

        } catch (Exception e) {
            log.error("Erro ao consultar lote RPS: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Erro ao consultar lote RPS: " + e.getMessage()));
        }
    }

    // ==================== Métodos Auxiliares ====================

    private Mono<String> enviarLoteRps(String soapEnvelope) {
        return saoCarlosWebClient.post()
                .bodyValue(soapEnvelope)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.debug("Resposta recebida do webservice:\n{}", response))
                .doOnError(error -> log.error("Erro na comunicação com webservice: {}", error.getMessage()));
    }

    private String montarXmlLoteRps(String numeroLote, List<SaoCarlosRpsDTO> rpsList) throws Exception {
        log.info("Montando XML do lote {} com {} RPS", numeroLote, rpsList.size());

        Document doc = xmlUtils.createDocument();

        if (doc == null) {
            throw new IllegalStateException("createDocument() retornou null");
        }

        // Elemento raiz: EnviarLoteRpsEnvio
        Element root = doc.createElementNS(GINFES_NS, "EnviarLoteRpsEnvio");
        root.setAttribute("xmlns", GINFES_NS);
        root.setAttribute("xmlns:tipos", TIPOS_NS);
        doc.appendChild(root);

        // LoteRps
        Element loteRps = xmlUtils.addElementNS(doc, root, TIPOS_NS, "tipos:LoteRps", null);
        loteRps.setAttribute("Id", "lote" + numeroLote);
        loteRps.setAttribute("versao", "3.00");

        // NumeroLote
        xmlUtils.addElementNS(doc, loteRps, TIPOS_NS, "tipos:NumeroLote", numeroLote);

        // Cnpj do Prestador
        xmlUtils.addElementNS(doc, loteRps, TIPOS_NS, "tipos:Cnpj", cnpjPrestador);

        // InscricaoMunicipal do Prestador
        if (inscricaoMunicipalPrestador != null && !inscricaoMunicipalPrestador.isEmpty()) {
            xmlUtils.addElementNS(doc, loteRps, TIPOS_NS, "tipos:InscricaoMunicipal", inscricaoMunicipalPrestador);
        }

        // QuantidadeRps
        xmlUtils.addElementNS(doc, loteRps, TIPOS_NS, "tipos:QuantidadeRps", String.valueOf(rpsList.size()));

        // ListaRps
        Element listaRps = xmlUtils.addElementNS(doc, loteRps, TIPOS_NS, "tipos:ListaRps", null);

        // Adiciona cada RPS
        for (SaoCarlosRpsDTO rpsDTO : rpsList) {
            adicionarRpsAoXml(doc, listaRps, rpsDTO);
        }

        String xmlResult = xmlUtils.documentToString(doc);
        log.info("XML do lote montado com sucesso. Tamanho: {} bytes", xmlResult.length());

        return xmlResult;
    }

    private void adicionarRpsAoXml(Document doc, Element listaRps, SaoCarlosRpsDTO rpsDTO) {
        Element rps = xmlUtils.addElementNS(doc, listaRps, TIPOS_NS, "tipos:Rps", null);

        // InfDeclaracaoPrestacaoServico
        Element infDeclaracao = xmlUtils.addElementNS(doc, rps, TIPOS_NS, "tipos:InfDeclaracaoPrestacaoServico", null);

        // Identificação do RPS
        Element identificacaoRps = xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:IdentificacaoRps", null);
        xmlUtils.addElementNS(doc, identificacaoRps, TIPOS_NS, "tipos:Numero", String.valueOf(rpsDTO.getNumero()));
        xmlUtils.addElementNS(doc, identificacaoRps, TIPOS_NS, "tipos:Serie", rpsDTO.getSerie());
        xmlUtils.addElementNS(doc, identificacaoRps, TIPOS_NS, "tipos:Tipo", String.valueOf(rpsDTO.getTipo()));

        // DataEmissao
        xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:DataEmissao",
                xmlUtils.formatDateTime(rpsDTO.getDataEmissao()));

        // Status
        xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:Status", String.valueOf(rpsDTO.getStatus()));

        // Serviço
        Element servico = xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:Servico", null);
        Element valores = xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:Valores", null);

        xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorServicos", xmlUtils.formatDecimal(rpsDTO.getValorServicos()));

        if (rpsDTO.getValorDeducoes() != null && rpsDTO.getValorDeducoes().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorDeducoes", xmlUtils.formatDecimal(rpsDTO.getValorDeducoes()));
        }

        if (rpsDTO.getValorPis() != null && rpsDTO.getValorPis().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorPis", xmlUtils.formatDecimal(rpsDTO.getValorPis()));
        }

        if (rpsDTO.getValorCofins() != null && rpsDTO.getValorCofins().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorCofins", xmlUtils.formatDecimal(rpsDTO.getValorCofins()));
        }

        if (rpsDTO.getValorInss() != null && rpsDTO.getValorInss().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorInss", xmlUtils.formatDecimal(rpsDTO.getValorInss()));
        }

        if (rpsDTO.getValorIr() != null && rpsDTO.getValorIr().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorIr", xmlUtils.formatDecimal(rpsDTO.getValorIr()));
        }

        if (rpsDTO.getValorCsll() != null && rpsDTO.getValorCsll().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:ValorCsll", xmlUtils.formatDecimal(rpsDTO.getValorCsll()));
        }

        xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:Aliquota", xmlUtils.formatAliquota(rpsDTO.getAliquota()));

        if (rpsDTO.getDescontoIncondicionado() != null && rpsDTO.getDescontoIncondicionado().compareTo(BigDecimal.ZERO) > 0) {
            xmlUtils.addElementNS(doc, valores, TIPOS_NS, "tipos:DescontoIncondicionado",
                    xmlUtils.formatDecimal(rpsDTO.getDescontoIncondicionado()));
        }

        xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:IssRetido", String.valueOf(rpsDTO.getSimplesNacional()));
        xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:ItemListaServico", rpsDTO.getItemListaServico());

        if (rpsDTO.getCodigoCnae() != null && !rpsDTO.getCodigoCnae().isEmpty()) {
            xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:CodigoCnae", rpsDTO.getCodigoCnae());
        }

        xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:Discriminacao", rpsDTO.getDiscriminacao());
        xmlUtils.addElementNS(doc, servico, TIPOS_NS, "tipos:CodigoMunicipio", String.valueOf(rpsDTO.getCodigoMunicipio()));

        // Prestador
        Element prestador = xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:Prestador", null);
        Element cpfCnpjPrestador = xmlUtils.addElementNS(doc, prestador, TIPOS_NS, "tipos:CpfCnpj", null);
        xmlUtils.addElementNS(doc, cpfCnpjPrestador, TIPOS_NS, "tipos:Cnpj", cnpjPrestador);

        if (inscricaoMunicipalPrestador != null && !inscricaoMunicipalPrestador.isEmpty()) {
            xmlUtils.addElementNS(doc, prestador, TIPOS_NS, "tipos:InscricaoMunicipal", inscricaoMunicipalPrestador);
        }

        // RegimeEspecialTributacao e OptanteSimplesNacional
        xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:OptanteSimplesNacional",
                String.valueOf(rpsDTO.getSimplesNacional()));
        xmlUtils.addElementNS(doc, infDeclaracao, TIPOS_NS, "tipos:IncentivadorCultural",
                String.valueOf(rpsDTO.getIncentivadorCultural()));
    }

    private String montarSoapEnvelope(String xmlBody) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
               "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
               "<soap:Body>" +
               xmlBody +
               "</soap:Body>" +
               "</soap:Envelope>";
    }

    private String montarSoapConsultaSituacaoLote(String protocolo) throws Exception {
        Document doc = xmlUtils.createDocument();

        Element root = doc.createElementNS(
                "http://www.ginfes.com.br/servico_consultar_situacao_lote_rps_envio_v03.xsd",
                "ConsultarSituacaoLoteRpsEnvio");
        doc.appendChild(root);

        Element prestador = xmlUtils.addElement(doc, root, "Prestador", null);
        Element cpfCnpj = xmlUtils.addElement(doc, prestador, "CpfCnpj", null);
        xmlUtils.addElement(doc, cpfCnpj, "Cnpj", cnpjPrestador);

        if (inscricaoMunicipalPrestador != null && !inscricaoMunicipalPrestador.isEmpty()) {
            xmlUtils.addElement(doc, prestador, "InscricaoMunicipal", inscricaoMunicipalPrestador);
        }

        xmlUtils.addElement(doc, root, "Protocolo", protocolo);

        return montarSoapEnvelope(xmlUtils.documentToString(doc));
    }

    private String montarSoapConsultaLote(String protocolo) throws Exception {
        Document doc = xmlUtils.createDocument();

        Element root = doc.createElementNS(
                "http://www.ginfes.com.br/servico_consultar_lote_rps_envio_v03.xsd",
                "ConsultarLoteRpsEnvio");
        doc.appendChild(root);

        Element prestador = xmlUtils.addElement(doc, root, "Prestador", null);
        Element cpfCnpj = xmlUtils.addElement(doc, prestador, "CpfCnpj", null);
        xmlUtils.addElement(doc, cpfCnpj, "Cnpj", cnpjPrestador);

        if (inscricaoMunicipalPrestador != null && !inscricaoMunicipalPrestador.isEmpty()) {
            xmlUtils.addElement(doc, prestador, "InscricaoMunicipal", inscricaoMunicipalPrestador);
        }

        xmlUtils.addElement(doc, root, "Protocolo", protocolo);

        return montarSoapEnvelope(xmlUtils.documentToString(doc));
    }

    @Transactional
    private SaoCarlosEmitirNfseResponseDTO processarResposta(SaoCarlosLoteRps lote, String xmlResposta) {
        try {
            log.debug("Processando resposta do webservice:\n{}", xmlUtils.prettyPrint(xmlResposta));

            lote.setXmlResposta(xmlResposta);
            lote.setDataRecebimento(OffsetDateTime.now());

            Document doc = xmlUtils.stringToDocument(xmlResposta);

            // Extrai o protocolo
            String protocolo = xmlUtils.getElementText(doc.getDocumentElement(), "Protocolo");
            if (protocolo != null) {
                lote.setProtocolo(protocolo);
                lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.ENVIADO);
            }

            loteRpsRepository.save(lote);

            return SaoCarlosEmitirNfseResponseDTO.builder()
                    .protocolo(protocolo)
                    .dataRecebimento(LocalDateTime.now())
                    .situacao(1) // Não processado ainda
                    .build();

        } catch (Exception e) {
            log.error("Erro ao processar resposta: {}", e.getMessage(), e);
            lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.ERRO_ENVIO);
            lote.setMensagemErro("Erro ao processar resposta: " + e.getMessage());
            loteRpsRepository.save(lote);
            throw new RuntimeException("Erro ao processar resposta", e);
        }
    }

    private SaoCarlosEmitirNfseResponseDTO processarRespostaConsultaSituacao(String xmlResposta) {
        try {
            log.debug("Processando resposta de consulta de situação:\n{}", xmlUtils.prettyPrint(xmlResposta));

            Document doc = xmlUtils.stringToDocument(xmlResposta);
            String situacao = xmlUtils.getElementText(doc.getDocumentElement(), "Situacao");

            return SaoCarlosEmitirNfseResponseDTO.builder()
                    .situacao(situacao != null ? Integer.parseInt(situacao) : null)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao processar resposta de situação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar resposta de situação", e);
        }
    }

    @Transactional
    private SaoCarlosEmitirNfseResponseDTO processarRespostaConsultaLote(SaoCarlosLoteRps lote, String xmlResposta) {
        try {
            log.debug("Processando resposta de consulta de lote:\n{}", xmlUtils.prettyPrint(xmlResposta));

            lote.setXmlResposta(xmlResposta);
            Document doc = xmlUtils.stringToDocument(xmlResposta);

            // TODO: Parsear as NFS-e geradas e salvar no banco
            // Aqui você precisa implementar a lógica de extração das NFS-e do XML de resposta

            lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.PROCESSADO);
            loteRpsRepository.save(lote);

            return SaoCarlosEmitirNfseResponseDTO.builder()
                    .protocolo(lote.getProtocolo())
                    .situacao(2) // Processado com sucesso
                    .build();

        } catch (Exception e) {
            log.error("Erro ao processar resposta de lote: {}", e.getMessage(), e);
            lote.setSituacao(SaoCarlosLoteRps.SituacaoLote.PROCESSADO_ERRO);
            lote.setMensagemErro("Erro ao processar resposta: " + e.getMessage());
            loteRpsRepository.save(lote);
            throw new RuntimeException("Erro ao processar resposta de lote", e);
        }
    }

    private SaoCarlosRps mapearRpsParaEntidade(SaoCarlosRpsDTO dto, SaoCarlosLoteRps lote, Customer customer) {
        return SaoCarlosRps.builder()
                .lote(lote)
                .customer(customer)
                .numeroRps(dto.getNumero())
                .serieRps(dto.getSerie())
                .tipoRps(dto.getTipo())
                .dataEmissao(dto.getDataEmissao())
                .naturezaOperacao(dto.getNaturezaOperacao())
                .regimeEspecialTributacao(dto.getRegimeEspecialTributacao())
                .simplesNacional(dto.getSimplesNacional())
                .incentivadorCultural(dto.getIncentivadorCultural())
                .statusRps(dto.getStatus())
                .valorServicos(dto.getValorServicos())
                .valorDeducoes(dto.getValorDeducoes())
                .valorPis(dto.getValorPis())
                .valorCofins(dto.getValorCofins())
                .valorInss(dto.getValorInss())
                .valorIr(dto.getValorIr())
                .valorCsll(dto.getValorCsll())
                .valorIssRetido(dto.getValorIssRetido())
                .valorOutrasRetencoes(dto.getValorOutrasRetencoes())
                .aliquota(dto.getAliquota())
                .descontoIncondicionado(dto.getDescontoIncondicionado())
                .descontoCondicionado(dto.getDescontoCondicionado())
                .itemListaServico(dto.getItemListaServico())
                .codigoCnae(dto.getCodigoCnae())
                .codigoTributacaoMunicipio(dto.getCodigoTributacaoMunicipio())
                .discriminacao(dto.getDiscriminacao())
                .codigoMunicipio(dto.getCodigoMunicipio())
                .build();
    }

    private String gerarNumeroLote() {
        // Gera um número de lote único baseado em timestamp
        return String.valueOf(System.currentTimeMillis() % 9999999999999L);
    }
}

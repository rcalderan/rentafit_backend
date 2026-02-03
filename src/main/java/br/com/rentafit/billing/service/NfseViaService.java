package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.via.*;
import br.com.rentafit.billing.util.NfseViaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

@Service
@Slf4j
public class NfseViaService {

    private final WebClient recepcaoWebClient;
    private final WebClient consultasWebClient;
    private final KeyStore nfsKeyStore;
    private final String certificatePassword;

    public NfseViaService(@Qualifier("nfseViaRecepcaoWebClient") WebClient recepcaoWebClient,
                          @Qualifier("nfseViaConsultasWebClient") WebClient consultasWebClient,
                          @org.springframework.lang.Nullable @Qualifier("nfsKeyStore") KeyStore nfsKeyStore,
                          @Value("${nfs-e.certificate.password:}") String certificatePassword) {
        this.recepcaoWebClient = recepcaoWebClient;
        this.consultasWebClient = consultasWebClient;
        this.nfsKeyStore = nfsKeyStore;
        this.certificatePassword = certificatePassword;
    }



    /**
     * Receber e validar NFS-e Via.
     * Após o sucesso na recepção, consulta automaticamente o status do protocolo.
     * O protocolo retornado já vem limpo (sem pontos ou barras).
     */
    public Mono<Object> receberNfse(RecepcaoRequest request) {
        log.info("Processando e assinando NFS-e Via para recepção. Identificador: {}", request.getIdentificador());

        request.setNotaFiscalViaXmlGZipBase64(signAndEncode(request.getNotaFiscalViaXmlGZipBase64()));

        return recepcaoWebClient.post()
                .uri("/nfsev")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecepcaoResponse.class)
                .flatMap(recepcaoResponse -> {
                    System.out.println(recepcaoResponse);
                    //log.info("NFS-e Via recebida. Protocolo: {}. Consultando resultado...", recepcaoResponse.getProtocolo());
                    return consultarPorChaveAcesso(recepcaoResponse.getChaveAcesso());
                    //return consultarPorProtocolo(recepcaoResponse.getProtocolo());
                })
                .doOnError(err -> log.error("Erro no processo de recepção/consulta NFS-e Via: {}", err.getMessage()));
    }

    /**
     * Compoe o Id da infDPS conforme regra: DPS + cLocEmi(7) + tipoInscricao(1) + CNPJ(14) + serie(5) + nDPS(15)
     */
    private String composeDpsId(String cLocEmi, String tipoInscricao, String cnpjPrestador, String serie, String numeroDps) {
        String cLocEmi7 = String.format("%07d", Integer.parseInt(cLocEmi));
        String cnpj14 = String.format("%014d", Long.parseLong(cnpjPrestador));
        String serie5 = String.format("%05d", Integer.parseInt(serie));
        String nDps15 = String.format("%015d", Integer.parseInt(numeroDps));
        return "DPS" + cLocEmi7 + tipoInscricao + cnpj14 + serie5 + nDps15;
    }

    /**
     * Método de teste para receber uma NFS-e Via de exemplo.
     */
    public Mono<Object> testeNfse() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        log.info("Executando teste NFS-e Via...");
        String cnpjConcessionaria = "00000000000191";
        String cLocEmi = "3550308";
        String tipoInscricao = "2"; // CNPJ
        String serie = "1"; // será preenchido com zeros à esquerda (5 dígitos)
        String numeroDps = "1"; // será preenchido com zeros à esquerda (15 dígitos)
        String identificadorCompleto = composeDpsId(cLocEmi, tipoInscricao, cnpjConcessionaria, serie, numeroDps);
        // O identificador nos metadados (JSON) deve ser apenas a parte numérica, SEM o prefixo "DPS"
        String identificadorMetadados = identificadorCompleto.substring(3);

        var request = RecepcaoRequest.builder()
                .cnpjConcessionaria(cnpjConcessionaria)
                .identificador(identificadorMetadados) // Apenas os 42 dígitos
                .notaFiscalViaXmlGZipBase64(this.getXml(cnpjConcessionaria, identificadorCompleto, cLocEmi, serie, numeroDps))
                .build();
        return receberNfse(request);
    }

    private String getXml(String cnpj,
                          String identificador,
                          String cLocEmi,
                          String serie,
                          String numeroDps) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

        if (nfsKeyStore == null) {
            throw new IllegalStateException("KeyStore não inicializado. Certificado digital é obrigatório para assinar XML.");
        }

        // Monta DPS apenas (sem wrapper NFSe) e com Id consistente
        String versaoAplic = "Teste_1.0";
        String descricaoServ = "SERVICOS DE CONSULTORIA EM TI";

        // Padding coerente com o Id
        String serie5 = String.format("%05d", Integer.parseInt(serie));
        String numeroDps15 = String.format("%015d", Integer.parseInt(numeroDps));

        String dpsXml = NfseViaUtil.generateDpsXml(identificador,
                cnpj,
                "00000000000272",
                serie5,
                numeroDps15,
                cLocEmi,
                cLocEmi,
                versaoAplic,
                descricaoServ);

        // Extrair Chave Privada e Certificado do KeyStore para assinar
        String alias = "";
        Enumeration<String> aliases = nfsKeyStore.aliases();
        if (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
        }

        PrivateKey privateKey = (PrivateKey) nfsKeyStore.getKey(alias, certificatePassword.toCharArray());
        X509Certificate cert = (X509Certificate) nfsKeyStore.getCertificate(alias);

        // Assinar o XML Digitalmente (XMLDSig) referenciando o Id da infDPS
        String signedXml = NfseViaUtil.signXml(dpsXml, identificador, privateKey, cert);
        return NfseViaUtil.compressAndEncode(signedXml);
    }


    /**
     * Receber e validar Evento de Cancelamento
     */
    public Mono<RecepcaoResponse> cancelarNfse(CancelamentoRequest request) {
        log.info("Processando e assinando cancelamento de NFS-e Via. Identificador: {}", request.getIdentificador());

        request.setEventoViaXmlGZipBase64(signAndEncode(request.getEventoViaXmlGZipBase64()));

        return recepcaoWebClient.post()
                .uri("/cancelamento")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecepcaoResponse.class)
                .doOnSuccess(res -> log.info("Evento de cancelamento recebido com sucesso. Protocolo: {}", res.getProtocolo()))
                .doOnError(err -> log.error("Erro ao cancelar NFS-e Via: {}", err.getMessage()));
    }

    /**
     * Receber NFS-e Via substituta e Evento de Cancelamento por Substituição
     */
    public Mono<RecepcaoResponse> substituirNfse(SubstituicaoRequest request) {
        log.info("Processando e assinando substituição de NFS-e Via. Identificador: {}", request.getIdentificador());

        request.setNotaFiscalViaXmlGZipBase64(signAndEncode(request.getNotaFiscalViaXmlGZipBase64()));
        request.setEventoViaXmlGZipBase64(signAndEncode(request.getEventoViaXmlGZipBase64()));

        return recepcaoWebClient.post()
                .uri("/substituicao")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecepcaoResponse.class)
                .doOnSuccess(res -> log.info("Substituição recebida com sucesso. Protocolo: {}", res.getProtocolo()))
                .doOnError(err -> log.error("Erro ao substituir NFS-e Via: {}", err.getMessage()));
    }

    /**
     * Helper para assinar, comprimir e codificar o conteúdo XML.
     */
    private String signAndEncode(String base64Gzip) {
        try {
            if (nfsKeyStore == null) {
                log.warn("Certificado não configurado. Enviando XML sem assinatura.");
                return base64Gzip;
            }

            // 1. Decodificar e descompactar
            String xml = NfseViaUtil.decodeAndDecompress(base64Gzip);

            // 2. Tentar identificar o ID para assinatura (infNFSe, infDPS ou Id de evento)
            String targetId = extractIdFromXml(xml);

            if (targetId == null) {
                log.warn("Não foi possível localizar um atributo 'Id' no XML para assinatura. Enviando sem assinatura digital interna.");
                return base64Gzip;
            }

            // 3. Obter credenciais do KeyStore
            String alias = "";
            Enumeration<String> aliases = nfsKeyStore.aliases();
            if (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
            }
            PrivateKey privateKey = (PrivateKey) nfsKeyStore.getKey(alias, certificatePassword.toCharArray());
            X509Certificate cert = (X509Certificate) nfsKeyStore.getCertificate(alias);

            // 4. Assinar
            String signedXml = NfseViaUtil.signXml(xml, targetId, privateKey, cert);

            // 5. Re-comprimir e re-codificar
            return NfseViaUtil.compressAndEncode(signedXml);

        } catch (Exception e) {
            log.error("Falha ao assinar conteúdo XML automaticamente: {}", e.getMessage());
            return base64Gzip; // Fallback para o original em caso de erro
        }
    }

    /**
     * Extrai o valor do atributo 'Id' de um elemento XML.
     * Busca tanto no formato correto (atributo do elemento raiz <DPS Id="...">)
     * quanto no formato antigo incorreto (<infDPS Id="...">) para compatibilidade.
     */
    private String extractIdFromXml(String xml) {
        if (xml == null) return null;

        // Primeiro tenta extrair do elemento raiz DPS (formato correto)
        int dpsPos = xml.indexOf("<DPS ");
        if (dpsPos != -1) {
            int idPos = xml.indexOf(" Id=\"", dpsPos);
            if (idPos != -1 && idPos < xml.indexOf(">", dpsPos)) {
                int start = idPos + 5;
                int end = xml.indexOf("\"", start);
                if (end != -1) {
                    return xml.substring(start, end);
                }
            }
        }

        // Fallback: busca em qualquer elemento com Id (formato antigo)
        int idPos = xml.indexOf(" Id=\"");
        if (idPos == -1) return null;
        int start = idPos + 5;
        int end = xml.indexOf("\"", start);
        if (end == -1) return null;
        return xml.substring(start, end);
    }

    /**
     * Recuperar alíquota efetiva por trecho e data
     */
    public Mono<Object> consultarAliquotaEfetiva(String cnpj, String codigoTrecho, String dataReferencia) {
        log.info("Consultando alíquota efetiva. CNPJ: {}, Trecho: {}, Data: {}", cnpj, codigoTrecho, dataReferencia);
        return consultasWebClient.get()
                .uri("/Aliquotas/efetiva/cnpj/{cnpj}/trecho/{codigoTrecho}/dataReferencia/{dataReferencia}",
                        cnpj, codigoTrecho, dataReferencia)
                .retrieve()
                .bodyToMono(Object.class)
                .doOnError(err -> log.error("Erro ao consultar alíquota efetiva: {}", err.getMessage()));
    }

    /**
     * Consultar resultado por protocolo.
     * O protocolo pode vir formatado (com . e /) mas a API espera no formato limpo.
     * Exemplo formatado: "2026012513270559612.345.678/000BE156DF3"
     * Exemplo limpo: "202601251327055961234567800BE156DF3"
     */
    public Mono<Object> consultarPorProtocolo(String protocolo) {

        return consultasWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consulta/protocolo")
                        .queryParam("protocolo", protocolo)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .doOnSuccess(res -> log.info("Resultado da consulta por protocolo {}: {}", protocolo, res))
                .doOnError(err -> log.error("Erro ao consultar protocolo {}: {} (URL base: {})",
                        protocolo, err.getMessage(), "hom-cert-api-nfsevia.np.estaleiro.serpro.gov.br"));
    }

    /**
     * Converte protocolo formatado para formato limpo (apenas números e letras).
     * "2026012513270559612.345.678/000BE156DF3" -> "202601251327055961234567800BE156DF3"
     */
    private String cleanProtocol(String protocolo) {
        if (protocolo == null || protocolo.isEmpty()) {
            return protocolo;
        }
        return protocolo.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Consultar resultado por chave de acesso.
     * A chave de acesso é enviada no formato 50 caracteres, sem formatação especial.
     */
    public Mono<Object> consultarPorChaveAcesso(String chaveAcesso) {
        log.info("Consultando resultado por chave de acesso: {}", chaveAcesso);
        return consultasWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consulta/chaveacesso")
                        .queryParam("chaveAcesso", chaveAcesso)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .doOnSuccess(res -> log.info("Resultado da consulta por chave de acesso {}: {}", chaveAcesso, res))
                .doOnError(err -> log.error("Erro ao consultar chave de acesso {}: {}", chaveAcesso, err.getMessage()));
    }

    /**
     * Cria um RecepcaoRequest pronto para envio, com DPS-only assinado (GZip+Base64).
     * Elimina qualquer chance de mismatch entre Id, metadado e assinatura.
     *
     * @param cnpjPrestador CNPJ do prestador (14 dígitos, sem formatação)
     * @param cLocEmi Código do município IBGE (7 dígitos)
     * @param serie Série da DPS (será padded para 5 dígitos)
     * @param numeroDps Número da DPS (será padded para 15 dígitos)
     * @param cnpjTomador CNPJ do tomador (14 dígitos, sem formatação)
     * @param descricaoServ Descrição do serviço
     * @return RecepcaoRequest pronto para envio
     * @throws Exception se houver erro na assinatura ou acesso ao keystore
     */
    public Mono<RecepcaoRequest> criarRecepcaoRequestAssinado(String cnpjPrestador,
                                                               String cLocEmi,
                                                               String serie,
                                                               String numeroDps,
                                                               String cnpjTomador,
                                                               String descricaoServ) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        log.info("Criando RecepcaoRequest assinado para: CNPJ={}, cLocEmi={}, série={}, nDPS={}",
                cnpjPrestador, cLocEmi, serie, numeroDps);

        // 1. Compor o Id da infDPS
        String idDps = composeDpsId(cLocEmi, "2", cnpjPrestador, serie, numeroDps);

        // 2. Preencher com padding coerente
        String serie5 = String.format("%05d", Integer.parseInt(serie));
        String numeroDps15 = String.format("%015d", Integer.parseInt(numeroDps));
        String cLocEmi7 = String.format("%07d", Integer.parseInt(cLocEmi));

        // 3. Gerar XML DPS-only
        String dpsXml = NfseViaUtil.generateDpsXml(
                idDps,
                cnpjPrestador,
                cnpjTomador,
                serie5,
                numeroDps15,
                cLocEmi7,
                cLocEmi7,
                "Rentafit_v1.0",
                descricaoServ
        );

        // 4. Extrair credenciais do KeyStore e assinar
        String alias = "";
        Enumeration<String> aliases = nfsKeyStore.aliases();
        if (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
        }

        PrivateKey privateKey = (PrivateKey) nfsKeyStore.getKey(alias, certificatePassword.toCharArray());
        X509Certificate cert = (X509Certificate) nfsKeyStore.getCertificate(alias);

        String signedXml = NfseViaUtil.signXml(dpsXml, idDps, privateKey, cert);
        String xmlGzipBase64 = NfseViaUtil.compressAndEncode(signedXml);

        // 5. Construir e retornar RecepcaoRequest
        RecepcaoRequest request = RecepcaoRequest.builder()
                .cnpjConcessionaria(cnpjPrestador)
                .identificador(idDps)
                .notaFiscalViaXmlGZipBase64(xmlGzipBase64)
                .build();

        log.info("RecepcaoRequest criado com identificador: {}", idDps);
        return Mono.just(request);
    }

}

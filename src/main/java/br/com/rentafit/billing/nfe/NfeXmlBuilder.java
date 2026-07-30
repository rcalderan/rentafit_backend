package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.people.domain.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Constrói o XML {@code infNFe} (modelo 55) conforme o leiaute NF-e v4.00.
 *
 * <p>Gera um documento mínimo, porém válido contra o schema oficial, com impostos
 * zerados/simplificados para emissão de teste/homologação.</p>
 *
 * <p>O elemento {@code infNFe} recebe atributo {@code Id="NFe"+chave44}.
 * Exemplo: {@code builder.buildXml(request, customer)} → String XML pronta para assinar.</p>
 */
@Component
public class NfeXmlBuilder {

    /** Formato exigido pelo schema para dhEmi: sem frações de segundo. */
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final String NFE_NS = "http://www.portalfiscal.inf.br/nfe";

    @Value("${nf-e.emit.cnpj:00000000000191}")
    private String emitCnpj;

    @Value("${nf-e.emit.uf-code:35}")
    private String ufCode;

    @Value("${nf-e.emit.serie:1}")
    private String serie;

    @Value("${nf-e.ambiente:2}")
    private String tpAmb;

    @Value("${nf-e.emit.razao-social:Emitente Homologacao}")
    private String emitRazaoSocial;

    @Value("${nf-e.emit.ie:123456789}")
    private String emitIe;

    @Value("${nf-e.emit.crt:3}")
    private String emitCrt;

    @Value("${nf-e.emit.endereco.logradouro:Rua Teste}")
    private String emitLogradouro;

    @Value("${nf-e.emit.endereco.numero:0}")
    private String emitNumero;

    @Value("${nf-e.emit.endereco.bairro:Centro}")
    private String emitBairro;

    @Value("${nf-e.emit.endereco.municipio:3550308}")
    private String emitMunicipioCodigo;

    @Value("${nf-e.emit.endereco.municipio-nome:Sao Paulo}")
    private String emitMunicipioNome;

    @Value("${nf-e.emit.endereco.uf:SP}")
    private String emitUf;

    @Value("${nf-e.emit.endereco.cep:00000000}")
    private String emitCep;

    @Value("${nf-e.emit.endereco.pais:1058}")
    private String emitPaisCodigo;

    @Value("${nf-e.emit.endereco.pais-nome:BRASIL}")
    private String emitPaisNome;

    @Value("${nf-e.processo.versao:1.0}")
    private String verProc;

    /**
     * Monta o XML da NF-e.
     *
     * @param request dados da emissão (cliente, natureza, itens)
     * @param customer destinatário já carregado
     * @return String XML com elemento {@code infNFe} e atributo {@code Id}
     * @throws IllegalArgumentException se request for null ou sem itens
     */
    public String buildXml(NfeEmissionRequest request, Customer customer) {
        if (request == null) {
            throw new IllegalArgumentException("NfeEmissionRequest não pode ser null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A NF-e deve conter ao menos um item; itens recebidos: "
                    + (request.getItems() == null ? "null" : "0"));
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer não pode ser null");
        }

        String nNF = gerarNNF();
        String cNF = gerarCNF();
        String chaveParcial = montarChaveParcial(nNF, cNF);
        String cDV = calcularDigitoVerificador(chaveParcial);
        String accessKey = chaveParcial + cDV;
        OffsetDateTime dhEmi = OffsetDateTime.now();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<NFe xmlns=\"").append(NFE_NS).append("\">");
        sb.append("<infNFe versao=\"4.00\" Id=\"NFe").append(accessKey).append("\">");

        sb.append(buildIde(dhEmi, nNF, cNF, cDV, request));
        sb.append(buildEmit());
        sb.append(buildDest(customer));

        int itemNumber = 1;
        for (NfeItemRequest item : request.getItems()) {
            sb.append(buildDet(item, itemNumber++));
        }

        sb.append(buildTotal(request.getItems()));
        sb.append("<transp><modFrete>9</modFrete></transp>");
        sb.append(buildPag(request.getItems()));

        sb.append("</infNFe>");
        sb.append("</NFe>");
        return sb.toString();
    }

    private String buildIde(OffsetDateTime dhEmi, String nNF, String cNF, String cDV, NfeEmissionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ide>");
        sb.append("<cUF>").append(ufCode).append("</cUF>");
        sb.append("<cNF>").append(cNF).append("</cNF>");
        sb.append("<natOp>").append(esc(request.getNatureOperation())).append("</natOp>");
        sb.append("<mod>55</mod>");
        sb.append("<serie>").append(esc(serie)).append("</serie>");
        sb.append("<nNF>").append(nNF).append("</nNF>");
        sb.append("<dhEmi>").append(ISO_FMT.format(dhEmi)).append("</dhEmi>");
        sb.append("<tpNF>1</tpNF>");
        sb.append("<idDest>1</idDest>");
        sb.append("<cMunFG>").append(esc(emitMunicipioCodigo)).append("</cMunFG>");
        sb.append("<tpImp>1</tpImp>");
        sb.append("<tpEmis>1</tpEmis>");
        sb.append("<cDV>").append(cDV).append("</cDV>");
        sb.append("<tpAmb>").append(esc(tpAmb)).append("</tpAmb>");
        sb.append("<finNFe>1</finNFe>");
        sb.append("<indFinal>0</indFinal>");
        sb.append("<indPres>0</indPres>");
        sb.append("<procEmi>0</procEmi>");
        sb.append("<verProc>").append(esc(verProc)).append("</verProc>");
        sb.append("</ide>");
        return sb.toString();
    }

    private String buildEmit() {
        StringBuilder sb = new StringBuilder();
        sb.append("<emit>");
        sb.append("<CNPJ>").append(somenteDigitos(emitCnpj, 14)).append("</CNPJ>");
        sb.append("<xNome>").append(esc(emitRazaoSocial)).append("</xNome>");
        sb.append("<enderEmit>");
        sb.append("<xLgr>").append(esc(emitLogradouro)).append("</xLgr>");
        sb.append("<nro>").append(esc(emitNumero)).append("</nro>");
        sb.append("<xBairro>").append(esc(emitBairro)).append("</xBairro>");
        sb.append("<cMun>").append(esc(emitMunicipioCodigo)).append("</cMun>");
        sb.append("<xMun>").append(esc(emitMunicipioNome)).append("</xMun>");
        sb.append("<UF>").append(esc(emitUf)).append("</UF>");
        sb.append("<CEP>").append(somenteDigitos(emitCep, 8)).append("</CEP>");
        sb.append("<cPais>").append(esc(emitPaisCodigo)).append("</cPais>");
        sb.append("<xPais>").append(esc(emitPaisNome)).append("</xPais>");
        sb.append("</enderEmit>");
        sb.append("<IE>").append(esc(emitIe)).append("</IE>");
        sb.append("<CRT>").append(esc(emitCrt)).append("</CRT>");
        sb.append("</emit>");
        return sb.toString();
    }

    private String buildDest(Customer customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<dest>");
        String doc = somenteDigitos(customer.getDocument());
        if (doc.length() == 14) {
            sb.append("<CNPJ>").append(doc).append("</CNPJ>");
        } else if (doc.length() == 11) {
            sb.append("<CPF>").append(doc).append("</CPF>");
        } else {
            sb.append("<idEstrangeiro>").append(esc(customer.getDocument())).append("</idEstrangeiro>");
        }
        sb.append("<xNome>").append(esc(customer.getName())).append("</xNome>");
        sb.append("<enderDest>");
        sb.append("<xLgr>Endereco nao informado</xLgr>");
        sb.append("<nro>S/N</nro>");
        sb.append("<xBairro>Centro</xBairro>");
        sb.append("<cMun>").append(esc(emitMunicipioCodigo)).append("</cMun>");
        sb.append("<xMun>").append(esc(emitMunicipioNome)).append("</xMun>");
        sb.append("<UF>").append(esc(emitUf)).append("</UF>");
        sb.append("<CEP>00000000</CEP>");
        sb.append("<cPais>1058</cPais>");
        sb.append("<xPais>BRASIL</xPais>");
        sb.append("</enderDest>");
        sb.append("<indIEDest>9</indIEDest>");
        sb.append("</dest>");
        return sb.toString();
    }

    private String buildDet(NfeItemRequest item, int numero) {
        BigDecimal qCom = arredondar(item.getQuantity(), 4);
        BigDecimal vUnCom = arredondar(item.getUnitValue(), 10);
        BigDecimal vProd = arredondar(qCom.multiply(vUnCom), 2);

        StringBuilder sb = new StringBuilder();
        sb.append("<det nItem=\"").append(numero).append("\">");
        sb.append("<prod>");
        sb.append("<cProd>").append(esc(item.getProductCode())).append("</cProd>");
        sb.append("<cEAN>SEM GTIN</cEAN>");
        sb.append("<xProd>").append(esc(item.getDescription())).append("</xProd>");
        sb.append("<NCM>").append(esc(item.getNcm())).append("</NCM>");
        sb.append("<CFOP>").append(esc(item.getCfop())).append("</CFOP>");
        sb.append("<uCom>").append(esc(item.getUnit())).append("</uCom>");
        sb.append("<qCom>").append(formatarDecimal(qCom)).append("</qCom>");
        sb.append("<vUnCom>").append(formatarDecimal(vUnCom)).append("</vUnCom>");
        sb.append("<vProd>").append(formatarDecimal(vProd)).append("</vProd>");
        sb.append("<cEANTrib>SEM GTIN</cEANTrib>");
        sb.append("<uTrib>").append(esc(item.getUnit())).append("</uTrib>");
        sb.append("<qTrib>").append(formatarDecimal(qCom)).append("</qTrib>");
        sb.append("<vUnTrib>").append(formatarDecimal(vUnCom)).append("</vUnTrib>");
        sb.append("<indTot>1</indTot>");
        sb.append("</prod>");
        sb.append("<imposto>");
        sb.append("<vTotTrib>0.00</vTotTrib>");
        sb.append("<ICMS>");
        sb.append("<ICMS00>");
        sb.append("<orig>0</orig>");
        sb.append("<CST>00</CST>");
        sb.append("<modBC>3</modBC>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<pICMS>0.00</pICMS>");
        sb.append("<vICMS>0.00</vICMS>");
        sb.append("</ICMS00>");
        sb.append("</ICMS>");
        sb.append("<PIS><PISNT><CST>07</CST></PISNT></PIS>");
        sb.append("<COFINS><COFINSNT><CST>07</CST></COFINSNT></COFINS>");
        sb.append("</imposto>");
        sb.append("</det>");
        return sb.toString();
    }

    private String buildTotal(List<NfeItemRequest> items) {
        BigDecimal vProd = totalProdutos(items);
        BigDecimal vNF = vProd;

        StringBuilder sb = new StringBuilder();
        sb.append("<total>");
        sb.append("<ICMSTot>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<vICMS>0.00</vICMS>");
        sb.append("<vICMSDeson>0.00</vICMSDeson>");
        sb.append("<vFCP>0.00</vFCP>");
        sb.append("<vBCST>0.00</vBCST>");
        sb.append("<vST>0.00</vST>");
        sb.append("<vFCPST>0.00</vFCPST>");
        sb.append("<vFCPSTRet>0.00</vFCPSTRet>");
        sb.append("<vProd>").append(formatarDecimal(vProd)).append("</vProd>");
        sb.append("<vFrete>0.00</vFrete>");
        sb.append("<vSeg>0.00</vSeg>");
        sb.append("<vDesc>0.00</vDesc>");
        sb.append("<vII>0.00</vII>");
        sb.append("<vIPI>0.00</vIPI>");
        sb.append("<vIPIDevol>0.00</vIPIDevol>");
        sb.append("<vPIS>0.00</vPIS>");
        sb.append("<vCOFINS>0.00</vCOFINS>");
        sb.append("<vOutro>0.00</vOutro>");
        sb.append("<vNF>").append(formatarDecimal(vNF)).append("</vNF>");
        sb.append("<vTotTrib>0.00</vTotTrib>");
        sb.append("</ICMSTot>");
        sb.append("</total>");
        return sb.toString();
    }

    private String buildPag(List<NfeItemRequest> items) {
        BigDecimal vPag = totalProdutos(items);
        StringBuilder sb = new StringBuilder();
        sb.append("<pag>");
        sb.append("<detPag>");
        sb.append("<tPag>90</tPag>");
        sb.append("<vPag>").append(formatarDecimal(vPag)).append("</vPag>");
        sb.append("</detPag>");
        sb.append("</pag>");
        return sb.toString();
    }

    private String montarChaveParcial(String nNF, String cNF) {
        String aamm = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyMM"));
        return ufCode + aamm
                + somenteDigitos(emitCnpj, 14)
                + "55"
                + String.format("%03d", Integer.parseInt(serie))
                + String.format("%09d", Long.parseLong(nNF))
                + "1"
                + cNF;
    }

    private String calcularDigitoVerificador(String chave43) {
        int peso = 2;
        int soma = 0;
        for (int i = chave43.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(chave43.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        int dv = (resto == 0 || resto == 1) ? 0 : 11 - resto;
        return String.valueOf(dv);
    }

    private String gerarNNF() {
        long value = Math.abs(System.nanoTime()) % 900_000_000L + 100_000_000L;
        return String.valueOf(value);
    }

    private String gerarCNF() {
        long value = Math.abs(System.nanoTime() / 7) % 100_000_000L;
        return String.format("%08d", value);
    }

    private BigDecimal totalProdutos(List<NfeItemRequest> items) {
        return items.stream()
                .map(i -> i.getUnitValue().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal arredondar(BigDecimal valor, int casas) {
        return valor.setScale(casas, RoundingMode.HALF_UP);
    }

    private String formatarDecimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String somenteDigitos(String valor) {
        if (valor == null) return "";
        return valor.replaceAll("\\D", "");
    }

    private String somenteDigitos(String valor, int tamanho) {
        String limpo = somenteDigitos(valor);
        if (limpo.length() > tamanho) {
            return limpo.substring(0, tamanho);
        }
        return "0".repeat(Math.max(0, tamanho - limpo.length())) + limpo;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

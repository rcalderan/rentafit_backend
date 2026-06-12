package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeItemRequest;
import br.com.rentafit.people.domain.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Constrói o XML {@code infNFe} (modelo 55) conforme o MOC da NF-e.
 *
 * <p>O elemento {@code infNFe} recebe atributo {@code Id="NFe"+chave44}.
 * Exemplo: {@code builder.buildXml(request, customer)} → String XML pronta para assinar.</p>
 */
@Component
public class NfeXmlBuilder {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String NFE_NS = "http://www.portalfiscal.inf.br/nfe";

    @Value("${nf-e.emit.cnpj:00000000000191}")
    private String emitCnpj = "00000000000191";

    @Value("${nf-e.emit.uf-code:35}")
    private String ufCode = "35";

    @Value("${nf-e.emit.serie:1}")
    private String serie = "1";

    @Value("${nf-e.ambiente:2}")
    private String tpAmb = "2";

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

        String accessKey = buildAccessKey();
        BigDecimal total = totalProdutos(request.getItems());

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<NFe xmlns=\"").append(NFE_NS).append("\">");
        sb.append("<infNFe versao=\"4.00\" Id=\"NFe").append(accessKey).append("\">");

        sb.append("<ide>");
        sb.append("<cUF>").append(ufCode).append("</cUF>");
        sb.append("<natOp>").append(esc(request.getNatureOperation())).append("</natOp>");
        sb.append("<mod>55</mod>");
        sb.append("<serie>").append(esc(serie)).append("</serie>");
        sb.append("<dhEmi>").append(ISO_FMT.format(OffsetDateTime.now())).append("</dhEmi>");
        sb.append("<tpAmb>").append(esc(tpAmb)).append("</tpAmb>");
        sb.append("</ide>");

        sb.append("<emit><CNPJ>").append(esc(emitCnpj)).append("</CNPJ></emit>");

        sb.append("<dest>");
        sb.append(documentoTag(customer.getDocument()));
        sb.append("<xNome>").append(esc(customer.getName())).append("</xNome>");
        sb.append("</dest>");

        int item = 1;
        for (NfeItemRequest i : request.getItems()) {
            sb.append(buildItem(i, item++));
        }

        sb.append("<total><ICMSTot><vNF>").append(total).append("</vNF></ICMSTot></total>");

        sb.append("</infNFe>");
        sb.append("</NFe>");
        return sb.toString();
    }

    private String buildItem(NfeItemRequest i, int numero) {
        BigDecimal vProd = i.getUnitValue().multiply(i.getQuantity());
        return "<det nItem=\"" + numero + "\"><prod>"
                + "<cProd>" + esc(i.getProductCode()) + "</cProd>"
                + "<xProd>" + esc(i.getDescription()) + "</xProd>"
                + "<NCM>" + esc(i.getNcm()) + "</NCM>"
                + "<CFOP>" + esc(i.getCfop()) + "</CFOP>"
                + "<uCom>" + esc(i.getUnit()) + "</uCom>"
                + "<qCom>" + i.getQuantity() + "</qCom>"
                + "<vUnCom>" + i.getUnitValue() + "</vUnCom>"
                + "<vProd>" + vProd + "</vProd>"
                + "</prod></det>";
    }

    private String documentoTag(String doc) {
        if (doc == null) return "";
        String clean = doc.replaceAll("\\D", "");
        return clean.length() == 14
                ? "<CNPJ>" + clean + "</CNPJ>"
                : "<CPF>" + clean + "</CPF>";
    }

    private BigDecimal totalProdutos(List<NfeItemRequest> items) {
        return items.stream()
                .map(i -> i.getUnitValue().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildAccessKey() {
        String aamm = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String nNF = String.format("%09d", Math.abs(System.nanoTime()) % 1_000_000_000L);
        String cNF = String.format("%08d", Math.abs(System.nanoTime() / 7) % 100_000_000L);
        String partial = ufCode + aamm + pad(emitCnpj, 14) + "55"
                + String.format("%03d", Integer.parseInt(serie)) + nNF + "1" + cNF;
        return partial + digitoVerificador(partial);
    }

    private String digitoVerificador(String chave43) {
        int peso = 2, soma = 0;
        for (int i = chave43.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(chave43.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        int dv = (resto == 0 || resto == 1) ? 0 : 11 - resto;
        return String.valueOf(dv);
    }

    private String pad(String s, int len) {
        String clean = s.replaceAll("\\D", "");
        if (clean.length() >= len) return clean.substring(0, len);
        return "0".repeat(len - clean.length()) + clean;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

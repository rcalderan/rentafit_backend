package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Constrói o XML DPS (Declaração de Prestação de Serviços) conforme XSD oficial
 * NFS-e Nacional v1.01 (DPS_v1.01.xsd / tiposComplexos_v1.01.xsd).
 *
 * <p>Exemplo: {@code builder.buildXml(dpsRequest)} → String XML válida para assinar e enviar.</p>
 */
@Component
public class NfseDpsXmlBuilder {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Monta o XML DPS a partir do request, seguindo a ordem de elementos do XSD oficial.
     *
     * @param request dados da emissão
     * @return String XML do DPS
     * @throws IllegalArgumentException se request for null
     */
    public String buildXml(DpsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DpsRequest não pode ser null");
        }

        DpsRequest.InfDPS inf = request.getInfDPS();
        String versao = request.getVersao() != null ? request.getVersao() : "1.01";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"")
          .append(esc(versao)).append("\">");

        sb.append("<infDPS Id=\"").append(esc(inf.getId())).append("\">");

        appendInfDpsFields(sb, inf);
        appendPrestador(sb, inf.getPrest());
        appendTomador(sb, inf.getToma());
        appendServico(sb, inf.getServ());
        appendValores(sb, inf.getValores());

        sb.append("</infDPS>");
        sb.append("</DPS>");

        return sb.toString();
    }

    private void appendInfDpsFields(StringBuilder sb, DpsRequest.InfDPS inf) {
        sb.append("<tpAmb>").append(esc(inf.getTpAmb())).append("</tpAmb>");
        sb.append("<dhEmi>").append(ISO_FMT.format(inf.getDhEmi())).append("</dhEmi>");
        sb.append("<verAplic>").append(esc(inf.getVerAplic())).append("</verAplic>");
        sb.append("<serie>").append(esc(inf.getSerie())).append("</serie>");
        sb.append("<nDPS>").append(esc(inf.getNDPS())).append("</nDPS>");
        sb.append("<dCompet>").append(DATE_FMT.format(inf.getDCompet())).append("</dCompet>");
        sb.append("<tpEmit>").append(esc(inf.getTpEmit())).append("</tpEmit>");
        sb.append("<cLocEmi>").append(esc(inf.getCLocEmi())).append("</cLocEmi>");
    }

    private void appendPrestador(StringBuilder sb, DpsRequest.Prestador prest) {
        sb.append("<prest>");
        if (prest.getCNPJ() != null) {
            sb.append("<CNPJ>").append(esc(prest.getCNPJ())).append("</CNPJ>");
        } else {
            sb.append("<CPF>").append(esc(prest.getCPF())).append("</CPF>");
        }
        if (prest.getIM() != null) {
            sb.append("<IM>").append(esc(prest.getIM())).append("</IM>");
        }
        if (prest.getRegTrib() != null) {
            appendRegTrib(sb, prest.getRegTrib());
        }
        sb.append("</prest>");
    }

    private void appendRegTrib(StringBuilder sb, DpsRequest.RegTrib rt) {
        sb.append("<regTrib>");
        sb.append("<opSimpNac>").append(esc(rt.getOpSimpNac())).append("</opSimpNac>");
        sb.append("<regEspTrib>").append(esc(rt.getRegEspTrib())).append("</regEspTrib>");
        sb.append("</regTrib>");
    }

    private void appendTomador(StringBuilder sb, DpsRequest.Tomador toma) {
        if (toma == null) return;
        sb.append("<toma>");
        if (toma.getCNPJ() != null) {
            sb.append("<CNPJ>").append(esc(toma.getCNPJ())).append("</CNPJ>");
        } else {
            sb.append("<CPF>").append(esc(toma.getCPF())).append("</CPF>");
        }
        sb.append("<xNome>").append(esc(toma.getXNome())).append("</xNome>");
        if (toma.getEnd() != null) {
            appendEndereco(sb, toma.getEnd());
        }
        sb.append("</toma>");
    }

    private void appendEndereco(StringBuilder sb, DpsRequest.Endereco end) {
        sb.append("<end>");
        sb.append("<endNac>");
        sb.append("<cMun>").append(esc(end.getCMun())).append("</cMun>");
        sb.append("<CEP>").append(esc(end.getCEP())).append("</CEP>");
        sb.append("</endNac>");
        sb.append(tag("xLgr", end.getXLgr()));
        sb.append(tag("nro", end.getNro()));
        sb.append(tag("xBairro", end.getXBairro()));
        sb.append("</end>");
    }

    private void appendServico(StringBuilder sb, DpsRequest.Servico serv) {
        sb.append("<serv>");
        sb.append("<locPrest><cLocPrestacao>")
          .append(esc(serv.getLocPrest().getCLocPrestacao()))
          .append("</cLocPrestacao></locPrest>");
        sb.append("<cServ>");
        sb.append("<cTribNac>").append(esc(serv.getCServ().getCTribNac())).append("</cTribNac>");
        sb.append("<xDescServ>").append(esc(serv.getCServ().getXDescServ())).append("</xDescServ>");
        if (serv.getCServ().getCNBS() != null) {
            sb.append("<cNBS>").append(esc(serv.getCServ().getCNBS())).append("</cNBS>");
        }
        sb.append("</cServ>");
        sb.append("</serv>");
    }

    private void appendValores(StringBuilder sb, DpsRequest.Valores vals) {
        sb.append("<valores>");
        sb.append("<vServPrest><vServ>")
          .append(vals.getVServPrest().getVServ())
          .append("</vServ></vServPrest>");
        if (vals.getTrib() != null) {
            appendTrib(sb, vals.getTrib());
        }
        sb.append("</valores>");
    }

    private void appendTrib(StringBuilder sb, DpsRequest.Trib trib) {
        sb.append("<trib>");
        if (trib.getTribMun() != null) {
            sb.append("<tribMun>");
            sb.append("<tribISSQN>").append(esc(trib.getTribMun().getTribISSQN())).append("</tribISSQN>");
            sb.append("<tpRetISSQN>").append(esc(trib.getTribMun().getTpRetISSQN())).append("</tpRetISSQN>");
            sb.append("</tribMun>");
        }
        if (trib.getTotTrib() != null) {
            sb.append("<totTrib><indTotTrib>")
              .append(esc(trib.getTotTrib().getIndTotTrib()))
              .append("</indTotTrib></totTrib>");
        }
        sb.append("</trib>");
    }

    private String tag(String name, String value) {
        return value != null ? "<" + name + ">" + esc(value) + "</" + name + ">" : "";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

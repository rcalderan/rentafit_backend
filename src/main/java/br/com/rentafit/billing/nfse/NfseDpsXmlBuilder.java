package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Constrói o XML DPS (Documento Preliminar de Serviço) conforme NT 004 da NFS-e Nacional.
 *
 * <p>Exemplo: {@code builder.buildXml(dpsRequest)} → String XML válida para assinar e enviar.</p>
 */
@Component
public class NfseDpsXmlBuilder {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Monta o XML DPS a partir do request.
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
        DpsRequest.Prestador prest = inf.getPrest();
        DpsRequest.Tomador toma = inf.getToma();
        DpsRequest.Servico serv = inf.getServ();
        DpsRequest.Valores vals = inf.getVals();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\">");
        sb.append("<infDPS>");
        sb.append("<dhEmi>").append(ISO_FMT.format(inf.getDhEmi())).append("</dhEmi>");
        sb.append("<pEmi>").append(esc(inf.getPEmi())).append("</pEmi>");
        sb.append("<tpAmb>").append(esc(inf.getTpAmb())).append("</tpAmb>");
        sb.append("<verAtu>").append(esc(inf.getVerAtu())).append("</verAtu>");

        sb.append("<prest>");
        sb.append("<CNPJ>").append(esc(prest.getCNPJ())).append("</CNPJ>");
        if (prest.getIM() != null) {
            sb.append("<IM>").append(esc(prest.getIM())).append("</IM>");
        }
        sb.append("</prest>");

        sb.append("<toma>");
        sb.append("<identif>");
        if (toma.getIdentif().getCNPJ() != null) {
            sb.append("<CNPJ>").append(esc(toma.getIdentif().getCNPJ())).append("</CNPJ>");
        } else {
            sb.append("<CPF>").append(esc(toma.getIdentif().getCPF())).append("</CPF>");
        }
        sb.append("</identif>");
        sb.append("<nNome>").append(esc(toma.getNNome())).append("</nNome>");
        if (toma.getEnd() != null) {
            sb.append(buildEndereco(toma.getEnd()));
        }
        sb.append("</toma>");

        sb.append("<serv>");
        sb.append("<locServ><cMunServ>").append(esc(serv.getLocServ().getCMunServ())).append("</cMunServ></locServ>");
        sb.append("<idServ>");
        sb.append("<cNBS>").append(esc(serv.getIdServ().getCNBS())).append("</cNBS>");
        sb.append("<desc>").append(esc(serv.getIdServ().getDesc())).append("</desc>");
        sb.append("</idServ>");
        sb.append("</serv>");

        sb.append("<vals>");
        sb.append("<vServ>").append(vals.getVServ()).append("</vServ>");
        if (vals.getTribut() != null) {
            sb.append(buildTributos(vals.getTribut()));
        }
        sb.append("</vals>");

        sb.append("</infDPS>");
        sb.append("</DPS>");

        return sb.toString();
    }

    private String buildEndereco(DpsRequest.Endereco end) {
        return "<end>"
                + tag("lograd", end.getLograd())
                + tag("nNum", end.getNNum())
                + tag("cMun", end.getCMun())
                + tag("UF", end.getUF())
                + tag("CEP", end.getCEP())
                + "</end>";
    }

    private String buildTributos(DpsRequest.Tributos t) {
        StringBuilder sb = new StringBuilder("<tribut>");
        if (t.getIbs() != null) {
            sb.append("<ibs>")
              .append("<pAliq>").append(t.getIbs().getPAliq()).append("</pAliq>")
              .append("<vIBS>").append(t.getIbs().getVIBS()).append("</vIBS>")
              .append("</ibs>");
        }
        if (t.getCbs() != null) {
            sb.append("<cbs>")
              .append("<pAliq>").append(t.getCbs().getPAliq()).append("</pAliq>")
              .append("<vCBS>").append(t.getCbs().getVCBS()).append("</vCBS>")
              .append("</cbs>");
        }
        sb.append("</tribut>");
        return sb.toString();
    }

    private String tag(String name, String value) {
        return value != null ? "<" + name + ">" + esc(value) + "</" + name + ">" : "";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

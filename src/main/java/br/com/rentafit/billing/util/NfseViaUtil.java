package br.com.rentafit.billing.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class NfseViaUtil {

    /**
     * Comprime uma string (XML) usando GZIP e retorna em Base64.
     */
    public static String compressAndEncode(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        try (ByteArrayOutputStream obj = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(obj)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return Base64.getEncoder().encodeToString(obj.toByteArray());
        } catch (IOException e) {
            log.error("Erro ao comprimir conteúdo para GZIP+Base64: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Decodifica Base64 e descompacta GZIP para retornar a String original (XML).
     */
    public static String decodeAndDecompress(String base64Gzip) {
        if (base64Gzip == null || base64Gzip.isEmpty()) {
            return "";
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(base64Gzip);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
                 GZIPInputStream gis = new GZIPInputStream(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
                return bos.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Erro ao descompactar conteúdo GZIP+Base64: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Assina digitalmente um XML utilizando o padrão XMLDSig (Enveloped).
     *
     * @param xml         Conteúdo XML a ser assinado.
     * @param targetId    O atributo 'Id' do elemento a ser assinado (ex: o valor de infNFSe).
     * @param privateKey  Chave privada para assinatura.
     * @param certificate Certificado X.509 para inclusão na assinatura.
     * @return XML assinado ou nulo em caso de erro.
     */
    public static String signXml(String xml, String targetId, PrivateKey privateKey, X509Certificate certificate) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // Align with spec v1.01 example: SHA1 + Inclusive Canonicalization
            List<Transform> transforms = new ArrayList<>();
            transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
            transforms.add(fac.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null));

            Reference ref = fac.newReference(
                    "#" + targetId,
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    transforms,
                    null,
                    null
            );

            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(ref)
            );

            KeyInfoFactory kif = fac.getKeyInfoFactory();
            List<Object> x509Content = new ArrayList<>();
            x509Content.add(certificate);
            X509Data xd = kif.newX509Data(x509Content);
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

            DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());

            NodeList nodes = doc.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
                if (element.hasAttribute("Id") && element.getAttribute("Id").equals(targetId)) {
                    element.setIdAttribute("Id", true);
                }
            }

            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(new DOMSource(doc), new StreamResult(os));

            return os.toString(StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Erro ao assinar XML: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gera um XML de exemplo para NFS-e Via (Padrão Nacional) sem assinatura.
     */
    public static String generateSampleXml(String cnpj, String identificador) {
        String accessKey = "NFS35503082000000000001910000000000001260101234567891";
//DPS35503082000000000001910000100000000000001
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<NFSe xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.01\">\n" +
                "  <infNFSe Id=\"" + accessKey + "\">\n" +
                "    <xLocEmi>SAO PAULO</xLocEmi>\n" +
                "    <xLocPrestacao>SAO PAULO</xLocPrestacao>\n" +
                "    <nNFSe>1</nNFSe>\n" +
                "    <xTribNac>Análise e desenvolvimento de sistemas.</xTribNac>\n" +
                "    <verAplic>Rentafit v1.0</verAplic>\n" +
                "    <ambGer>2</ambGer>\n" +
                "    <tpEmis>1</tpEmis>\n" +
                "    <cStat>100</cStat>\n" +
                "    <dhProc>2026-01-25T14:30:00-03:00</dhProc>\n" +
                "    <nDFSe>1</nDFSe>\n" +
                "    <emit>\n" +
                "      <CNPJ>" + cnpj + "</CNPJ>\n" +
                "      <xNome>EMPRESA DE TESTE LTDA</xNome>\n" +
                "      <enderNac>\n" +
                "        <cMun>3550308</cMun>\n" +
                "        <UF>SP</UF>\n" +
                "        <xLgr>AVENIDA PAULISTA</xLgr>\n" +
                "        <nro>1000</nro>\n" +
                "        <xBairro>BELA VISTA</xBairro>\n" +
                "        <CEP>01310100</CEP>\n" +
                "      </enderNac>\n" +
                "    </emit>\n" +
                "    <valores>\n" +
                "      <vLiq>100.00</vLiq>\n" +
                "    </valores>\n" +
                "    <DPS versao=\"1.01\">\n" +
                "      <infDPS Id=\""+identificador+"\">\n" +
                "        <tpAmb>2</tpAmb>\n" +
                "        <dhEmi>2026-01-25T14:00:00-03:00</dhEmi>\n" +
                "        <verAplic>Rentafit v1.0</verAplic>\n" +
                "        <serie>00001</serie>\n" +
                "        <nDPS>1</nDPS>\n" +
                "        <dCompet>2026-01-25</dCompet>\n" +
                "        <tpEmit>1</tpEmit>\n" +
                "        <cLocEmi>3550308</cLocEmi>\n" +
                "        <prest>\n" +
                "          <CNPJ>" + cnpj + "</CNPJ>\n" +
                "        </prest>\n" +
                "        <toma>\n" +
                "          <CNPJ>00000000000272</CNPJ>\n" +
                "          <xNome>CLIENTE DE TESTE S/A</xNome>\n" +
                "        </toma>\n" +
                "        <serv>\n" +
                "          <locPrest>\n" +
                "            <cLocPrestacao>3550308</cLocPrestacao>\n" +
                "          </locPrest>\n" +
                "          <cServ>\n" +
                "            <cTribNac>010101</cTribNac>\n" +
                "            <xDescServ>SERVICOS DE CONSULTORIA EM TI</xDescServ>\n" +
                "          </cServ>\n" +
                "        </serv>\n" +
                "        <valores>\n" +
                "          <vServPrest>\n" +
                "            <vServ>100.00</vServ>\n" +
                "          </vServPrest>\n" +
                "          <trib>\n" +
                "            <tribMun>\n" +
                "              <tribISSQN>1</tribISSQN>\n" +
                "              <tpRetISSQN>1</tpRetISSQN>\n" +
                "              <pAliq>5.00</pAliq>\n" +
                "            </tribMun>\n" +
                "          </trib>\n" +
                "        </valores>\n" +
                "      </infDPS>\n" +
                "    </DPS>\n" +
                "  </infNFSe>\n" +
                "</NFSe>";
    }

    /**
     * Gera um XML DPS conforme schema NFS-e Via v1.01 (Serpro).
     * IMPORTANTE: O Id vai DIRETO no elemento <DPS> como atributo, NÃO existe <infDPS>!
     * Esta era a causa do erro 209 - "Formato do identificador do metadados inválido"
     */
    public static String generateDpsXml(String idDps,
                                        String cnpjPrestador,
                                        String cnpjTomador,
                                        String serie,
                                        String numeroDps,
                                        String cLocEmi,
                                        String cLocPrestacao,
                                        String versaoAplic,
                                        String descricaoServ) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.01\" Id=\"" + idDps + "\">\n" +
                "  <tpAmb>2</tpAmb>\n" +
                "  <dhEmi>2026-01-25T14:00:00-03:00</dhEmi>\n" +
                "  <verAplic>" + versaoAplic + "</verAplic>\n" +
                "  <serie>" + serie + "</serie>\n" +
                "  <nDPS>" + numeroDps + "</nDPS>\n" +
                "  <dCompet>2026-01-25</dCompet>\n" +
                "  <tpEmit>1</tpEmit>\n" +
                "  <cLocEmi>" + cLocEmi + "</cLocEmi>\n" +
                "  <prest>\n" +
                "    <CNPJ>" + cnpjPrestador + "</CNPJ>\n" +
                "  </prest>\n" +
                "  <toma>\n" +
                "    <CNPJ>" + cnpjTomador + "</CNPJ>\n" +
                "    <xNome>CLIENTE DE TESTE</xNome>\n" +
                "  </toma>\n" +
                "  <serv>\n" +
                "    <locPrest>\n" +
                "      <cLocPrestacao>" + cLocPrestacao + "</cLocPrestacao>\n" +
                "    </locPrest>\n" +
                "    <cServ>\n" +
                "      <cTribNac>010101</cTribNac>\n" +
                "      <xDescServ>" + descricaoServ + "</xDescServ>\n" +
                "    </cServ>\n" +
                "  </serv>\n" +
                "  <valores>\n" +
                "    <vServPrest>\n" +
                "      <vServ>100.00</vServ>\n" +
                "    </vServPrest>\n" +
                "    <trib>\n" +
                "      <tribMun>\n" +
                "        <tribISSQN>1</tribISSQN>\n" +
                "        <tpRetISSQN>1</tpRetISSQN>\n" +
                "        <pAliq>5.00</pAliq>\n" +
                "      </tribMun>\n" +
                "    </trib>\n" +
                "  </valores>\n" +
                "</DPS>";
    }
}

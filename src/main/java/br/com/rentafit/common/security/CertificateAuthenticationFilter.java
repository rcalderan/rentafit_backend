package br.com.rentafit.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro para autenticação via certificado digital X.509 (mTLS)
 * Extrai o CNPJ/CPF do Subject do certificado e cria autenticação no contexto
 */
@Component
@Slf4j
public class CertificateAuthenticationFilter extends OncePerRequestFilter {

    private static final String X509_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\b(\\d{14})\\b");
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{11})\\b");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extrai certificado do cliente (mTLS)
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(X509_ATTRIBUTE);

        if (certs != null && certs.length > 0) {
            X509Certificate clientCert = certs[0];

            try {
                // Valida o certificado
                clientCert.checkValidity();

                String certSubject = clientCert.getSubjectDN().getName();
                log.info("Certificado recebido - Subject: {}", certSubject);

                // Extrai CNPJ/CPF do certificado
                String documentNumber = extractDocumentFromCertificate(certSubject);

                if (documentNumber != null) {
                    // Cria autenticação com o certificado
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_CERTIFICATE_AUTH"));

                    CertificateAuthentication auth = new CertificateAuthentication(
                        documentNumber,
                        clientCert,
                        authorities
                    );
                    auth.setAuthenticated(true);

                    // Adiciona ao contexto (não sobrescreve JWT se existir)
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("Autenticação por certificado estabelecida para documento: {}",
                            maskDocument(documentNumber));
                } else {
                    log.warn("Não foi possível extrair CNPJ/CPF do certificado: {}", certSubject);
                }

            } catch (CertificateExpiredException e) {
                log.error("Certificado expirado: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Certificado digital expirado");
                return;
            } catch (CertificateNotYetValidException e) {
                log.error("Certificado ainda não válido: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Certificado digital ainda não é válido");
                return;
            } catch (Exception e) {
                log.warn("Erro ao processar certificado: {}", e.getMessage());
            }
        } else {
            log.debug("Nenhum certificado de cliente encontrado na requisição");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai CNPJ ou CPF do Subject DN do certificado
     * Formato esperado: CN=12345678000191 ou serialNumber=12345678000191
     */
    private String extractDocumentFromCertificate(String subjectDN) {
        // Primeiro tenta extrair CNPJ (14 dígitos)
        Matcher cnpjMatcher = CNPJ_PATTERN.matcher(subjectDN);
        if (cnpjMatcher.find()) {
            return cnpjMatcher.group(1);
        }

        // Se não encontrar CNPJ, tenta CPF (11 dígitos)
        Matcher cpfMatcher = CPF_PATTERN.matcher(subjectDN);
        if (cpfMatcher.find()) {
            return cpfMatcher.group(1);
        }

        // Fallback: tenta extrair do CN ou serialNumber
        String[] parts = subjectDN.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=") || trimmed.startsWith("serialNumber=")) {
                String value = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                // Remove caracteres não numéricos
                String digits = value.replaceAll("\\D", "");
                if (digits.length() == 14 || digits.length() == 11) {
                    return digits;
                }
            }
        }

        return null;
    }

    /**
     * Mascara o documento para log (exibe apenas primeiros e últimos dígitos)
     */
    private String maskDocument(String document) {
        if (document == null || document.length() < 6) {
            return "***";
        }
        return document.substring(0, 3) + "***" + document.substring(document.length() - 2);
    }
}

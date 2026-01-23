package br.com.rentafit.common.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Autenticação baseada em certificado digital X.509 (A1/A3 ICP-Brasil)
 */
@Getter
public class CertificateAuthentication implements Authentication {

    private final String cnpj;
    private final X509Certificate certificate;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;

    public CertificateAuthentication(String cnpj,
                                     X509Certificate certificate,
                                     Collection<? extends GrantedAuthority> authorities) {
        this.cnpj = cnpj;
        this.certificate = certificate;
        this.authorities = authorities;
        this.authenticated = false;
    }

    @Override
    public String getName() {
        return cnpj;
    }

    @Override
    public Object getPrincipal() {
        return cnpj;
    }

    @Override
    public Object getCredentials() {
        return certificate;
    }

    @Override
    public Object getDetails() {
        return certificate.getSubjectDN().getName();
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }
}

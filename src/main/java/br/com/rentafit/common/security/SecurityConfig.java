package br.com.rentafit.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final CertificateAuthenticationFilter certificateAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/public-key").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/addresses/find/**").permitAll()

                        // Área self-service do cliente autenticado (qualquer role autenticada)
                        .requestMatchers("/api/v1/account/**").authenticated()

                        // Employees: somente ADMIN (hierarquia não é auto-aplicada aqui — listar explicitamente)
                        .requestMatchers("/api/v1/employees/**").hasRole("ADMIN")

                        // Customers: EMPLOYEE, MANAGER, ADMIN gerenciam; CUSTOMER não acessa dados de outros
                        .requestMatchers(HttpMethod.GET,    "/api/v1/customers/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/customers/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasAnyRole("MANAGER", "ADMIN")

                        // Produtos: leitura EMPLOYEE/MANAGER/ADMIN, escrita MANAGER/ADMIN, delete ADMIN
                        .requestMatchers(HttpMethod.GET,    "/api/v1/products/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // Locação e pagamentos: EMPLOYEE, MANAGER, ADMIN
                        .requestMatchers("/api/v1/rental/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                        // Vendas: EMPLOYEE, MANAGER, ADMIN
                        .requestMatchers("/api/v1/sales/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                        // Faturamento/NF-e e NFS-e — autorizacao centralizada aqui
                        .requestMatchers("/api/nfe/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers("/api/nfse/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers("/api/billing/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/billing/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                        .requestMatchers("/api/fiscal-documents/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                        // Admin de usuários (já tem @PreAuthorize, defense-in-depth)
                        .requestMatchers("/api/auth/users/**").hasAnyRole("MANAGER", "ADMIN")

                        // Demais endpoints auth (login, me, refresh, setup, change-pw)
                        .requestMatchers("/api/auth/**").authenticated()

                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Ordem: Certificado -> JWT
                // CertificateFilter adiciona ROLE_CERTIFICATE_AUTH se mTLS presente
                // SecurityFilter (JWT) adiciona roles do usuário (ADMIN/EMPLOYEE)
                .addFilterBefore(certificateAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy(){
        String hierarchy = "ROLE_ADMIN > ROLE_MANAGER > ROLE_EMPLOYEE > ROLE_CUSTOMER";
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }
}

package br.com.rentafit.auth;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
public class FullAuthenticationDiagnosticTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void fullDiagnostic() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DIAGNÓSTICO COMPLETO DE AUTENTICAÇÃO");
        System.out.println("=".repeat(60));

        // 1. Verificar se o usuário existe
        System.out.println("\n1. Buscando usuário 'admin'...");
        var adminOpt = userAccountRepository.findByUsername("admin");

        if (adminOpt.isEmpty()) {
            System.out.println("❌ ERRO FATAL: Usuário 'admin' NÃO EXISTE no banco!");
            System.out.println("Execute o reset do banco de dados:");
            System.out.println("docker exec dev-postgresql psql -U postgres -d rentafit -c \"DROP TABLE IF EXISTS flyway_schema_history CASCADE;\"");
            assertThat(adminOpt).isPresent();
            return;
        }

        UserAccount admin = adminOpt.get();
        System.out.println("✅ Usuário encontrado!");

        // 2. Verificar dados básicos
        System.out.println("\n2. Dados do usuário:");
        System.out.println("   ID: " + admin.getId());
        System.out.println("   Username: " + admin.getUsername());
        System.out.println("   IsActive: " + admin.getIsActive());
        System.out.println("   Password hash: " + admin.getPassword());

        // 3. Verificar roles
        System.out.println("\n3. Verificando roles:");
        System.out.println("   Roles list: " + admin.getRoles());
        System.out.println("   Roles size: " + (admin.getRoles() == null ? "null" : admin.getRoles().size()));

        if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
            System.out.println("   ❌ PROBLEMA: Lista de roles está VAZIA!");
            System.out.println("   Verifique se a migration V2 foi executada corretamente.");
        } else {
            System.out.println("   ✅ Roles encontradas:");
            admin.getRoles().forEach(r ->
                System.out.println("      - " + r.getRole() + " (Authority: " + r.getAuthority() + ")")
            );
        }

        // 4. Verificar authorities
        System.out.println("\n4. Verificando authorities:");
        var authorities = admin.getAuthorities();
        System.out.println("   Authorities: " + authorities);
        System.out.println("   Count: " + authorities.size());

        if (authorities.isEmpty()) {
            System.out.println("   ❌ PROBLEMA: Nenhuma authority encontrada!");
        }

        // 5. Testar senha
        System.out.println("\n5. Testando validação de senha:");
        String testPassword = "admin123";
        String dbHash = admin.getPassword();

        System.out.println("   Senha teste: " + testPassword);
        System.out.println("   Hash do banco: " + dbHash);

        boolean matches = passwordEncoder.matches(testPassword, dbHash);
        System.out.println("   Match result: " + matches);

        if (!matches) {
            System.out.println("   ❌ PROBLEMA: Senha não corresponde ao hash!");
            System.out.println("   Hash esperado para 'admin123':");
            String correctHash = passwordEncoder.encode(testPassword);
            System.out.println("   " + correctHash);
            System.out.println("\n   SOLUÇÃO: Atualize diretamente no banco:");
            System.out.println("   docker exec dev-postgresql psql -U postgres -d rentafit -c \"UPDATE user_accounts SET password = '" + correctHash + "' WHERE username = 'admin';\"");
        } else {
            System.out.println("   ✅ Senha válida!");
        }

        // 6. Verificar UserDetails
        System.out.println("\n6. Verificando UserDetails:");
        System.out.println("   isAccountNonExpired: " + admin.isAccountNonExpired());
        System.out.println("   isAccountNonLocked: " + admin.isAccountNonLocked());
        System.out.println("   isCredentialsNonExpired: " + admin.isCredentialsNonExpired());
        System.out.println("   isEnabled: " + admin.isEnabled());

        // 7. Resumo
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RESUMO DO DIAGNÓSTICO");
        System.out.println("=".repeat(60));

        boolean allGood = true;

        if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
            System.out.println("❌ Roles vazias - Execute migration V2");
            allGood = false;
        }

        if (!matches) {
            System.out.println("❌ Senha inválida - Atualize o hash no banco");
            allGood = false;
        }

        if (!admin.isEnabled()) {
            System.out.println("❌ Conta desabilitada - isActive = false");
            allGood = false;
        }

        if (allGood) {
            System.out.println("✅ TUDO OK! O login deveria funcionar.");
            System.out.println("\nSe ainda assim o login falhar, verifique:");
            System.out.println("1. Se está enviando username 'admin' (minúsculo)");
            System.out.println("2. Se está enviando password 'admin123'");
            System.out.println("3. Se há algum filter de segurança bloqueando");
        }

        System.out.println("=".repeat(60) + "\n");

        assertThat(admin.getRoles()).isNotEmpty();
        assertThat(matches).isTrue();
    }
}


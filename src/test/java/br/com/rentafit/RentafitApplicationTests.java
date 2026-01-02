package br.com.rentafit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de inicialização da aplicação.
 * Usa H2 em memória (rápido, sem Docker).
 * Verifica se o contexto Spring carrega corretamente.
 */
@SpringBootTest
@ActiveProfiles("test") // Carrega application-test.properties
class RentafitApplicationTests {

	@Test
	void contextLoads() {
		// Se chegar aqui, o contexto Spring carregou com sucesso
		assertThat(true).isTrue();
	}

	@Test
	void applicationStartsWithH2Database() {
		// Smoke test - verifica inicialização com H2
		// Não precisa Docker PostgreSQL para testes!
	}
}

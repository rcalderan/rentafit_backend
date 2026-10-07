package br.com.rentafit.common.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TsQueryBuilder - Unit Tests")
class TsQueryBuilderTest {

    @Test
    @DisplayName("PREFIX_LAST: só o último token vira prefixo")
    void shouldPrefixOnlyLastToken() {
        assertThat(TsQueryBuilder.toTsQuery("maria silva", SearchMode.PREFIX_LAST))
                .isEqualTo("maria & silva:*");
    }

    @Test
    @DisplayName("PREFIX_ALL: todos os tokens viram prefixo")
    void shouldPrefixAllTokens() {
        assertThat(TsQueryBuilder.toTsQuery("maria silva", SearchMode.PREFIX_ALL))
                .isEqualTo("maria:* & silva:*");
    }

    @Test
    @DisplayName("mode null cai no default PREFIX_LAST")
    void shouldDefaultToPrefixLastWhenModeIsNull() {
        assertThat(TsQueryBuilder.toTsQuery("maria silva", null))
                .isEqualTo("maria & silva:*");
    }

    @Test
    @DisplayName("Token único vira prefixo em ambos os modos")
    void shouldPrefixSingleToken() {
        assertThat(TsQueryBuilder.toTsQuery("test", SearchMode.PREFIX_LAST)).isEqualTo("test:*");
        assertThat(TsQueryBuilder.toTsQuery("test", SearchMode.PREFIX_ALL)).isEqualTo("test:*");
    }

    @Test
    @DisplayName("Operadores tsquery e pontuação são removidos")
    void shouldStripTsQueryOperators() {
        assertThat(TsQueryBuilder.toTsQuery("maria & silva | (joão)!", SearchMode.PREFIX_LAST))
                .isEqualTo("maria & silva & joão:*");
    }

    @Test
    @DisplayName("Entrada nula, vazia ou sem token retorna null")
    void shouldReturnNullWhenNoToken() {
        assertThat(TsQueryBuilder.toTsQuery(null, SearchMode.PREFIX_LAST)).isNull();
        assertThat(TsQueryBuilder.toTsQuery("   ", SearchMode.PREFIX_LAST)).isNull();
        assertThat(TsQueryBuilder.toTsQuery("&!|()", SearchMode.PREFIX_LAST)).isNull();
    }

    @Test
    @DisplayName("toExactQuery: tokens crus ORed, sem prefixo")
    void shouldBuildExactQuery() {
        assertThat(TsQueryBuilder.toExactQuery("maria silva")).isEqualTo("maria | silva");
        assertThat(TsQueryBuilder.toExactQuery("claudio")).isEqualTo("claudio");
    }

    @Test
    @DisplayName("toExactQuery: sanitiza operadores e retorna null sem token")
    void shouldSanitizeExactQuery() {
        assertThat(TsQueryBuilder.toExactQuery("maria & silva:(a)")).isEqualTo("maria | silva | a");
        assertThat(TsQueryBuilder.toExactQuery(null)).isNull();
        assertThat(TsQueryBuilder.toExactQuery(" &!|() ")).isNull();
    }

    @Test
    @DisplayName("idsOrNeverMatch retorna o próprio list quando não vazio")
    void shouldReturnIdsWhenNotEmpty() {
        UUID id = UUID.randomUUID();
        assertThat(TsQueryBuilder.idsOrNeverMatch(List.of(id))).containsExactly(id);
    }

    @Test
    @DisplayName("idsOrNeverMatch retorna UUID sentinela quando vazio")
    void shouldReturnSentinelWhenEmpty() {
        assertThat(TsQueryBuilder.idsOrNeverMatch(List.of()))
                .containsExactly(new UUID(0, 0));
    }
}

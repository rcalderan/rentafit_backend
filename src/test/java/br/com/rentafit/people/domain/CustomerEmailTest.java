package br.com.rentafit.people.domain;

import br.com.rentafit.people.dto.CustomerDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Customer - e-mail opcional")
class CustomerEmailTest {

    private static CustomerDTO dtoWithEmail(String email) {
        return CustomerDTO.builder().name("Ana Souza").document("52998224725").email(email)
                .phones(List.of("16999999999")).isAuthenticated(false).build();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("cadastro no balcão sem e-mail grava null (evita colisão no UNIQUE de people.email)")
    void blankEmailBecomesNull(String email) {
        assertThat(new Customer(dtoWithEmail(email)).getEmail()).isNull();
        assertThat(new Customer().updateBasicFieldsFromDTO(dtoWithEmail(email)).getEmail()).isNull();
    }

    @Test
    @DisplayName("e-mail informado é normalizado")
    void emailIsNormalized() {
        assertThat(new Customer(dtoWithEmail("  Ana@Ex.COM ")).getEmail()).isEqualTo("ana@ex.com");
    }
}

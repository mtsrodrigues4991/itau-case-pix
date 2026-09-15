package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.exception.PixValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjValidatorTest {

    private final CnpjValidator validator = new CnpjValidator();

    @Test
    @DisplayName("suporta apenas o tipo CNPJ")
    void suportaApenasCnpj() {
        assertThat(validator.supports(KeyType.CNPJ)).isTrue();
        assertThat(validator.supports(KeyType.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("normaliza removendo pontuacao")
    void normaliza() {
        assertThat(validator.normalize(" 60.701.190/0001-04 ")).isEqualTo("60701190000104");
        assertThat(validator.normalize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "60701190000104",   // Itau Unibanco S.A.
            "11222333000181",
            "00000000000191"    // Banco do Brasil
    })
    @DisplayName("aceita CNPJs com digitos verificadores validos")
    void aceitaValidos(String valor) {
        assertThatCode(() -> validator.validate(valor)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("aceita CNPJ informado com pontuacao apos a normalizacao")
    void aceitaComPontuacao() {
        assertThatCode(() -> validator.validate(validator.normalize("60.701.190/0001-04")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"60701190000105", "11222333000180", "12345678901234"})
    @DisplayName("rejeita CNPJ com digito verificador invalido")
    void rejeitaDvInvalido(String valor) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("digitos verificadores");
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "11111111111111", "99999999999999"})
    @DisplayName("rejeita CNPJ com todos os digitos iguais, mesmo passando no modulo 11")
    void rejeitaDigitosRepetidos(String valor) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("digitos verificadores");
    }

    @ParameterizedTest
    @ValueSource(strings = {"6070119000010", "607011900001045"})
    @DisplayName("rejeita CNPJ com tamanho diferente de 14")
    void rejeitaTamanhoInvalido(String valor) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("14 digitos");
    }

    @Test
    @DisplayName("rejeita CNPJ com letras")
    void rejeitaLetras() {
        assertThatThrownBy(() -> validator.validate("6070119000010A"))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("somente numeros");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejeita valor vazio")
    void rejeitaVazio(String valor) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("rejeita valor null")
    void rejeitaNull() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(PixValidationException.class);
    }
}

package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.exception.PixValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CelularValidatorTest {

    private final CelularValidator validator = new CelularValidator();

    @Test
    @DisplayName("suporta apenas o tipo CELULAR")
    void suportaApenasCelular() {
        assertThat(validator.supports(KeyType.CELULAR)).isTrue();
        assertThat(validator.supports(KeyType.EMAIL)).isFalse();
        assertThat(validator.supports(KeyType.CNPJ)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "' +5511987654321 ', +5511987654321",
            "'+55 (11) 98765-4321', +5511987654321",
            "'+55.11.98765.4321', +5511987654321"
    })
    @DisplayName("normaliza removendo mascara de digitacao")
    void normalizaMascara(String bruto, String esperado) {
        assertThat(validator.normalize(bruto)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("normalize devolve null para entrada null")
    void normalizaNull() {
        assertThat(validator.normalize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+5511987654321",   // pais 2 + DDD 2 + numero 9
            "+155987654321",    // pais 1 + DDD 2 + numero 9
            "+55119987654321"   // pais 2 + DDD 3 + numero 9
    })
    @DisplayName("aceita celulares validos")
    void aceitaValidos(String valor) {
        assertThatCode(() -> validator.validate(valor)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "5511987654321, iniciar",
            "'+5511abc654321', somente numeros",
            "'+55119876543', codigo do pais",
            "'+551199876543219', codigo do pais"
    })
    @DisplayName("rejeita celulares invalidos com mensagem especifica")
    void rejeitaInvalidos(String valor, String trechoDaMensagem) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining(trechoDaMensagem);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "+"})
    @DisplayName("rejeita valor vazio ou apenas o sinal de mais")
    void rejeitaVazio(String valor) {
        assertThatThrownBy(() -> validator.validate(valor)).isInstanceOf(PixValidationException.class);
    }

    @Test
    @DisplayName("rejeita valor null")
    void rejeitaNull() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("obrigatorio");
    }
}

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

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    @DisplayName("suporta apenas o tipo EMAIL")
    void suportaApenasEmail() {
        assertThat(validator.supports(KeyType.EMAIL)).isTrue();
        assertThat(validator.supports(KeyType.CELULAR)).isFalse();
    }

    @Test
    @DisplayName("normaliza com trim e caixa baixa")
    void normaliza() {
        assertThat(validator.normalize("  Maria.Silva@Itau.COM.br ")).isEqualTo("maria.silva@itau.com.br");
        assertThat(validator.normalize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "maria.silva@itau.com.br",
            "maria_silva99@itau.com",
            "maria+pix@itau.com.br"
    })
    @DisplayName("aceita e-mails validos")
    void aceitaValidos(String valor) {
        assertThatCode(() -> validator.validate(valor)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita e-mail sem arroba")
    void rejeitaSemArroba() {
        assertThatThrownBy(() -> validator.validate("maria.silva.itau.com.br"))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("'@'");
    }

    @Test
    @DisplayName("rejeita e-mail acima de 77 caracteres")
    void rejeitaAcimaDoLimite() {
        String local = "a".repeat(70);
        assertThatThrownBy(() -> validator.validate(local + "@itau.com.br"))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("77");
    }

    @Test
    @DisplayName("aceita e-mail com exatamente 77 caracteres")
    void aceitaNoLimite() {
        String email = "a".repeat(77 - "@itau.com.br".length()) + "@itau.com.br";
        assertThat(email).hasSize(77);
        assertThatCode(() -> validator.validate(email)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"@itau.com.br", "maria@", "maria@itau", "mari a@itau.com.br"})
    @DisplayName("rejeita formatos invalidos")
    void rejeitaFormatoInvalido(String valor) {
        assertThatThrownBy(() -> validator.validate(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("formato invalido");
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

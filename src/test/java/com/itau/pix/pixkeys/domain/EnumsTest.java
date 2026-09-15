package com.itau.pix.pixkeys.domain;

import com.itau.pix.pixkeys.exception.PixValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumsTest {

    @ParameterizedTest
    @ValueSource(strings = {"celular", "CELULAR", " Celular "})
    @DisplayName("KeyType aceita o tipo em qualquer caixa")
    void keyTypeAceitaQualquerCaixa(String valor) {
        assertThat(KeyType.from(valor)).isEqualTo(KeyType.CELULAR);
    }

    @Test
    @DisplayName("KeyType serializa em caixa baixa para a API")
    void keyTypeToApi() {
        assertThat(KeyType.CNPJ.toApi()).isEqualTo("cnpj");
        assertThat(KeyType.EMAIL.toApi()).isEqualTo("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cpf", "aleatorio", "xpto"})
    @DisplayName("KeyType rejeita tipos nao implementados")
    void keyTypeRejeitaInvalido(String valor) {
        assertThatThrownBy(() -> KeyType.from(valor))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("Tipo de chave invalido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("KeyType rejeita valor em branco")
    void keyTypeRejeitaBranco(String valor) {
        assertThatThrownBy(() -> KeyType.from(valor)).isInstanceOf(PixValidationException.class);
    }

    @Test
    @DisplayName("KeyType rejeita null")
    void keyTypeRejeitaNull() {
        assertThatThrownBy(() -> KeyType.from(null))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("obrigatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"poupanca", "POUPANCA", "poupança", "Poupança"})
    @DisplayName("AccountType aceita poupanca com e sem cedilha")
    void accountTypeAceitaAcento(String valor) {
        assertThat(AccountType.from(valor)).isEqualTo(AccountType.POUPANCA);
    }

    @Test
    @DisplayName("AccountType serializa em caixa baixa para a API")
    void accountTypeToApi() {
        assertThat(AccountType.CORRENTE.toApi()).isEqualTo("corrente");
        assertThat(AccountType.POUPANCA.toApi()).isEqualTo("poupanca");
    }

    @Test
    @DisplayName("AccountType rejeita valor invalido e null")
    void accountTypeRejeita() {
        assertThatThrownBy(() -> AccountType.from("salario"))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("Tipo de conta invalido");
        assertThatThrownBy(() -> AccountType.from(null))
                .isInstanceOf(PixValidationException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("PersonType define os limites de chaves do case")
    void limitesPorTipoDePessoa() {
        assertThat(PersonType.PF.limiteDeChaves()).isEqualTo(5);
        assertThat(PersonType.PJ.limiteDeChaves()).isEqualTo(20);
    }
}

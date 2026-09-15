package com.itau.pix.pixkeys.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    @DisplayName("conta PF expoe limite de 5 chaves")
    void contaPessoaFisica() {
        Account conta = new Account("1234", "12345678", PersonType.PF);

        assertThat(conta.getId()).isNull();
        assertThat(conta.getNumeroAgencia()).isEqualTo("1234");
        assertThat(conta.getNumeroConta()).isEqualTo("12345678");
        assertThat(conta.getTipoPessoa()).isEqualTo(PersonType.PF);
        assertThat(conta.limiteDeChaves()).isEqualTo(5);
    }

    @Test
    @DisplayName("conta PJ expoe limite de 20 chaves")
    void contaPessoaJuridica() {
        assertThat(new Account("4321", "11223344", PersonType.PJ).limiteDeChaves()).isEqualTo(20);
    }
}

package com.itau.pix.pixkeys.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PixKeyTest {

    private PixKey novaChave() {
        return new PixKey(KeyType.EMAIL, "maria@itau.com.br", AccountType.CORRENTE,
                "1234", "12345678", "Maria", "Silva");
    }

    @Test
    @DisplayName("nasce ativa, com UUID gerado e data de inclusao preenchida")
    void nasceAtiva() {
        PixKey chave = novaChave();

        assertThat(chave.getId()).isNotNull();
        assertThat(chave.getStatus()).isEqualTo(KeyStatus.ACTIVE);
        assertThat(chave.isInativa()).isFalse();
        assertThat(chave.getDataHoraInclusao()).isNotNull();
        assertThat(chave.getDataHoraInativacao()).isNull();
    }

    @Test
    @DisplayName("gera um UUID diferente por chave")
    void idUnicoPorChave() {
        assertThat(novaChave().getId()).isNotEqualTo(novaChave().getId());
    }

    @Test
    @DisplayName("alterarVinculo troca conta e titular preservando tipo e valor da chave")
    void alterarVinculoPreservaChave() {
        PixKey chave = novaChave();

        chave.alterarVinculo(AccountType.POUPANCA, "4321", "87654321", "Mariana", "Souza");

        assertThat(chave.getTipoConta()).isEqualTo(AccountType.POUPANCA);
        assertThat(chave.getNumeroAgencia()).isEqualTo("4321");
        assertThat(chave.getNumeroConta()).isEqualTo("87654321");
        assertThat(chave.getNomeCorrentista()).isEqualTo("Mariana");
        assertThat(chave.getSobrenomeCorrentista()).isEqualTo("Souza");
        assertThat(chave.getTipoChave()).isEqualTo(KeyType.EMAIL);
        assertThat(chave.getValorChave()).isEqualTo("maria@itau.com.br");
    }

    @Test
    @DisplayName("inativar faz soft delete e registra a data da solicitacao")
    void inativarFazSoftDelete() {
        PixKey chave = novaChave();

        chave.inativar();

        assertThat(chave.getStatus()).isEqualTo(KeyStatus.INACTIVE);
        assertThat(chave.isInativa()).isTrue();
        assertThat(chave.getDataHoraInativacao()).isNotNull();
    }
}

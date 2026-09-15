package com.itau.pix.pixkeys.dto;

import com.itau.pix.pixkeys.domain.PixKey;

/**
 * Saida da alteracao (tabela 3 do enunciado): datas em DATETIME, sem data de inativacao.
 */
public record PixKeyResponse(String id,
                             String tipoChave,
                             String valorChave,
                             String tipoConta,
                             String numeroAgencia,
                             String numeroConta,
                             String nomeCorrentista,
                             String sobrenomeCorrentista,
                             String dataHoraInclusao) {

    public static PixKeyResponse of(PixKey chave) {
        return new PixKeyResponse(
                chave.getId().toString(),
                chave.getTipoChave().toApi(),
                chave.getValorChave(),
                chave.getTipoConta().toApi(),
                chave.getNumeroAgencia(),
                chave.getNumeroConta(),
                chave.getNomeCorrentista(),
                Formatos.texto(chave.getSobrenomeCorrentista()),
                Formatos.data(chave.getDataHoraInclusao(), Formatos.DATA_HORA));
    }
}
